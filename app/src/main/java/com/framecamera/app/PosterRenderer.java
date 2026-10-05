package com.framecamera.app;
import android.graphics.*;

/** Preview and export share this renderer. Coordinates are based on a 1000px photo width. */
public final class PosterRenderer {
 public static final class Options {
  public boolean vertical=true,reverse=false,swapText=false,blur=false,cameraOnly=false;
  public int blurType=0; public float blurStrength=.6f,blurDirection=0;
  public boolean showSignature=true,showParameters=true,fadeScreen=false;
  public boolean scannerDevice=false,screenlessDevice=false;
  public boolean clearBodySize=false,flipPortraitBody=false;
  public boolean tintScreen=false,reflectScreen=false,reverseReflection=false;
  public int tintPreset=0;public int screenPreset=0;public float screenPresetStrength=.65f;
  public float tintStrength=.45f,reflectionStrength=.35f;
  public int sourcePhotoWidth=0;
  public int signatureFont=0,parameterFont=0;
  public float signatureSize=1,parameterSize=1,bodyScale=1,fadeAmount=.3f;
  public String camera="",lens="";
  public Options copy(){Options o=new Options();o.scannerDevice=scannerDevice;o.screenlessDevice=screenlessDevice;o.screenPreset=screenPreset;o.screenPresetStrength=screenPresetStrength;o.cameraOnly=cameraOnly;o.vertical=vertical;o.reverse=reverse;o.swapText=swapText;o.blur=blur;o.blurType=blurType;o.blurStrength=blurStrength;o.blurDirection=blurDirection;o.camera=camera;o.lens=lens;o.showSignature=showSignature;o.showParameters=showParameters;o.fadeScreen=fadeScreen;o.signatureFont=signatureFont;o.parameterFont=parameterFont;o.signatureSize=signatureSize;o.parameterSize=parameterSize;o.bodyScale=bodyScale;o.fadeAmount=fadeAmount;o.clearBodySize=clearBodySize;o.flipPortraitBody=flipPortraitBody;o.sourcePhotoWidth=sourcePhotoWidth;o.tintScreen=tintScreen;o.reflectScreen=reflectScreen;o.reverseReflection=reverseReflection;o.tintPreset=tintPreset;o.tintStrength=tintStrength;o.reflectionStrength=reflectionStrength;return o;}
 }
 private final BackgroundEffects backgrounds=new BackgroundEffects();
 public void clearCache(){backgrounds.clear();}
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 /** Match original source pixels per canvas unit; EXIF physical print PPI is irrelevant here. */
 public static float effectiveBodyScale(Bitmap photo,Bitmap body,Options o){
  if(!o.clearBodySize||body==null)return o.bodyScale;
  float panelHeight=o.cameraOnly||!o.vertical?1000f*photo.getHeight()/photo.getWidth():740;boolean portrait=!o.scannerDevice&&!o.screenlessDevice&&photo.getHeight()>photo.getWidth();
  float bw=portrait?body.getHeight():body.getWidth(),bh=portrait?body.getWidth():body.getHeight();
  float base=Math.min(780/bw,panelHeight*.66f/bh);
  float density=1000f/(o.sourcePhotoWidth>0?o.sourcePhotoWidth:photo.getWidth());
  return Math.min(1.15f,density/base);
 }
 public float[] size(Bitmap photo,Options o){float h=1000f*photo.getHeight()/photo.getWidth();return o.cameraOnly?new float[]{1000,h}:o.vertical?new float[]{1000,h+740}:new float[]{2000,h};}
 public RectF panelBounds(Bitmap photo,Options o){float ph=1000f*photo.getHeight()/photo.getWidth();if(o.cameraOnly)return new RectF(0,0,1000,ph);return o.vertical?new RectF(0,o.reverse?0:ph,1000,(o.reverse?0:ph)+740):new RectF(o.reverse?0:1000,0,o.reverse?1000:2000,ph);}
 /** Editing bounds in the same coordinate system as the poster. */
 public RectF focusBounds(Bitmap photo,Bitmap body,CameraCatalog.Model model,Options o,boolean screen){
  RectF panel=panelBounds(photo,o);if(!screen||body==null||model==null||!model.hasScreen)return panel;
  boolean portrait=!o.scannerDevice&&!o.screenlessDevice&&photo.getHeight()>photo.getWidth();float ts=Math.min(1,panel.height()/500);
  float text=Math.max(o.showSignature?29*o.signatureSize:0,o.showParameters?24*o.parameterSize:0)*ts;
  float bodyScale=effectiveBodyScale(photo,body,o),bw=portrait?body.getHeight():body.getWidth(),bh=portrait?body.getWidth():body.getHeight();
  float maxW=Math.min(panel.width()*.9f,panel.width()*.78f*bodyScale),maxH=Math.min(panel.height()-2*(32*ts+text/2+16),panel.height()*.66f*bodyScale),k=Math.min(maxW/bw,maxH/bh);
  if(screen){Rect r=model.screen;RectF lcd=new RectF(r.left-model.crop.left,r.top-model.crop.top,r.right-model.crop.left,r.bottom-model.crop.top);Matrix m=new Matrix();m.setTranslate(-body.getWidth()/2f,-body.getHeight()/2f);m.postScale(k,k);if(portrait)m.postRotate(o.flipPortraitBody?-90:90);m.postTranslate(panel.centerX(),panel.centerY());m.mapRect(lcd);lcd.inset(-lcd.width()*.025f,-lcd.height()*.025f);return lcd;}
  return panel;
 }
 public Bitmap render(Bitmap photo,Bitmap body,CameraCatalog.Model model,PhotoMetadata metadata,Options o,int longEdge){float[] s=size(photo,o);float scale=longEdge/Math.max(s[0],s[1]);Bitmap b=Bitmap.createBitmap(Math.max(1,Math.round(s[0]*scale)),Math.max(1,Math.round(s[1]*scale)),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.scale(scale,scale);draw(c,photo,body,model,metadata,o);return b;}
 public void draw(Canvas c,Bitmap photo,Bitmap body,CameraCatalog.Model model,PhotoMetadata metadata,Options o){
  float ph=1000f*photo.getHeight()/photo.getWidth();RectF image,panel;
  if(o.vertical){image=new RectF(0,o.reverse?740:0,1000,(o.reverse?740:0)+ph);panel=new RectF(0,o.reverse?0:ph,1000,(o.reverse?0:ph)+740);}else{image=new RectF(o.reverse?1000:0,0,o.reverse?2000:1000,ph);panel=new RectF(o.reverse?0:1000,0,o.reverse?1000:2000,ph);}
  if(o.cameraOnly)panel=panelBounds(photo,o);
  c.drawColor(0xff273d3b);p.reset();p.setAntiAlias(true);p.setFilterBitmap(true);if(!o.cameraOnly)c.drawBitmap(photo,null,image,p);p.setColor(palette(photo));c.drawRect(panel,p);
  if(o.blur){c.save();c.clipRect(panel);if(o.blurStrength<=0)BackgroundEffects.cover(c,photo,panel,p);else{Bitmap background=backgrounds.get(photo,panel.width()/panel.height(),o.blurType,o.blurStrength,o.blurDirection);p.setFilterBitmap(o.blurType!=2);c.drawBitmap(background,null,panel,p);p.setFilterBitmap(true);}p.setColor(0x700c1d1a);c.drawRect(panel,p);c.restore();}else backgrounds.clear();
  p.setAlpha(255);float cx=panel.centerX(),cy=panel.centerY();boolean portrait=!o.scannerDevice&&!o.screenlessDevice&&photo.getHeight()>photo.getWidth();float ts=Math.min(1,panel.height()/500);
  float largestText=Math.max(o.showSignature?29*o.signatureSize:0,o.showParameters?24*o.parameterSize:0)*ts;
  float bodyScale=effectiveBodyScale(photo,body,o);
  float maxW=Math.min(panel.width()*.90f,panel.width()*.78f*bodyScale);
  float maxH=Math.min(panel.height()-2*(32*ts+largestText/2+16),panel.height()*.66f*bodyScale);
  if(body!=null&&model!=null&&model.scanner){float k=Math.min(maxW/body.getWidth(),maxH/body.getHeight());float dw=body.getWidth()*k,dh=body.getHeight()*k;c.drawBitmap(body,null,new RectF(cx-dw/2,cy-dh/2,cx+dw/2,cy+dh/2),p);labels(c,cx,cy-dh/2-32*ts,cy+dh/2+32*ts,panel.width()*.87f,metadata,o,ts);if(model.genericArtwork)text(c,"通用扫描示意 · 硬件型号未记录",cx,panel.bottom-14*ts,22*ts,panel.width()*.88f,false);return;}
  if(body!=null&&model!=null){float bw=portrait?body.getHeight():body.getWidth(),bh=portrait?body.getWidth():body.getHeight();float k=Math.min(maxW/bw,maxH/bh),dh=bh*k;
   c.save();c.translate(cx,cy);if(portrait)c.rotate(o.flipPortraitBody?-90:90);c.scale(k,k);c.translate(-body.getWidth()/2f,-body.getHeight()/2f);
   // Keep shadow softness in composition coordinates, independent of source image resolution.
   Paint shadow=new Paint(Paint.ANTI_ALIAS_FLAG);Bitmap silhouette=body.extractAlpha();
   shadow.setColor(0x3307110f);shadow.setMaskFilter(new BlurMaskFilter(15/k,BlurMaskFilter.Blur.NORMAL));c.drawBitmap(silhouette,0,10/k,shadow);
   shadow.setColor(0x2507110f);shadow.setMaskFilter(new BlurMaskFilter(4/k,BlurMaskFilter.Blur.NORMAL));c.drawBitmap(silhouette,0,3/k,shadow);silhouette.recycle();c.drawBitmap(body,0,0,p);
   if(model.hasScreen){Rect r=model.screen;RectF lcd=new RectF(r.left-model.crop.left,r.top-model.crop.top,r.right-model.crop.left,r.bottom-model.crop.top);p.setColor(Color.BLACK);c.drawRect(lcd,p);c.save();c.clipRect(lcd);c.translate(lcd.centerX(),lcd.centerY());if(portrait)c.rotate(o.flipPortraitBody?90:-90);
   float sw=portrait?lcd.height():lcd.width(),sh=portrait?lcd.width():lcd.height(),fit=Math.min(sw/photo.getWidth(),sh/photo.getHeight());RectF dst=new RectF(-photo.getWidth()*fit/2,-photo.getHeight()*fit/2,photo.getWidth()*fit/2,photo.getHeight()*fit/2);ScreenEffects.applyTint(p,o);c.drawBitmap(photo,null,dst,p);p.setColorFilter(null);
   // A translucent white veil reduces contrast only inside the displayed photo.
   if(o.fadeScreen){p.setColor(Color.WHITE);p.setAlpha(Math.round(255*Math.max(0,Math.min(.7f,o.fadeAmount))));c.drawRect(dst,p);p.setAlpha(255);}
   c.restore();ScreenEffects.presetGlass(c,lcd,o);ScreenEffects.reflection(c,lcd,o);}c.restore();labels(c,cx,cy-dh/2-32*ts,cy+dh/2+32*ts,panel.width()*.87f,metadata,o,ts);
  }else{p.setColor(0x55ffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRoundRect(new RectF(cx-210,cy-115,cx+210,cy+115),20,20,p);p.setStyle(Paint.Style.FILL);text(c,"暂无对应机模",cx,cy-8,34,panel.width()*.8f,false);text(c,"需要完整 EXIF 与已适配素材",cx,cy+38,22,panel.width()*.8f,false);labels(c,cx,cy-165,cy+178,panel.width()*.87f,metadata,o,1);}
 }
 private void labels(Canvas c,float x,float top,float bottom,float width,PhotoMetadata m,Options o,float scale){
  String camera=o.camera.trim().isEmpty()?"相机名称未知":o.camera.trim(),lens=o.lens.trim(),names=camera+(lens.isEmpty()?"":"  ·  "+lens);
  if(o.showSignature)styledText(c,names,x,o.swapText?bottom:top,29*o.signatureSize*scale,width,o.signatureFont);
  if(o.showParameters&&!o.scannerDevice)styledText(c,m.parameters(),x,o.swapText?top:bottom,24*o.parameterSize*scale,width,o.parameterFont);
 }
 private void styledText(Canvas c,String text,float x,float centerY,float size,float width,int font){p.setColor(0xfff9faf7);p.setTypeface(FontBook.get(font));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);float measured=p.measureText(text);if(measured>width)p.setTextSize(size*width/measured);Paint.FontMetrics fm=p.getFontMetrics();c.drawText(text,x,centerY-(fm.ascent+fm.descent)/2,p);}
 private void text(Canvas c,String text,float x,float y,float size,float width,boolean mono){p.setColor(0xfff9faf7);p.setTypeface(mono?Typeface.MONOSPACE:Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);if(p.measureText(text)>width)p.setTextSize(size*width/p.measureText(text));c.drawText(text,x,y,p);}
 public static int palette(Bitmap b){long r=0,g=0,bl=0;int n=0;for(int y=0;y<b.getHeight();y+=Math.max(1,b.getHeight()/24))for(int x=0;x<b.getWidth();x+=Math.max(1,b.getWidth()/24)){int p=b.getPixel(x,y);r+=Color.red(p);g+=Color.green(p);bl+=Color.blue(p);n++;}return Color.rgb((int)(r/n*.68),(int)(g/n*.68),(int)(bl/n*.68));}
}
