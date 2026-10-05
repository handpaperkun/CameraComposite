package com.framecamera.app;

import android.graphics.*;

/** Local edge matting: preserve interior lettering and decontaminate background-colored fringes. */
final class EdgeMatte {
 /** A reviewed exterior contour for white hardware on a white studio background. */
 static void retainOutline(Bitmap bitmap,float[] points,int left,int top){
  if(points.length<6)return;Path outside=new Path();outside.setFillType(Path.FillType.EVEN_ODD);outside.addRect(0,0,bitmap.getWidth(),bitmap.getHeight(),Path.Direction.CW);
  outside.moveTo(points[0]-left,points[1]-top);for(int i=2;i<points.length;i+=2)outside.lineTo(points[i]-left,points[i+1]-top);outside.close();
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));new Canvas(bitmap).drawPath(outside,p);
 }
 /** Remove disconnected background debris, never interior white lettering.
  * Eight-neighbour connectivity preserves diagonal metal loops and fine edges.
  * Larger detached parts survive; this is deliberately not largest-blob-only.
  */
 static void removeSpecks(Bitmap bitmap){
  int w=bitmap.getWidth(),h=bitmap.getHeight(),count=w*h,limit=Math.max(12,Math.min(256,count/4000));
  int[] pixels=new int[count],queue=new int[count];boolean[] seen=new boolean[count];bitmap.getPixels(pixels,0,w,0,0,w,h);
  for(int start=0;start<count;start++){
   if(seen[start]||Color.alpha(pixels[start])==0)continue;
   int tail=1;queue[0]=start;seen[start]=true;
   for(int head=0;head<tail;head++){int at=queue[head],x=at%w,y=at/w;
    for(int yy=Math.max(0,y-1);yy<=Math.min(h-1,y+1);yy++)for(int xx=Math.max(0,x-1);xx<=Math.min(w-1,x+1);xx++){
     int next=yy*w+xx;if(!seen[next]&&Color.alpha(pixels[next])>0){seen[next]=true;queue[tail++]=next;}
    }
   }
   if(tail<=limit)for(int j=0;j<tail;j++)pixels[queue[j]]=Color.TRANSPARENT;
  }
  bitmap.setPixels(pixels,0,w,0,0,w,h);
 }
 /** Hand-reviewed openings in source-image coordinates; never infer from white. */
 static void cutOpenings(Bitmap bitmap,java.util.List<float[]> polygons,int left,int top){
  if(polygons.isEmpty())return;Canvas canvas=new Canvas(bitmap);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
  for(float[] points:polygons){Path path=new Path();path.moveTo(points[0]-left,points[1]-top);for(int i=2;i<points.length;i+=2)path.lineTo(points[i]-left,points[i+1]-top);path.close();canvas.drawPath(path,paint);}
 }
 static void soften(Bitmap bitmap,int background,boolean keyed){
  int w=bitmap.getWidth(),h=bitmap.getHeight(),count=w*h;int[] pixels=new int[count];bitmap.getPixels(pixels,0,w,0,0,w,h);
  // Capped distance transform limits processing to a narrow silhouette band.
  byte[] distance=new byte[count];int radius=Math.max(2,Math.min(5,Math.round(Math.max(w,h)/700f)));
  for(int i=0;i<count;i++)distance[i]=(byte)(Color.alpha(pixels[i])==0?0:radius+1);
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=y*w+x,d=distance[i];if(x>0)d=Math.min(d,distance[i-1]+1);if(y>0)d=Math.min(d,distance[i-w]+1);distance[i]=(byte)d;}
  for(int y=h-1;y>=0;y--)for(int x=w-1;x>=0;x--){int i=y*w+x,d=distance[i];if(x+1<w)d=Math.min(d,distance[i+1]+1);if(y+1<h)d=Math.min(d,distance[i+w]+1);distance[i]=(byte)d;}
  int[] result=pixels.clone();
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   int index=y*w+x,d=distance[index],color=pixels[index];if(d==0||d>radius)continue;
   int nearest=-1,best=Integer.MAX_VALUE;
   for(int dy=-radius-1;dy<=radius+1;dy++)for(int dx=-radius-1;dx<=radius+1;dx++){
    int xx=x+dx,yy=y+dy;if(xx<0||xx>=w||yy<0||yy>=h)continue;int at=yy*w+xx,delta=dx*dx+dy*dy;
    if(distance[at]>radius&&Color.alpha(pixels[at])==255&&delta<best){nearest=at;best=delta;}
   }
   if(nearest<0)continue;
   int inner=pixels[nearest];float alpha=Color.alpha(color)/255f;
   if(keyed){
    float observed=luma(color),foreground=luma(inner),denominator=background-foreground;
    if(denominator>35&&observed>foreground+5){
     float matte=Math.max(.03f,Math.min(1,(background-observed)/denominator));
     int r=decontaminate(Color.red(color),background,matte),g=decontaminate(Color.green(color),background,matte),b=decontaminate(Color.blue(color),background,matte);
     result[index]=Color.argb(Math.round(255*alpha*matte),r,g,b);
    }
   }else if(luma(color)>luma(inner)+35){
    // Hard-cut transparent PNGs can retain an opaque white outline. Repair only that outline.
    float blend=d==1?.75f:.35f;
    result[index]=Color.argb(Color.alpha(color),mix(Color.red(color),Color.red(inner),blend),mix(Color.green(color),Color.green(inner),blend),mix(Color.blue(color),Color.blue(inner),blend));
   }
  }
  bitmap.setPixels(result,0,w,0,0,w,h);
 }
 private static float luma(int c){return .2126f*Color.red(c)+.7152f*Color.green(c)+.0722f*Color.blue(c);}
 private static int mix(int a,int b,float t){return Math.round(a*(1-t)+b*t);}
 private static int decontaminate(int value,int bg,float alpha){return Math.max(0,Math.min(255,Math.round((value-bg*(1-alpha))/alpha)));}
 private EdgeMatte(){}
}
