package com.framecamera.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.*;
import android.view.animation.DecelerateInterpolator;

/** One fixed-size poster surface, translated with the drawer. No per-frame remeasure. */
public final class EditorStage extends ViewGroup {
 public interface Listener {void changed(boolean open);}
 public interface PreviewGeometry {android.graphics.RectF protectedBounds(int width,int height);}
 private float previewShift;
 private boolean vertical=true,dockVertical=true,leading=false,open=false,dragging=false,enabled=true,hideActions=false;
 private float progress=0,startProgress,downX,downY;
 private int distance,actionsHeight;
 private ValueAnimator animator;
 private Listener listener;
 public EditorStage(Context c){super(c);setClipChildren(true);setChildrenDrawingOrderEnabled(true);}
 public void setListener(Listener l){listener=l;}
 public boolean isOpen(){return open;}
 public float progress(){return progress;}
 public boolean isVertical(){return vertical;}
 public boolean isLeading(){return leading;}
 public boolean isDockVertical(){return dockVertical;}
 public int drawerDistance(){return distance;}
 public void setActionsHidden(boolean hidden){if(hideActions!=hidden){hideActions=hidden;requestLayout();}}
 public void configure(boolean v,boolean first,boolean active){
  vertical=true;leading=false;enabled=active;
  if(!active&&(open||progress>0))setOpen(false,false);requestLayout();
 }
 private void move(float value){progress=Math.max(0,Math.min(1,value));applyPositions();if(listener!=null)listener.changed(progress>0);}
 public void setOpen(boolean value,boolean animate){
  if(value&&!enabled)return;open=value;if(animator!=null)animator.cancel();
  if(!animate){move(value?1:0);return;}
  animator=ValueAnimator.ofFloat(progress,value?1:0);animator.setDuration(Math.max(80,Math.round(260*Math.abs((value?1:0)-progress))));animator.setInterpolator(new DecelerateInterpolator());
  animator.addUpdateListener(a->move((float)a.getAnimatedValue()));animator.start();
 }
 /** Attach only to preview and drawer handle. Slider touches remain with their controls. */
 public boolean gesture(View source,MotionEvent e){
  if(!enabled)return false;
  if(e.getActionMasked()==MotionEvent.ACTION_DOWN){if(animator!=null)animator.cancel();downX=e.getRawX();downY=e.getRawY();startProgress=progress;dragging=false;return true;}
  float delta=vertical?e.getRawY()-downY:e.getRawX()-downX,cross=vertical?e.getRawX()-downX:e.getRawY()-downY,toward=delta*(leading?1:-1);
  if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
   if(!dragging&&Math.abs(delta)>ViewConfiguration.get(getContext()).getScaledTouchSlop()&&Math.abs(delta)>Math.abs(cross)*1.2f){dragging=true;getParent().requestDisallowInterceptTouchEvent(true);}
   if(dragging)move(startProgress+toward/Math.max(1,distance));return true;
  }
  if(e.getActionMasked()==MotionEvent.ACTION_UP){if(dragging){boolean target=Math.abs(toward)>48*getResources().getDisplayMetrics().density?toward>0:progress>.5f;setOpen(target,true);}else{setOpen(open,true);source.performClick();}return true;}
  if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){setOpen(open,true);return true;}return true;
 }
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec);setMeasuredDimension(w,h);
  dockVertical=true;float density=getResources().getDisplayMetrics().density;
  View actions=getChildAt(2);actions.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.round(h*.42f),MeasureSpec.AT_MOST));actionsHeight=hideActions?0:actions.getMeasuredHeight();
  int previewHeight=Math.max(1,h-actionsHeight);
  getChildAt(0).measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(previewHeight,MeasureSpec.EXACTLY));
  android.graphics.RectF protectedArea=getChildAt(0) instanceof PreviewGeometry?((PreviewGeometry)getChildAt(0)).protectedBounds(w,previewHeight):new android.graphics.RectF(0,0,w,previewHeight*.5f);
  float margin=8*density;
  // Grow into free space without scaling the poster or clipping the device panel.
  distance=Math.round(Math.max(0,Math.min(h*.65f,h-protectedArea.height()-2*margin)));
  previewShift=Math.min(0,h-distance-margin-protectedArea.bottom);
  previewShift=Math.max(previewShift,Math.min(0,margin-protectedArea.top));
  getChildAt(1).measure(MeasureSpec.makeMeasureSpec(dockVertical?w:distance,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(dockVertical?distance:h,MeasureSpec.EXACTLY));
 }
 @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
  int w=r-l,h=b-t;getChildAt(0).layout(0,0,w,h-actionsHeight);getChildAt(2).layout(0,h-actionsHeight,w,h);
  getChildAt(1).layout(0,0,dockVertical?w:distance,dockVertical?distance:h);applyPositions();
 }
 private void applyPositions(){
  if(getChildCount()<3)return;int w=getWidth(),h=getHeight();View preview=getChildAt(0),menu=getChildAt(1),actions=getChildAt(2);
  float shift=-previewShift*progress;
  preview.setTranslationX(dockVertical?0:(leading?shift:-shift));preview.setTranslationY(dockVertical?(leading?shift:-shift):0);
  menu.setTranslationX(dockVertical?0:(leading?-distance+distance*progress:w-distance*progress));
  menu.setTranslationY(dockVertical?(leading?-distance+distance*progress:h-distance*progress):0);
  actions.setTranslationY(actionsHeight*progress);actions.setAlpha(1-progress);
  actions.setVisibility(hideActions||progress>=1?INVISIBLE:VISIBLE);menu.setVisibility(progress<=0?INVISIBLE:VISIBLE);
  actions.setImportantForAccessibility(progress>0?IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS:IMPORTANT_FOR_ACCESSIBILITY_AUTO);
  menu.setImportantForAccessibility(progress<=0?IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS:IMPORTANT_FOR_ACCESSIBILITY_AUTO);
 }
 @Override protected int getChildDrawingOrder(int count,int index){return count==3?(index==1?2:index==2?1:0):index;}
 @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
}

