package com.framecamera.app;
import android.content.Context;
import android.graphics.*;
import org.json.*;
import java.io.*;
import java.util.*;

public final class CameraCatalog {
 public static final class Model {
  public final String id,name,make,file,source,series,status,qualityNote;
  public final boolean available,scanner;public boolean genericArtwork;public String softwarePrefix;
  public final List<String> aliases=new ArrayList<>();
  public final Set<String> makes=new HashSet<>();
  public final Rect crop,screen;
  public final boolean whiteKey;
  public final float[] floorBoundary;public float[] bodyOutline;
  public final List<float[]> cutoutPolygons=new ArrayList<>();
  public final int keyColor,keyTolerance;
  public final String maskFile;public final Matrix maskToSource;
  Model(JSONObject j)throws JSONException{ scanner=j.optString("deviceType").equals("scanner");genericArtwork=j.optBoolean("genericArtwork",false);softwarePrefix=j.optString("softwarePrefix","");id=j.getString("id");name=j.getString("name");make=j.getString("make");available=j.optBoolean("available",true);series=j.optString("series","");status=j.optString("status","");qualityNote=j.optString("qualityNote","");file=j.optString("file","");source=j.getString("sourcePage");JSONArray a=j.getJSONArray("aliases");for(int i=0;i<a.length();i++)aliases.add(PhotoMetadata.normalize(a.getString(i)));makes.add(PhotoMetadata.brand(make));JSONArray brands=j.optJSONArray("makes");if(brands!=null)for(int i=0;i<brands.length();i++)makes.add(PhotoMetadata.brand(brands.getString(i)));crop=available?rect(j.getJSONArray("crop")):new Rect();screen=available?rect(j.getJSONArray("screen")):new Rect();JSONArray outline=j.optJSONArray("bodyOutline");bodyOutline=new float[outline==null?0:outline.length()];for(int k=0;k<bodyOutline.length;k++)bodyOutline[k]=(float)outline.getDouble(k);JSONArray boundary=j.optJSONArray("floorBoundary");floorBoundary=boundary==null?new float[0]:new float[boundary.length()];for(int k=0;k<floorBoundary.length;k++)floorBoundary[k]=(float)boundary.getDouble(k);JSONArray openings=j.optJSONArray("cutoutPolygons");if(openings!=null)for(int k=0;k<openings.length();k++){JSONArray polygon=openings.getJSONArray(k);if(polygon.length()<6||polygon.length()%2!=0)throw new JSONException("Invalid opening polygon");float[] points=new float[polygon.length()];for(int n=0;n<points.length;n++)points[n]=(float)polygon.getDouble(n);cutoutPolygons.add(points);}whiteKey=j.optBoolean("whiteKey",false);keyColor=j.optInt("keyColor",255);keyTolerance=j.optInt("keyTolerance",23);maskFile=j.optString("maskFile","");maskToSource=new Matrix();JSONArray transform=j.optJSONArray("sourceToMask");if(transform!=null){Matrix sourceToMask=new Matrix();sourceToMask.setValues(new float[]{(float)transform.getDouble(0),(float)transform.getDouble(1),(float)transform.getDouble(2),(float)transform.getDouble(3),(float)transform.getDouble(4),(float)transform.getDouble(5),0,0,1});if(!sourceToMask.invert(maskToSource))throw new JSONException("Invalid silhouette transform");maskToSource.postTranslate(-crop.left,-crop.top);} }
  private Rect rect(JSONArray a)throws JSONException{return new Rect(a.getInt(0),a.getInt(1),a.getInt(2),a.getInt(3));}
 }
 public final List<Model> models=new ArrayList<>(),library=new ArrayList<>();
 public CameraCatalog(Context c)throws Exception{
  ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getAssets().open("cameras/catalog.json")){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
  JSONObject root=new JSONObject(out.toString("UTF-8"));JSONArray a=root.getJSONArray("models");for(int i=0;i<a.length();i++)models.add(new Model(a.getJSONObject(i)));library.addAll(models);JSONArray pending=root.optJSONArray("pendingModels");if(pending!=null)for(int i=0;i<pending.length();i++)library.add(new Model(pending.getJSONObject(i)));
 }
 public Model match(PhotoMetadata m){
  if(m.make.isEmpty())return null;String brand=PhotoMetadata.brand(m.make),raw=PhotoMetadata.normalize(m.model);Model found=null;
  for(Model item:models)if(item.makes.contains(brand)&&item.aliases.contains(raw)){if(found!=null)return null;found=item;}
  if(found!=null)return found;
  // Software identifies a workflow, not hardware. Never override a concrete unknown model.
  for(Model item:models)if(item.genericArtwork&&item.makes.contains(brand)){
   String value=m.model.isEmpty()?m.software:m.model;
   if(softwareName(value,item.softwarePrefix)){if(found!=null)return null;found=item;}
  }
  return found;
 }
 private static boolean softwareName(String value,String prefix){return !prefix.isEmpty()&&value.trim().matches("(?i)"+java.util.regex.Pattern.quote(prefix)+"(?:\\s+v?\\d+(?:[.\\-]\\d+)*)?");}
 private Bitmap silhouette(Context context,Bitmap cropped,Model model)throws IOException{
  Bitmap mask;try(InputStream in=context.getAssets().open("cameras/"+model.maskFile)){mask=BitmapFactory.decodeStream(in);}if(mask==null)throw new IOException("机模轮廓损坏");
  Bitmap result=cropped.copy(Bitmap.Config.ARGB_8888,true);result.setHasAlpha(true);cropped.recycle();
  // Multiply sampled alpha explicitly: ALPHA_8 drawBitmap can treat zero alpha as no coverage.
  int w=result.getWidth(),h=result.getHeight(),mw=mask.getWidth(),mh=mask.getHeight();int[] pixels=new int[w*h],outline=new int[mw*mh];result.getPixels(pixels,0,w,0,0,w,h);mask.getPixels(outline,0,mw,0,0,mw,mh);mask.recycle();
  Matrix inverse=new Matrix();model.maskToSource.invert(inverse);float[] matrix=new float[9];inverse.getValues(matrix);
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   float mx=matrix[0]*(x+.5f)+matrix[1]*(y+.5f)+matrix[2]-.5f,my=matrix[3]*(x+.5f)+matrix[4]*(y+.5f)+matrix[5]-.5f;int ix=(int)Math.floor(mx),iy=(int)Math.floor(my);float fx=mx-ix,fy=my-iy;
   float a=alpha(outline,mw,mh,ix,iy)*(1-fx)*(1-fy)+alpha(outline,mw,mh,ix+1,iy)*fx*(1-fy)+alpha(outline,mw,mh,ix,iy+1)*(1-fx)*fy+alpha(outline,mw,mh,ix+1,iy+1)*fx*fy;
   int index=y*w+x;colorAlpha(pixels,index,Math.round(Color.alpha(pixels[index])*a/255));
  }
  result.setPixels(pixels,0,w,0,0,w,h);
  finishMatte(result,model,true);return result;
 }
 private static void finishMatte(Bitmap bitmap,Model model,boolean keyed){EdgeMatte.retainOutline(bitmap,model.bodyOutline,model.crop.left,model.crop.top);EdgeMatte.cutOpenings(bitmap,model.cutoutPolygons,model.crop.left,model.crop.top);EdgeMatte.removeSpecks(bitmap);EdgeMatte.soften(bitmap,model.keyColor,keyed);}
 private static int alpha(int[] values,int w,int h,int x,int y){return x<0||x>=w||y<0||y>=h?0:Color.alpha(values[y*w+x]);}
 private static void colorAlpha(int[] pixels,int index,int alpha){pixels[index]=(pixels[index]&0x00ffffff)|(alpha<<24);}
 public Bitmap load(Context context,Model model)throws IOException{
  if(!model.available)throw new IOException(model.status);
  if(model.genericArtwork)return ScannerArtwork.create();
  Bitmap b;try(InputStream in=context.getAssets().open("cameras/"+model.file)){b=BitmapFactory.decodeStream(in);}if(b==null)throw new IOException("机模文件损坏");
  if(!new Rect(0,0,b.getWidth(),b.getHeight()).contains(model.crop))throw new IOException("机模坐标不正确");
  Bitmap cropped=Bitmap.createBitmap(b,model.crop.left,model.crop.top,model.crop.width(),model.crop.height());if(cropped!=b)b.recycle();if(!model.maskFile.isEmpty())return silhouette(context,cropped,model);if(!model.whiteKey){Bitmap cleaned=cropped.copy(Bitmap.Config.ARGB_8888,true);cleaned.setHasAlpha(true);cropped.recycle();finishMatte(cleaned,model,false);return cleaned;}
  // Remove only the border-connected white background, preserving white camera markings.
  Bitmap result=cropped.copy(Bitmap.Config.ARGB_8888,true);result.setHasAlpha(true);cropped.recycle();int w=result.getWidth(),h=result.getHeight();int[] p=new int[w*h],q=new int[w*h];boolean[] seen=new boolean[w*h];result.getPixels(p,0,w,0,0,w,h);
  int tail=0;for(int x=0;x<w;x++){q[tail++]=x;seen[x]=true;if(h>1){q[tail++]=(h-1)*w+x;seen[(h-1)*w+x]=true;}}for(int y=1;y<h-1;y++){q[tail++]=y*w;seen[y*w]=true;if(w>1){q[tail++]=y*w+w-1;seen[y*w+w-1]=true;}}
  for(int head=0;head<tail;head++){int at=q[head],color=p[at];if(Math.abs(Color.red(color)-model.keyColor)>model.keyTolerance||Math.abs(Color.green(color)-model.keyColor)>model.keyTolerance||Math.abs(Color.blue(color)-model.keyColor)>model.keyTolerance)continue;p[at]=Color.TRANSPARENT;int x=at%w,y=at/w;int[] near={x>0?at-1:-1,x<w-1?at+1:-1,y>0?at-w:-1,y<h-1?at+w:-1};for(int n:near)if(n>=0&&!seen[n]){seen[n]=true;q[tail++]=n;}}
  if(model.floorBoundary.length>=4){float[] line=model.floorBoundary;for(int x=0;x<w;x++){float originalX=x+model.crop.left;int k=0;while(k+3<line.length&&originalX>line[k+2])k+=2;if(k+3>=line.length)k=line.length-4;float t=Math.max(0,Math.min(1,(originalX-line[k])/(line[k+2]-line[k]))),limit=line[k+1]+t*(line[k+3]-line[k+1])-model.crop.top;for(int y=Math.max(0,(int)Math.floor(limit));y<h;y++){int at=y*w+x;float coverage=Math.max(0,Math.min(1,limit-y));p[at]=(p[at]&0x00ffffff)|(Math.round(Color.alpha(p[at])*coverage)<<24);}}}
  result.setPixels(p,0,w,0,0,w,h);finishMatte(result,model,true);return result;
 }
}
