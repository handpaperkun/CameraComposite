package com.framecamera.app;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import java.io.*;

/** Shared preview/export path, with content-based AVIF detection. */
final class PhotoDecoder {
 static boolean isAvif(BufferedInputStream in)throws IOException{
  in.mark(4096);byte[] header=new byte[4096];int size=0;
  try{while(size<header.length){int n=in.read(header,size,header.length-size);if(n<0)break;if(n==0)break;size+=n;}}
  finally{in.reset();}
  if(size<16||!fourcc(header,4,"ftyp"))return false;
  long box=((header[0]&255L)<<24)|((header[1]&255L)<<16)|((header[2]&255L)<<8)|(header[3]&255L);
  if(box<16)return false;
  int end=(int)Math.min(box,size);
  for(int i=8;i+4<=end;i+=4)if(i!=12&&(fourcc(header,i,"avif")||fourcc(header,i,"avis")))return true;
  return false;
 }
 private static boolean fourcc(byte[] b,int at,String s){for(int i=0;i<4;i++)if(b[at+i]!=(byte)s.charAt(i))return false;return true;}
 static void requireAvifSupport(boolean avif,int sdk)throws IOException{
  if(avif&&sdk<31)throw new IOException("AVIF 导入需要 Android 12 或更新版本。此设备可使用保留 EXIF 的 JPG、PNG 或 WebP。");
 }
 static Bitmap decode(ContentResolver resolver,Uri uri,int edge,int[] originalSize)throws IOException{
  boolean avif;try(InputStream source=resolver.openInputStream(uri)){
   if(source==null)throw new IOException("无法打开照片");
   avif=isAvif(new BufferedInputStream(source));
  }
  requireAvifSupport(avif,Build.VERSION.SDK_INT);
  try{
   return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver,uri),(decoder,info,src)->{
    int w=info.getSize().getWidth(),h=info.getSize().getHeight();
    if(originalSize!=null){originalSize[0]=w;originalSize[1]=h;}
    double scale=Math.min(1,(double)edge/Math.max(w,h));
    decoder.setTargetSize(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));
    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
    decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));
   });
  }catch(ImageDecoder.DecodeException e){
   if(avif)throw new IOException("AVIF 无法解码：文件可能不完整，或此系统不支持该 AVIF 编码。请尝试保留 EXIF 的 JPG。",e);
   throw e;
  }
 }
}
