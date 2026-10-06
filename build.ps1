param([string]$Sdk="$env:LOCALAPPDATA\Android\Sdk",[string]$Jdk=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if (!$Jdk) { throw 'Set JAVA_HOME or pass -Jdk with your JDK 17 path.' }
$signingPasswordFile = Join-Path $PSScriptRoot 'signing/local-test.password'
if (!$env:FRAMECAMERA_STORE_PASSWORD -and (Test-Path -LiteralPath $signingPasswordFile)) {
 $env:FRAMECAMERA_STORE_PASSWORD = [IO.File]::ReadAllText($signingPasswordFile).Trim()
}
if (!$env:FRAMECAMERA_STORE_PASSWORD) {
 throw 'Set FRAMECAMERA_STORE_PASSWORD or create the ignored signing/local-test.password file. Never commit signing credentials.'
}

Push-Location -LiteralPath $PSScriptRoot
try {
 $env:JAVA_HOME=$Jdk
 $env:Path="$Jdk\bin;"+$env:Path
 $bt=Join-Path $Sdk 'build-tools\35.0.0'
 $androidJar=Join-Path $Sdk 'platforms\android-35\android.jar'
 function Check([string]$step){if($LASTEXITCODE -ne 0){throw "$step failed: $LASTEXITCODE"}}
 New-Item -ItemType Directory -Force build,build\classes,build\gen,build\dex,tools\deps,signing,dist | Out-Null
 Add-Type -AssemblyName System.IO.Compression.FileSystem
 if(!(Test-Path tools\deps\exifinterface.aar)){Invoke-WebRequest 'https://dl.google.com/dl/android/maven2/androidx/exifinterface/exifinterface/1.4.1/exifinterface-1.4.1.aar' -OutFile tools\deps\exifinterface.aar}
 $aar=[IO.Compression.ZipFile]::OpenRead((Join-Path $PSScriptRoot 'tools\deps\exifinterface.aar'))
 try{[IO.Compression.ZipFileExtensions]::ExtractToFile($aar.GetEntry('classes.jar'),(Join-Path $PSScriptRoot 'tools\deps\exifinterface.jar'),$true)}finally{$aar.Dispose()}
 & "$bt\aapt2.exe" compile --dir app/src/main/res -o build/resources.zip
 Check 'Resources'
 & "$bt\aapt2.exe" link -o build/unsigned.apk -I $androidJar --manifest app/src/main/AndroidManifest.xml --java build/gen build/resources.zip
 Check 'Package'
 $files=@(Get-ChildItem app\src\main\java,build\gen -Recurse -Filter *.java | ForEach-Object {'"'+$_.FullName.Substring($PSScriptRoot.Length+1).Replace('\','/')+'"'})
 [IO.File]::WriteAllLines((Join-Path $PSScriptRoot 'build\sources.txt'),$files,[Text.UTF8Encoding]::new($false))
 & "$Jdk\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -classpath "$androidJar;tools/deps/exifinterface.jar" -d build/classes '@build/sources.txt'
 Check 'Java'
 & "$Jdk\bin\jar.exe" cf build/classes.jar -C build/classes .
 Check 'Jar'
 & "$bt\d8.bat" --release --min-api 28 --lib $androidJar --output build/dex build/classes.jar tools/deps/exifinterface.jar
 Check 'DEX'
 $zip=[IO.Compression.ZipFile]::Open((Join-Path $PSScriptRoot 'build\unsigned.apk'),[IO.Compression.ZipArchiveMode]::Update)
 try{
  Get-ChildItem build\dex -Filter *.dex | ForEach-Object {[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$_.FullName,$_.Name,[IO.Compression.CompressionLevel]::Optimal)|Out-Null}
  $assetRoot=(Resolve-Path app/src/main/assets).Path
  Get-ChildItem $assetRoot -Recurse -File | ForEach-Object {$entry='assets/'+$_.FullName.Substring($assetRoot.Length+1).Replace('\','/');[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$_.FullName,$entry,[IO.Compression.CompressionLevel]::Optimal)|Out-Null}
 }finally{$zip.Dispose()}
 if(!(Test-Path signing\local-test.jks)){& "$Jdk\bin\keytool.exe" -genkeypair -keystore signing/local-test.jks -storepass:env FRAMECAMERA_STORE_PASSWORD -keypass:env FRAMECAMERA_STORE_PASSWORD -alias framecamera -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=FrameCamera Local Test,O=FrameCamera,C=CN';Check 'Signing key'}
 & "$bt\zipalign.exe" -f -P 16 4 build/unsigned.apk build/aligned.apk
 Check 'Align'
 & "$bt\apksigner.bat" sign --ks signing/local-test.jks --ks-key-alias framecamera --ks-pass env:FRAMECAMERA_STORE_PASSWORD --out dist/FrameCamera-0.8.14.apk build/aligned.apk
 Check 'Sign'
 & "$bt\apksigner.bat" verify --verbose dist/FrameCamera-0.8.14.apk
 Check 'Verify'
 Get-FileHash dist/FrameCamera-0.8.14.apk -Algorithm SHA256
} finally {Pop-Location}
