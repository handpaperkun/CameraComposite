package com.framecamera.app;
import android.graphics.*;

/** Deliberately schematic, never the appearance of a guessed hardware model. */
final class ScannerArtwork {
 static Bitmap create(){
  Bitmap b=Bitmap.createBitmap(640,400,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(3);
  p.setColor(0xffc3cec9);c.drawRoundRect(new RectF(65,50,575,327),24,24,p);
  p.setColor(0xff71887d);c.drawRoundRect(new RectF(89,72,551,246),16,16,p);
  p.setColor(0xff20352e);c.drawRoundRect(new RectF(96,260,544,286),9,9,p);
  p.setColor(0xff293a32);c.drawRect(190,228,450,360,p);
  for(int i=0;i<6;i++){p.setColor(0xffd9e4db);c.drawRect(199,239+i*19,210,249+i*19,p);c.drawRect(430,239+i*19,441,249+i*19,p);}
  p.setColor(0xffaabfb2);c.drawRect(226,245,414,345,p);
  p.setColor(0xff4a6658);c.drawCircle(363,273,14,p);
  Path hill=new Path();hill.moveTo(226,345);hill.lineTo(282,282);hill.lineTo(330,326);hill.lineTo(370,303);hill.lineTo(414,345);hill.close();c.drawPath(hill,p);
  p.setColor(0xffdbe6dc);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(29);c.drawText("FILM SCAN",320,168,p);
  return b;
 }
 private ScannerArtwork(){}
}
