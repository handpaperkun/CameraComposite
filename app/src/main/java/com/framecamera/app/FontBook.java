package com.framecamera.app;

import android.content.Context;
import android.graphics.Typeface;

/** Original, unmodified OFL fonts are bundled with their full license texts. */
public final class FontBook {
 public static final String[] NAMES={"简约黑体", "经典衬线", "等宽字", "站酷小薇 · 艺术", "马善政 · 毛笔", "缝合像素 · 12px"};
 public static final String[] LICENSES={"ZCOOLXiaoWei-OFL.txt","MaShanZheng-OFL.txt","FusionPixel-OFL.txt"};
 private static volatile Typeface[] fonts;
 public static synchronized void initialize(Context context){
  if(fonts!=null)return;
  fonts=new Typeface[]{Typeface.create("sans-serif-medium",0),Typeface.SERIF,Typeface.MONOSPACE,
   Typeface.createFromAsset(context.getAssets(),"fonts/ZCOOLXiaoWei-Regular.ttf"),
   Typeface.createFromAsset(context.getAssets(),"fonts/MaShanZheng-Regular.ttf"),
   Typeface.createFromAsset(context.getAssets(),"fonts/fusion-pixel-12px-proportional-zh_hans.ttf")};
 }
 public static Typeface get(int id){Typeface[] available=fonts;if(available==null)return id==2?Typeface.MONOSPACE:Typeface.DEFAULT;return available[Math.max(0,Math.min(id,available.length-1))];}
 private FontBook(){}
}
