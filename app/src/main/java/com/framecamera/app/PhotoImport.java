package com.framecamera.app;

import android.content.Intent;
import android.provider.MediaStore;

/** A source preference changes only the picker, never the EXIF import pipeline. */
final class PhotoImport {
 static final String PREFS="photo_import",KEY="source";
 static final int FILES=0,ALBUM=1;
 static Intent intent(int source,int sdk){
  if(source==ALBUM){
   Intent i=sdk>=33?new Intent(MediaStore.ACTION_PICK_IMAGES):new Intent(Intent.ACTION_PICK);
   if(sdk>=33)i.setType("image/*");
   else i.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
   return i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  }
  return new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
   .setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","image/avif","application/octet-stream"})
   .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
 }
 static Intent fallbackAlbum(){
  return Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE)
   .setType("image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"选择相册应用");
 }
}
