# Android 构建

仓库仅包含安卓工程及运行时素材。素材与 v0.8.3 APK 一致，直接打包，无需 Python、采集下载、抠图或测试脚本。

需要 JDK 17、Android SDK Platform 35、Build Tools 35.0.0。Gradle 配置使用 Android Gradle Plugin 8.10.0，仓库未附带 Gradle Wrapper。

Windows 可执行 build.ps1，使用 -Sdk 和 -Jdk 指定本机 SDK、JDK 路径；也可以从 JAVA_HOME 读取 JDK。

签名密码从 FRAMECAMERA_STORE_PASSWORD 环境变量读取，或保存在本机 signing/local-test.password。后者是私有纯文本文件，不应分享；整个 signing 目录都被排除在 Git 之外。未配置密码时脚本会提示并停止。

首次构建生成 signing/local-test.jks，后续复用密钥。不同密钥签名的 APK 无法直接覆盖已有安装，原发行者的私钥不随仓库提供。

构建只获取标准 AndroidX 依赖，不下载或处理机模。输出位于 dist/；安装包通过 GitHub Releases 分发。

项目代码沿用根目录 LICENSE。机模来源及哈希在 app/src/main/assets/cameras/catalog.json；第三方产品图的分发授权尚未全部核实，项目代码许可不代表第三方素材授权。字体和纹理的许可保留在相应素材目录内。
