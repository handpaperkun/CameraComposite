package com.framecamera.app;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Read-only viewer: pan/zoom never mutates composition or export options. */
public final class ZoomPreview extends Dialog {
 private Bitmap bitmap;private final RectF start;
 ZoomView imageView;
 public ZoomPreview(Context c,Bitmap image,RectF initial){super(c,android.R.style.Theme_Material_NoActionBar);bitmap=image;start=new RectF(initial);}
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(0);getWindow().setStatusBarColor(0xff101311);getWindow().setNavigationBarColor(0xff101311);LinearLayout root=new LinearLayout(getContext());root.setOrientation(1);root.setBackgroundColor(0xff101311);
  root.setOnApplyWindowInsetsListener((v,in)->{v.setPadding(0,in.getSystemWindowInsetTop(),0,in.getSystemWindowInsetBottom());return in;});
  Button close=new Button(getContext());close.setText("关闭放大查看");close.setTextColor(Color.WHITE);close.setBackgroundColor(0xff26352e);close.setOnClickListener(v->dismiss());root.addView(close,new LinearLayout.LayoutParams(-1,dp(48)));
  imageView=new ZoomView(getContext());root.addView(imageView,new LinearLayout.LayoutParams(-1,0,1));TextView hint=new TextView(getContext());hint.setText("双指缩放 · 拖动查看 · 双击复位");hint.setTextColor(0xffc5cec7);hint.setGravity(Gravity.CENTER);root.addView(hint,new LinearLayout.LayoutParams(-1,dp(40)));setContentView(root);getWindow().setLayout(-1,-1);
 }
 private int dp(int n){return Math.round(n*getContext().getResources().getDisplayMetrics().density);}
 @Override public void dismiss(){super.dismiss();if(bitmap!=null){bitmap.recycle();bitmap=null;}}
 final class ZoomView extends View {
  final Paint paint=new Paint(3);final ScaleGestureDetector pinch;final GestureDetector taps;float scale,fit,tx,ty;
  ZoomView(Context c){super(c);setContentDescription("放大作品，可双指缩放与拖动");
   pinch=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){zoom(scale*d.getScaleFactor(),d.getFocusX(),d.getFocusY());return true;}});
   taps=new GestureDetector(c,new GestureDetector.SimpleOnGestureListener(){@Override public boolean onDown(MotionEvent e){return true;}@Override public boolean onDoubleTap(MotionEvent e){reset();return true;}@Override public boolean onScroll(MotionEvent a,MotionEvent b,float x,float y){if(!pinch.isInProgress()){tx-=x;ty-=y;clamp();invalidate();}return true;}});
  }
  @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){if(bitmap==null)return;fit=Math.min((float)w/bitmap.getWidth(),(float)h/bitmap.getHeight());scale=Math.max(fit,Math.min((float)w/start.width(),(float)h/start.height()));tx=w/2f-start.centerX()*scale;ty=h/2f-start.centerY()*scale;clamp();}
  void reset(){scale=fit;tx=(getWidth()-bitmap.getWidth()*scale)/2;ty=(getHeight()-bitmap.getHeight()*scale)/2;invalidate();}
  void zoom(float value,float x,float y){float next=Math.max(fit,Math.min(fit*8,value));tx=x-(x-tx)*next/scale;ty=y-(y-ty)*next/scale;scale=next;clamp();invalidate();}
  void clamp(){if(bitmap==null)return;float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;tx=w<=getWidth()?(getWidth()-w)/2:Math.max(getWidth()-w,Math.min(0,tx));ty=h<=getHeight()?(getHeight()-h)/2:Math.max(getHeight()-h,Math.min(0,ty));}
  @Override public boolean onTouchEvent(MotionEvent e){pinch.onTouchEvent(e);taps.onTouchEvent(e);return true;}
  @Override protected void onDraw(Canvas c){if(bitmap==null)return;c.save();c.translate(tx,ty);c.scale(scale,scale);c.drawBitmap(bitmap,0,0,paint);c.restore();}
 }
}
