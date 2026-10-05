package com.framecamera.app;

import android.graphics.*;

/** Background-only filters in normalized panel space, shared by preview and export. */
final class BackgroundEffects {
 static final String[] NAMES={"高斯模糊","运动模糊","马赛克"};
 private Bitmap source,cached;private int generation,type,amount,angle,width,height;
 Bitmap get(Bitmap photo,float ratio,int style,float strength,float direction){
  int w=ratio>=1?320:Math.max(1,Math.round(320*ratio)),h=ratio>=1?Math.max(1,Math.round(320/ratio)):320;
  int a=Math.round(Math.max(0,Math.min(1,strength))*100),d=Math.round(direction)%180;
  if(cached!=null&&source==photo&&generation==photo.getGenerationId()&&type==style&&amount==a&&angle==d&&width==w&&height==h)return cached;
  if(cached!=null)cached.recycle();source=photo;generation=photo.getGenerationId();type=style;amount=a;angle=d;width=w;height=h;
  Bitmap base=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(base);Paint p=new Paint(Paint.FILTER_BITMAP_FLAG);cover(c,photo,new RectF(0,0,w,h),p);
  int[] input=new int[w*h];base.getPixels(input,0,w,0,0,w,h);int[] output;
  if(a==0)output=input;
  else if(style==1)output=motion(input,w,h,a/100f,d);
  else if(style==2)output=mosaic(input,w,h,Math.max(2,Math.round(2+38*a/100f)));
  else output=gaussian(input,w,h,.35f+14*a/100f);
  base.setPixels(output,0,w,0,0,w,h);cached=base;return cached;
 }
 static void cover(Canvas c,Bitmap source,RectF panel,Paint p){
  float k=Math.max(panel.width()/source.getWidth(),panel.height()/source.getHeight());float w=source.getWidth()*k,h=source.getHeight()*k;
  c.save();c.clipRect(panel);c.drawBitmap(source,null,new RectF(panel.centerX()-w/2,panel.centerY()-h/2,panel.centerX()+w/2,panel.centerY()+h/2),p);c.restore();
 }
 private static int[] gaussian(int[] input,int w,int h,float sigma){
  int radius=(int)Math.ceil(sigma*3);float[] kernel=new float[radius*2+1];float sum=0;
  for(int j=-radius;j<=radius;j++){float v=(float)Math.exp(-j*j/(2*sigma*sigma));kernel[j+radius]=v;sum+=v;}for(int j=0;j<kernel.length;j++)kernel[j]/=sum;
  int[] temp=new int[input.length],out=new int[input.length];
  for(int pass=0;pass<2;pass++){int[] from=pass==0?input:temp,to=pass==0?temp:out;
   for(int y=0;y<h;y++)for(int x=0;x<w;x++){float r=0,g=0,b=0;
    for(int j=-radius;j<=radius;j++){int xx=pass==0?clamp(x+j,w):x,yy=pass==1?clamp(y+j,h):y;int color=from[yy*w+xx];float weight=kernel[j+radius];r+=Color.red(color)*weight;g+=Color.green(color)*weight;b+=Color.blue(color)*weight;}
    to[y*w+x]=Color.rgb(Math.round(r),Math.round(g),Math.round(b));
   }
  }return out;
 }
 private static int[] motion(int[] in,int w,int h,float strength,int angle){
  int radius=Math.max(1,Math.round(strength*42)),count=radius*2+1;double radians=Math.toRadians(angle);int[] dx=new int[count],dy=new int[count],out=new int[in.length];
  for(int j=-radius;j<=radius;j++){dx[j+radius]=(int)Math.round(j*Math.cos(radians));dy[j+radius]=(int)Math.round(j*Math.sin(radians));}
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){int r=0,g=0,b=0;for(int j=0;j<count;j++){int color=in[clamp(y+dy[j],h)*w+clamp(x+dx[j],w)];r+=Color.red(color);g+=Color.green(color);b+=Color.blue(color);}out[y*w+x]=Color.rgb(r/count,g/count,b/count);}return out;
 }
 private static int[] mosaic(int[] in,int w,int h,int block){
  int[] out=new int[in.length];for(int y=0;y<h;y+=block)for(int x=0;x<w;x+=block){int r=0,g=0,b=0,n=0,ex=Math.min(w,x+block),ey=Math.min(h,y+block);
   for(int yy=y;yy<ey;yy++)for(int xx=x;xx<ex;xx++){int color=in[yy*w+xx];r+=Color.red(color);g+=Color.green(color);b+=Color.blue(color);n++;}
   int color=Color.rgb(r/n,g/n,b/n);for(int yy=y;yy<ey;yy++)java.util.Arrays.fill(out,yy*w+x,yy*w+ex,color);
  }return out;
 }
 private static int clamp(int n,int size){return Math.max(0,Math.min(size-1,n));}
 void clear(){if(cached!=null){cached.recycle();cached=null;}source=null;}
}
