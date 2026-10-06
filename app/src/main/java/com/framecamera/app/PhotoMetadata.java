package com.framecamera.app;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import java.util.*;
import java.text.Normalizer;

/** Immutable EXIF values: display edits never change identification or exposure. */
public final class PhotoMetadata {
 public final String make,model,lens,focal,aperture,shutter,iso,software,droneModel;
 PhotoMetadata(String make,String model,String lens,String focal,String aperture,String shutter,String iso){
  this(make,model,lens,focal,aperture,shutter,iso,"");
 }
 PhotoMetadata(String make,String model,String lens,String focal,String aperture,String shutter,String iso,String software){
  this(make,model,lens,focal,aperture,shutter,iso,software,"");
 }
 PhotoMetadata(String make,String model,String lens,String focal,String aperture,String shutter,String iso,String software,String droneModel){
  this.droneModel=clean(droneModel);this.software=clean(software);this.make=clean(make);this.model=clean(model);this.lens=clean(lens);this.focal=focal;this.aperture=aperture;this.shutter=shutter;this.iso=iso;
 }
 public static PhotoMetadata read(InputStream in)throws IOException{
  ExifInterface e=new ExifInterface(in);
  double f=e.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH,Double.NaN),a=e.getAttributeDouble(ExifInterface.TAG_F_NUMBER,Double.NaN),s=e.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME,Double.NaN);
  String iso=e.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY);
  return new PhotoMetadata(e.getAttribute(ExifInterface.TAG_MAKE),e.getAttribute(ExifInterface.TAG_MODEL),e.getAttribute(ExifInterface.TAG_LENS_MODEL),positive(f)?number(f)+" mm":"焦距 —",positive(a)?"f/"+number(a):"光圈 —",exposure(s),iso==null?"ISO —":"ISO "+iso,e.getAttribute(ExifInterface.TAG_SOFTWARE),droneFromXmp(e.getAttribute(ExifInterface.TAG_XMP)));
 }
 static String droneFromXmp(String xml){
  if(xml==null||xml.length()>1024*1024||xml.contains("<!DOCTYPE")||xml.contains("<!ENTITY"))return "";
  try{org.xmlpull.v1.XmlPullParser parser=android.util.Xml.newPullParser();parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);parser.setInput(new StringReader(xml));String found="";
   for(int event=parser.next();event!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT;event=parser.next())if(event==org.xmlpull.v1.XmlPullParser.START_TAG){
    String value="";if("DroneModel".equals(parser.getName())&&djiNamespace(parser.getNamespace()))value=parser.nextText();
    else for(int i=0;i<parser.getAttributeCount();i++)if("DroneModel".equals(parser.getAttributeName(i))&&djiNamespace(parser.getAttributeNamespace(i)))value=parser.getAttributeValue(i);
    if(!clean(value).isEmpty()){if(!found.isEmpty()&&!normalize(found).equals(normalize(value)))return "";found=clean(value);}
   }return found.length()<=100?found:"";
  }catch(Exception ignored){return "";}
 }
 private static boolean djiNamespace(String value){return "http://www.dji.com/drone-dji/1.0/".equals(value);}
 static boolean positive(double n){return Double.isFinite(n)&&n>0;}
 static String clean(String s){return s==null?"":s.replace('\0',' ').trim();}
 public static String number(double n){return String.format(Locale.US,"%.6f",n).replaceAll("0+$","").replaceAll("\\.$","");}
 public static String exposure(double s){if(!positive(s))return "快门 —";if(s<1){double r=1/s;long n=Math.round(r);if(n>=2&&Math.abs(r-n)/r<.002)return "1/"+n+" s";}return number(s)+" s";}
 public String parameters(){return focal+"   ·   "+shutter+"   ·   "+aperture+"   ·   "+iso;}
 public String cameraName(){if(model.isEmpty())return make;String maker=brand(make);return maker.isEmpty()||normalize(model).startsWith(maker)?model:(make+" "+model).trim();}
 public boolean scanHint(){String b=brand(make),m=normalize(model),s=normalize(software);return b.startsWith("NORITSU")||b.equals("FUJIPHOTOFILMCOLTD")||b.equals("FUJIFILM")&&m.matches("(?:FRONTIER)?SP[0-9]+")||b.equals("EPSON")&&m.contains("PERFECTION")||(b.startsWith("HASSELBLAD")||b.startsWith("IMACON"))&&(m.startsWith("FLEXTIGHT")||m.startsWith("FLEXCOLOR"))||s.startsWith("VUESCAN")||s.startsWith("SILVERFAST");}
 public String scanDescription(){return "扫描设备："+cameraName()+(software.isEmpty()?"":"\n扫描软件："+software)+"\n扫描文件不能确定胶片相机、镜头或胶片感光度。";}
 public String unmatchedReason(){if(normalize(model).equals("L2D20C")&&droneModel.isEmpty())return "Mavic 3 系列共用 L2D-20c，照片未保留飞行器型号，无法确定具体机身";if(scanHint())return "扫描信息："+cameraName()+" · 尚无可准确匹配的扫描仪素材";return make.isEmpty()||model.isEmpty()?"设备 EXIF 缺少品牌或型号，请导入保留信息的原始照片":"尚未适配："+cameraName()+" · 暂无法导出机模作品";}
 public static String normalize(String s){return Normalizer.normalize(clean(s),Normalizer.Form.NFKC).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]","");}
 public static String brand(String s){String n=normalize(s);if(n.equals("DJITECHNOLOGYCOLTD")||n.equals("SZDJITECHNOLOGYCOLTD"))return "DJI";if(n.equals("NIKONCORPORATION"))return "NIKON";if(n.equals("RICOHIMAGINGCOMPANYLTD")||n.equals("RICOHIMAGING"))return "RICOH";if(n.equals("SIGMACORPORATION"))return "SIGMA";if(n.equals("OMDIGITALSOLUTIONS"))return "OMSYSTEM";if(n.equals("LEICACAMERAAG"))return "LEICA";return n;}
}
