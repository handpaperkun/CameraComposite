package com.framecamera.app;

import android.graphics.*;

/** Stylized LCD color casts and glass highlights, confined to the calibrated screen. */
public final class ScreenEffects {
 public static final String[] PRESET_NAMES={"原始显示（不加预设）","冷蓝 LCD","暖黄 LCD","轻微绿偏","轻微品红","斜视泛灰","明亮环境反光"};
 // R/G/B gain, contrast, saturation, lifted blacks. Artistic approximations, not model measurements.
 private static final float[][] PRESETS={
  {1,1,1,1,1,0}, {.94f,.995f,1.065f,.96f,.96f,3},
  {1.055f,1.01f,.93f,.97f,.95f,3}, {.955f,1.045f,.98f,.96f,.95f,3},
  {1.04f,.955f,1.025f,.98f,.97f,2}, {.98f,1.015f,1.035f,.82f,.84f,9},
  {1.015f,1.015f,1.025f,.91f,.91f,5}
 };
 public static final String[] TINT_NAMES={"冷蓝", "暖黄", "青绿", "淡紫"};
 // RGB gains and offsets; interpolation at zero is exactly the original image.
 private static final float[][] TINTS={
  {.84f,.97f,1.12f,-2,1,7},
  {1.12f,1.01f,.84f,6,2,-2},
  {.88f,1.08f,1.03f,-1,4,2},
  {1.07f,.88f,1.10f,4,-1,5}
 };
 public static void applyTint(Paint paint,PosterRenderer.Options o){
  paint.setColorFilter(null);
  ColorMatrix combined=new ColorMatrix();boolean preset=o.screenPreset>0&&o.screenPreset<PRESETS.length&&o.screenPresetStrength>0;
  if(preset){float a=clamp(o.screenPresetStrength);float[] v=PRESETS[o.screenPreset];float contrast=1+(v[3]-1)*a,offset=128*(1-contrast)+v[5]*a;
   combined.setSaturation(1+(v[4]-1)*a);
   combined.postConcat(new ColorMatrix(new float[]{contrast*(1+(v[0]-1)*a),0,0,0,offset,0,contrast*(1+(v[1]-1)*a),0,0,offset,0,0,contrast*(1+(v[2]-1)*a),0,offset,0,0,0,1,0}));
  }
  if(!o.tintScreen||o.tintStrength<=0){if(preset)paint.setColorFilter(new ColorMatrixColorFilter(combined));return;}
  float amount=clamp(o.tintStrength);float[] tint=TINTS[Math.max(0,Math.min(TINTS.length-1,o.tintPreset))];
  combined.postConcat(new ColorMatrix(new float[]{
   1+(tint[0]-1)*amount,0,0,0,tint[3]*amount,
   0,1+(tint[1]-1)*amount,0,0,tint[4]*amount,
   0,0,1+(tint[2]-1)*amount,0,tint[5]*amount,
   0,0,0,1,0}));paint.setColorFilter(new ColorMatrixColorFilter(combined));
 }
 public static void presetGlass(Canvas canvas,RectF screen,PosterRenderer.Options o){
  if(o.screenPresetStrength<=0||(o.screenPreset!=5&&o.screenPreset!=6))return;
  Paint p=new Paint(3);float a=clamp(o.screenPresetStrength);canvas.save();canvas.clipRect(screen);
  if(o.screenPreset==5){p.setShader(new LinearGradient(screen.left,screen.top,screen.left,screen.bottom,new int[]{Color.argb(Math.round(26*a),202,216,239),Color.TRANSPARENT,Color.argb(Math.round(12*a),19,29,43)},new float[]{0,.6f,1},Shader.TileMode.CLAMP));}
  else{p.setShader(new LinearGradient(screen.left,screen.top,screen.right,screen.bottom,new int[]{white(.25f*a),white(.04f*a),white(.13f*a),white(0)},new float[]{0,.4f,.52f,1},Shader.TileMode.CLAMP));}
  canvas.drawRect(screen,p);canvas.restore();
 }
 public static void reflection(Canvas canvas,RectF screen,PosterRenderer.Options o){
  if(!o.reflectScreen||o.reflectionStrength<=0)return;
  float amount=clamp(o.reflectionStrength);Paint glass=new Paint(Paint.ANTI_ALIAS_FLAG);
  float start=o.reverseReflection?screen.right:screen.left,end=o.reverseReflection?screen.left:screen.right;
  canvas.save();canvas.clipRect(screen);
  // Broad ambient reflection plus a soft diagonal highlight on the glass.
  glass.setShader(new LinearGradient(start,screen.top,end,screen.bottom,
   new int[]{white(.25f*amount),white(.07f*amount),white(0)},new float[]{0,.48f,1},Shader.TileMode.CLAMP));
  canvas.drawRect(screen,glass);
  glass.setShader(new LinearGradient(start,screen.top,end,screen.bottom,
   new int[]{white(0),white(0),white(.13f*amount),white(.52f*amount),white(.08f*amount),white(0),white(0)},
   new float[]{0,.19f,.29f,.35f,.43f,.53f,1},Shader.TileMode.CLAMP));
  canvas.drawRect(screen,glass);canvas.restore();
 }
 private static int white(float alpha){return Color.argb(Math.round(255*alpha),244,250,255);}
 private static float clamp(float value){return Math.max(0,Math.min(1,value));}
 private ScreenEffects(){}
}
