package com.framecamera.app;

import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Only matched, processed device artwork is cached. Never stores imported photographs. */
final class ModelCache {
 interface Loader {Bitmap load()throws IOException;}
 static final long LIMIT=32L*1024*1024;
 private static File directory(Context c){return new File(c.getNoBackupFilesDir(),"device-artwork-v1");}
 static synchronized Bitmap load(Context c,String identity,int width,int height,Loader loader)throws IOException{
  File dir=directory(c),entry=new File(dir,key(identity)+".png");
  if(entry.isFile()){
   BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(entry.getPath(),bounds);
   if(bounds.outWidth==width&&bounds.outHeight==height){Bitmap cached=BitmapFactory.decodeFile(entry.getPath());if(cached!=null){entry.setLastModified(System.currentTimeMillis());return cached;}}
   entry.delete();
  }
  Bitmap body=loader.load();
  // A cache write failure must never prevent viewing/exporting the matched artwork.
  File temp=null;
  try{
   if(!dir.isDirectory()&&!dir.mkdirs())return body;
   temp=File.createTempFile("artwork-",".tmp",dir);
   try(OutputStream out=new FileOutputStream(temp)){if(!body.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Cache encoding failed");}
   if(temp.length()<=LIMIT&&temp.renameTo(entry)){entry.setLastModified(System.currentTimeMillis());trim(dir,entry);}
  }catch(IOException|SecurityException ignored){}finally{if(temp!=null)temp.delete();}
  return body;
 }
 private static void trim(File dir,File keep){File[] files=dir.listFiles((d,n)->n.endsWith(".png"));if(files==null)return;long total=0;for(File f:files)total+=f.length();Arrays.sort(files,Comparator.comparingLong(File::lastModified));for(File f:files)if(total>LIMIT&&!f.equals(keep)){long size=f.length();if(f.delete())total-=size;}}
 static String key(String value){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}catch(Exception e){throw new IllegalStateException(e);}}
}
