package com.framecamera.app;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import java.io.IOException;
import java.io.File;

/** Scoped exports: create a new sibling without modifying the imported original. */
final class PhotoExport {
 static final String LOCATION="export_location_v2",FORMAT="export_format";
 static final int SAME_FOLDER=0,ASK=1,ALBUM=2;
 static final String ALBUM_PATH="Pictures/器材片/";
 static final String[] LOCATIONS={"相册 · 器材片（默认）","原照片所在文件夹","每次选择保存位置"};
 static int location(int value){return value==SAME_FOLDER||value==ASK?value:ALBUM;}
 static int locationIndex(int value){return value==ALBUM?0:value+1;}
 static int locationAt(int index){return index==1?SAME_FOLDER:index==2?ASK:ALBUM;}
 static final String[] FORMATS={"JPG · 长边 4096 px · 画质 95%","PNG · 长边 4096 px · 无损","JPG · 长边 2048 px · 轻量"};
 static int format(int value){return value>=0&&value<FORMATS.length?value:0;}
 static String mime(int value){return value==1?"image/png":"image/jpeg";}
 static int edge(int value){return value==2?2048:4096;}
 static final class Target {
  final Uri uri;final boolean media,owned,album;
  Target(Uri uri,boolean media,boolean owned){this(uri,media,owned,false);}
  Target(Uri uri,boolean media,boolean owned,boolean album){this.uri=uri;this.media=media;this.owned=owned;this.album=album;}
  void complete(Context context)throws IOException{
   if(media&&Build.VERSION.SDK_INT>=29){ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.IS_PENDING,0);if(context.getContentResolver().update(uri,values,null,null)!=1)throw new IOException("无法完成保存");}
  }
  void discard(Context context){if(!owned)return;try{if(media)context.getContentResolver().delete(uri,null,null);else DocumentsContract.deleteDocument(context.getContentResolver(),uri);}catch(Exception ignored){}}
 }
 static Target album(Context context,String mime,String name)throws IOException{
  File reserved=null;Uri created=null;
  try{
   ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,name);values.put(MediaStore.MediaColumns.MIME_TYPE,mime);
   if(Build.VERSION.SDK_INT>=29){values.put(MediaStore.MediaColumns.RELATIVE_PATH,ALBUM_PATH);values.put(MediaStore.MediaColumns.IS_PENDING,1);}
   else{
    File folder=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"器材片");
    if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("无法创建相册目录，请检查存储空间。");
    reserved=new File(folder,name);
    if(!reserved.createNewFile())reserved=File.createTempFile("器材片_",mime.equals("image/png")?".png":".jpg",folder);
    values.put(MediaStore.MediaColumns.DATA,reserved.getAbsolutePath());values.put(MediaStore.MediaColumns.DISPLAY_NAME,reserved.getName());
   }
   created=context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
   if(created==null)throw new IOException("无法创建相册图片，请检查存储空间。");
   return new Target(created,true,true,true);
  }catch(Exception e){if(created!=null)new Target(created,true,true,true).discard(context);if(reserved!=null)reserved.delete();throw new IOException("无法保存到相册，请检查存储空间或相册权限。",e);}
 }
 static final class MediaFolder {
  final String path,volume;
  MediaFolder(String p,String v){path=p;volume=v;}
 }
 static boolean safeRelativePath(String path){
  if(path==null||path.isEmpty()||path.startsWith("/")||path.contains("\\")||!path.endsWith("/"))return false;
  for(String part:path.split("/"))if(part.equals(".")||part.equals(".."))return false;
  return path.startsWith("Pictures/")||path.startsWith("DCIM/")||path.startsWith("Download/");
 }
 static MediaFolder mediaFolder(Context context,Uri source){
  if(Build.VERSION.SDK_INT<29||source==null)return null;
  Uri media=source;
  if(!"media".equals(media.getAuthority()))try{media=MediaStore.getMediaUri(context,source);}catch(Exception ignored){return null;}
  if(media==null||!"media".equals(media.getAuthority()))return null;
  try(Cursor c=context.getContentResolver().query(media,new String[]{MediaStore.MediaColumns.RELATIVE_PATH,MediaStore.MediaColumns.VOLUME_NAME},null,null,null)){
   if(c!=null&&c.moveToFirst()){
    String path=c.getString(0),volume=c.getString(1);
    if(safeRelativePath(path)&&volume!=null&&MediaStore.getExternalVolumeNames(context).contains(volume))return new MediaFolder(path,volume);
   }
  }catch(Exception ignored){}
  return null;
 }
 static boolean canRequestFolder(Context context,Uri source){return source!=null&&DocumentsContract.isDocumentUri(context,source);}
 static Uri folderHint(Context context,Uri source){
  if(source==null)return null;
  // Only the platform external-storage provider defines volume:relative/path IDs.
  if("com.android.externalstorage.documents".equals(source.getAuthority()))try{
   String id=DocumentsContract.getDocumentId(source);int slash=id.lastIndexOf('/');
   if(slash>=0)return DocumentsContract.buildDocumentUri(source.getAuthority(),id.substring(0,slash));
  }catch(Exception ignored){}
  return source;
 }
 static Intent folderIntent(Context context,Uri source){
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
  Uri hint=folderHint(context,source);if(hint!=null)i.putExtra(DocumentsContract.EXTRA_INITIAL_URI,hint);return i;
 }
 /** Query direct children; a matching filename or an ancestor folder alone is insufficient. */
 static boolean containsOriginal(Context context,Uri tree,Uri source){
  if(!canRequestFolder(context,source)||tree==null||!DocumentsContract.isTreeUri(tree)||!source.getAuthority().equals(tree.getAuthority()))return false;
  try{
   String wanted=DocumentsContract.getDocumentId(source),root=DocumentsContract.getTreeDocumentId(tree);
   Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,root);
   try(Cursor c=context.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},null,null,null)){
    if(c!=null)while(c.moveToNext())if(wanted.equals(c.getString(0)))return true;
   }
  }catch(Exception ignored){}
  return false;
 }
 static Target inTree(Context context,Uri tree,Uri source,String mime,String name)throws IOException{
  if(!containsOriginal(context,tree,source))throw new IOException("所选文件夹不是原照片所在目录，请选择包含原照片的文件夹。");
  try{
   Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
   Uri created=DocumentsContract.createDocument(context.getContentResolver(),parent,mime,name);
   if(created==null)throw new IOException("该文件夹无法创建图片");return new Target(created,false,true);
  }catch(IOException e){throw e;}catch(Exception e){throw new IOException("无法写入原照片目录，请重新授权或选择保存位置。",e);}
 }
 static Target automatic(Context context,Uri source,String mime,String name)throws IOException{
  MediaFolder folder=mediaFolder(context,source);
  if(folder!=null){
   Uri created=null;
   try{
    ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,name);values.put(MediaStore.MediaColumns.MIME_TYPE,mime);values.put(MediaStore.MediaColumns.RELATIVE_PATH,folder.path);values.put(MediaStore.MediaColumns.IS_PENDING,1);
    Uri collection=folder.path.startsWith("Download/")?MediaStore.Downloads.getContentUri(folder.volume):MediaStore.Images.Media.getContentUri(folder.volume);
    created=context.getContentResolver().insert(collection,values);
    if(created!=null){
     MediaFolder actual=mediaFolder(context,created);
     if(actual!=null&&actual.path.equals(folder.path)&&actual.volume.equals(folder.volume))return new Target(created,true,true);
     new Target(created,true,true).discard(context);
    }
   }catch(Exception e){if(created!=null)new Target(created,true,true).discard(context);}
  }
  for(UriPermission permission:context.getContentResolver().getPersistedUriPermissions()){
   if(permission.isReadPermission()&&permission.isWritePermission()&&containsOriginal(context,permission.getUri(),source))return inTree(context,permission.getUri(),source,mime,name);
  }
  return null;
 }
 private PhotoExport(){}
}
