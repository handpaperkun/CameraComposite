package com.framecamera.app;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.io.*;

/** Original, licensed tiles are unchanged. Only the UI composites them with its palette. */
public final class TextureBackground extends Drawable {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final int base;
 public TextureBackground(Context context,boolean dark)throws IOException{
  base=dark?0xff191d1b:0xfff5f1e7;
  try(InputStream in=context.getAssets().open("textures/"+(dark?"dark_leather.png":"paper.png"))){
   Bitmap bitmap=BitmapFactory.decodeStream(in);if(bitmap==null)throw new IOException("纹理无法读取");
   BitmapShader shader=new BitmapShader(bitmap,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);
   Matrix matrix=new Matrix();float scale=context.getResources().getDisplayMetrics().density*.65f;matrix.setScale(scale,scale);shader.setLocalMatrix(matrix);paint.setShader(shader);paint.setAlpha(dark?105:95);
  }
 }
 @Override public void draw(Canvas c){c.drawColor(base);c.drawRect(getBounds(),paint);}
 @Override public void setAlpha(int a){paint.setAlpha(a);invalidateSelf();}
 @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
