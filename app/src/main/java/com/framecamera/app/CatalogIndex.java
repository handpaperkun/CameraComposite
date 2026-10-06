package com.framecamera.app;
import java.util.*;

/** Browsing groups only; never used by the EXIF model matcher. */
public final class CatalogIndex {
 public final LinkedHashMap<String,TreeMap<String,List<CameraCatalog.Model>>> brands=new LinkedHashMap<>();
 public CatalogIndex(List<CameraCatalog.Model> models){
  String[][] names={{"DJI","大疆"},{"SONY","索尼"},{"NIKON","尼康"},{"CANON","佳能"},{"FUJIFILM","富士"},{"HASSELBLAD","哈苏"},{"PANASONIC","松下"},{"OLYMPUS","奥林巴斯"},{"RICOH","理光"},{"PENTAX","宾得"},{"SIGMA","适马"},{"LEICA","徕卡"},{"NORITSU","诺日士"},{"EPSON","爱普生"}};
  for(String[] name:names){TreeMap<String,List<CameraCatalog.Model>> groups=new TreeMap<>();for(CameraCatalog.Model m:models)if(m.make.equalsIgnoreCase(name[0]))groups.computeIfAbsent(series(m),k->new ArrayList<>()).add(m);for(List<CameraCatalog.Model> list:groups.values())list.sort(Comparator.comparing(m->m.name));if(!groups.isEmpty())brands.put(name[1]+" · "+name[0],groups);}
 }
 public List<CameraCatalog.Model> search(String query){
  String raw=query.trim().toLowerCase(Locale.ROOT),key=PhotoMetadata.normalize(query);List<CameraCatalog.Model> results=new ArrayList<>();
  if(raw.isEmpty())return results;
  for(Map.Entry<String,TreeMap<String,List<CameraCatalog.Model>>> brand:brands.entrySet())for(List<CameraCatalog.Model> group:brand.getValue().values())for(CameraCatalog.Model m:group){
   boolean found=brand.getKey().toLowerCase(Locale.ROOT).contains(raw)||m.name.toLowerCase(Locale.ROOT).contains(raw);
   if(!key.isEmpty()){found|=PhotoMetadata.normalize(m.name).contains(key);for(String alias:m.aliases)found|=alias.contains(key);}
   if(found)results.add(m);
  }
  results.sort(Comparator.comparing(m->m.name));return results;
 }
 public static String series(CameraCatalog.Model m){
  if(!m.series.isEmpty())return m.series;
  String make=m.make.toUpperCase(Locale.ROOT),n=m.name.toUpperCase(Locale.ROOT).replaceFirst("^\\S+\\s+", "");
  switch(make){
   case "SONY":if(n.startsWith("ZV"))return "ZV 系列";if(n.startsWith("FX"))return "FX 系列";if(n.startsWith("ILCE-"))n="A"+n.substring(5);if(n.startsWith("A7"))return "α7 系列";if(n.startsWith("A9"))return "α9 系列";if(n.startsWith("A1"))return "α1 系列";return "α6000 系列";
   case "NIKON":if(n.startsWith("Z"))return "Z 系列";if(n.startsWith("D"))return "D 系列";return "COOLPIX 系列";
   case "CANON":n=n.replace("EOS ","");if(n.startsWith("R"))return "EOS R 系列";if(n.startsWith("M"))return "EOS M 系列";if(n.startsWith("G"))return "PowerShot G 系列";return "EOS 单反系列";
   case "FUJIFILM":if(n.startsWith("GFX"))return "GFX 系列";if(n.startsWith("X100"))return "X100 系列";return n.length()>3?n.substring(0,3)+" 系列":"X 系列";
   case "HASSELBLAD":return "X 系列";
   case "PANASONIC":if(n.startsWith("S"))return "LUMIX S 系列";if(n.startsWith("GH"))return "LUMIX GH 系列";if(n.startsWith("GX"))return "LUMIX GX 系列";if(n.startsWith("G"))return "LUMIX G 系列";if(n.startsWith("FZ"))return "LUMIX FZ 系列";if(n.startsWith("LX"))return "LUMIX LX 系列";return "LUMIX ZS / TZ 系列";
   case "OLYMPUS":if(n.startsWith("E-M"))return "OM-D 系列";if(n.startsWith("E-P")||n.startsWith("PEN"))return "PEN 系列";if(n.startsWith("TG"))return "Tough TG 系列";return "SH 系列";
   case "RICOH":return n.contains("DIGITAL")?"GR DIGITAL 系列":"GR 系列";
   case "PENTAX":return "K 系列";
   case "SIGMA":if(n.startsWith("FP"))return "fp 系列";if(n.startsWith("BF"))return "BF 系列";return n.startsWith("DP")?"dp Quattro 系列":"sd Quattro 系列";
   case "LEICA":for(String prefix:new String[]{"D-LUX","V-LUX","C-LUX","SL","TL","CL","Q","M"})if(n.startsWith(prefix))return prefix+" 系列";return "其他系列";
   default:return "其他系列";
  }
 }
}
