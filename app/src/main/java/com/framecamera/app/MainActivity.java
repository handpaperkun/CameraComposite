package com.framecamera.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private static final int PICK=10,SAVE=11;
 private int BG=0xfff5f3ee,INK=0xff263e35,MUTED=0xff667366,GREEN=0xff346a5a,SURFACE=0xeeffffff,SECONDARY=0xffe7eae3;
 private boolean dark,focusCamera,keyboardVisible;
 private boolean scannerMode(){return model!=null&&model.scanner;}
 private ZoomPreview zoomDialog;
 private AlertDialog catalogDialog;
 private String catalogBrand,catalogSeries;
 private CatalogIndex catalogIndex;
 private ListView catalogList;
 private EditText catalogSearch;
 private EditorStage stage;
 private LinearLayout drawer;
 private LinearLayout[] sections;
 private Button collageButton,cameraOnlyButton,detailButton;
 private Button drawerHandle;
 private Spinner sectionPicker;
 private int activeSection=1;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Handler main=new Handler(Looper.getMainLooper());
 private CameraCatalog catalog;
 private CameraCatalog.Model model;
 private PhotoMetadata metadata;
 private Bitmap photo,body;
 private Uri selected;
 private PosterRenderer.Options options=new PosterRenderer.Options();
 private PosterRenderer.Options pendingOptions;
 private String pendingFormat="image/jpeg";
 private int exportEdge=4096;
 private boolean busy=false,updating=false;
 private LinearLayout root,controls,bottomActions;
 private TextView scannerScreenNote;
 private TextView status,details,exportInfo;
 private EditText cameraName,lensName;
 private Button importButton,exportButton,verticalButton,horizontalButton,importSourceButton;
 private int importSource=PhotoImport.FILES;
 private AlertDialog importSourceDialog;
 private Switch portraitFlipSwitch,reverseSwitch,textSwitch,blurSwitch,signatureSwitch,parametersSwitch,fadeSwitch,clearSizeSwitch;
 private Spinner signatureFont,parameterFont;
 private Slider signatureSize,parameterSize,bodySize,fadeStrength;
 private Switch tintSwitch,reflectionSwitch,reflectionDirection;
 private Spinner tintPicker,blurPicker,screenPresetPicker;private Slider screenPresetAmount;
 private LinearLayout backgroundControls;
 private Slider blurAmount,blurDirection;
 private Slider tintAmount,reflectionAmount;
 private Preview preview;
 private Bundle restore;

 @Override public void onCreate(Bundle state){
  super.onCreate(state);restore=state;
  dark=(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
  if(dark){BG=0xff191d1b;INK=0xffecf0e8;MUTED=0xffb5beb3;GREEN=0xff437e69;SURFACE=0xf02b332e;SECONDARY=0xff354139;}
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  try{FontBook.initialize(this);catalog=new CameraCatalog(this);}catch(Exception e){new AlertDialog.Builder(this).setTitle("素材加载失败").setMessage(e.getMessage()).setPositiveButton("关闭",(d,w)->finish()).show();return;}
  if(state!=null)options.cameraOnly=state.getBoolean("cameraOnly");
  importSource=getSharedPreferences(PhotoImport.PREFS,MODE_PRIVATE).getInt(PhotoImport.KEY,PhotoImport.FILES);
  if(importSource!=PhotoImport.ALBUM)importSource=PhotoImport.FILES;
  buildUi();
  String old=state==null?null:state.getString("uri");if(old!=null)load(Uri.parse(old),true);
 }
 private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView label(String text,int size,int color){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(dp(3),1);return v;}
 private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private void gap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
 private Button button(String text,boolean primary){Button b=new Button(this);b.setText(text);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(primary?Color.WHITE:INK);b.setBackground(shape(primary?GREEN:SECONDARY,14));b.setPadding(dp(12),0,dp(12),0);b.setMinHeight(dp(48));return b;}
 private LinearLayout card(){LinearLayout l=column();l.setPadding(dp(14),dp(12),dp(14),dp(14));l.setBackground(shape(SURFACE,18));return l;}
 private void buildUi(){
  root=column();try{root.setBackground(new TextureBackground(this,dark));}catch(IOException e){root.setBackgroundColor(BG);}
  root.setPadding(dp(14),dp(10),dp(14),dp(10));
  root.setOnApplyWindowInsetsListener((v,in)->{
   int bottom=in.getSystemWindowInsetBottom();boolean keyboard=false;
   if(Build.VERSION.SDK_INT>=30){int ime=in.getInsets(WindowInsets.Type.ime()).bottom;keyboard=in.isVisible(WindowInsets.Type.ime());bottom=Math.max(bottom,ime);}
   else keyboard=bottom>dp(150);
   v.setPadding(dp(14),in.getSystemWindowInsetTop()+dp(6),dp(14),bottom+dp(8));
   keyboardVisible=keyboard;if(importButton!=null)updateChrome();
   return in;
  });
  setContentView(root);
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
  TextView title=label(getString(R.string.app_name),24,INK);title.setTypeface(null,Typeface.BOLD);title.setSingleLine(true);title.setAutoSizeTextTypeUniformWithConfiguration(18,24,1,android.util.TypedValue.COMPLEX_UNIT_SP);header.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));title.setGravity(Gravity.CENTER_VERTICAL);
  importSourceButton=button("",false);importSourceButton.setTextSize(12);importSourceButton.setSingleLine(true);importSourceButton.setPadding(dp(8),0,dp(8),0);importSourceButton.setOnClickListener(v->showImportSource());updateImportSource();LinearLayout.LayoutParams sourceLp=new LinearLayout.LayoutParams(dp(76),dp(42));sourceLp.setMarginEnd(dp(8));header.addView(importSourceButton,sourceLp);
  Button library=button("机模库 ↗",false);library.setTextSize(12);library.setSingleLine(true);library.setPadding(dp(8),0,dp(8),0);library.setOnClickListener(v->showCatalog());header.addView(library,new LinearLayout.LayoutParams(dp(82),dp(42)));
  root.addView(header);gap(root,8);
  LinearLayout modes=new LinearLayout(this);collageButton=button("照片拼接",false);cameraOnlyButton=button("仅相机",false);LinearLayout.LayoutParams modeLp=new LinearLayout.LayoutParams(0,dp(42),1);modeLp.setMarginEnd(dp(8));modes.addView(collageButton,modeLp);modes.addView(cameraOnlyButton,new LinearLayout.LayoutParams(0,dp(42),1));root.addView(modes);
  collageButton.setOnClickListener(v->{options.cameraOnly=false;refresh();});cameraOnlyButton.setOnClickListener(v->{options.cameraOnly=true;refresh();});gap(root,6);
  stage=new EditorStage(this);root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
  preview=new Preview();preview.setContentDescription("作品预览，可沿机身方向滑动调整");stage.addView(preview);
  drawer=column();drawer.setBackground(shape(SURFACE,18));drawer.setPadding(dp(6),dp(5),dp(6),0);stage.addView(drawer);
  LinearLayout menuHeader=new LinearLayout(this);menuHeader.setGravity(Gravity.CENTER_VERTICAL);drawer.addView(menuHeader);
  sectionPicker=new Spinner(this);sectionPicker.setContentDescription("选择调整项目");sectionPicker.setAdapter(textAdapter(new String[]{"布局与大小","屏幕质感","相机署名","拍摄参数"}));menuHeader.addView(sectionPicker,new LinearLayout.LayoutParams(0,dp(42),1));
  detailButton=button("屏幕细节",false);detailButton.setTextSize(11);detailButton.setOnClickListener(v->showZoom(true));menuHeader.addView(detailButton,new LinearLayout.LayoutParams(dp(84),dp(36)));
  drawerHandle=button("收起",false);drawerHandle.setTextSize(12);drawerHandle.setContentDescription("收起调整菜单，也可反向滑动");drawerHandle.setOnClickListener(v->stage.setOpen(false,true));drawerHandle.setOnTouchListener((v,e)->stage.gesture(v,e));menuHeader.addView(drawerHandle,new LinearLayout.LayoutParams(dp(60),dp(36)));
  ScrollView settingsScroll=new ScrollView(this);settingsScroll.setFillViewport(true);drawer.addView(settingsScroll,new LinearLayout.LayoutParams(-1,0,1));controls=column();settingsScroll.addView(controls);
  stage.setListener(open->{boolean changed=focusCamera!=open;focusCamera=open;if(changed){updateChrome();if(exportInfo!=null)exportInfo.setText(swipeHint());}});
  GestureDetector previewTaps=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){@Override public boolean onDown(MotionEvent e){return true;}@Override public boolean onDoubleTap(MotionEvent e){showZoom();return true;}});
  preview.setOnTouchListener((v,e)->{previewTaps.onTouchEvent(e);return stage.gesture(v,e);});
  bottomActions=column();stage.addView(bottomActions);
  status=label("等待导入 · 机模由 EXIF 自动匹配",11,MUTED);status.setMaxLines(2);bottomActions.addView(status);gap(bottomActions,5);
  importButton=button("＋ 导入原始照片",true);bottomActions.addView(importButton,new LinearLayout.LayoutParams(-1,dp(44)));importButton.setOnClickListener(v->pick());gap(bottomActions,5);
  exportButton=button("导出作品",true);exportButton.setOnClickListener(v->chooseExport());bottomActions.addView(exportButton,new LinearLayout.LayoutParams(-1,dp(44)));
  exportInfo=label("导入照片后滑动调整 · 双击放大",11,MUTED);exportInfo.setGravity(Gravity.CENTER);exportInfo.setMinHeight(dp(40));exportInfo.setOnClickListener(v->stage.setOpen(!stage.isOpen(),true));root.addView(exportInfo,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout layoutCard=card();layoutCard.addView(label("拼接布局",15,INK));gap(layoutCard,8);LinearLayout row=new LinearLayout(this);
  verticalButton=button("↕  上下拼接",false);horizontalButton=button("↔  左右拼接",false);for(Button b:new Button[]{verticalButton,horizontalButton}){b.setTextSize(12);b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(10),0,dp(10),0);}LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(dp(112),dp(36));half.setMarginEnd(dp(8));row.addView(verticalButton,half);row.addView(horizontalButton,new LinearLayout.LayoutParams(dp(112),dp(36)));layoutCard.addView(row);
  verticalButton.setOnClickListener(v->{options.vertical=true;refresh();});horizontalButton.setOnClickListener(v->{options.vertical=false;refresh();});gap(layoutCard,10);
  reverseSwitch=toggle(layoutCard,"互换照片与相机位置",checked->{options.reverse=checked;refresh();});
  portraitFlipSwitch=toggle(layoutCard,"竖图机身旋转 180°",checked->{options.flipPortraitBody=checked;refresh();});
  textSwitch=toggle(layoutCard,"互换相机上下文字",checked->{options.swapText=checked;refresh();});
  blurSwitch=toggle(layoutCard,"照片柔化背景",checked->{options.blur=checked;refresh();});
  backgroundControls=column();layoutCard.addView(backgroundControls);
  blurPicker=new Spinner(this);blurPicker.setAdapter(textAdapter(BackgroundEffects.NAMES));blurPicker.setContentDescription("背景模糊方式");backgroundControls.addView(blurPicker,new LinearLayout.LayoutParams(-1,dp(42)));
  blurPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){}public void onItemSelected(AdapterView<?> a,View v,int position,long id){if(!updating&&options.blurType!=position){options.blurType=position;refresh();}}});
  blurAmount=new Slider(backgroundControls,"背景模糊强度",0,100,value->{options.blurStrength=value/100f;preview.changed();});
  blurDirection=new Slider(backgroundControls,"运动方向",0,180,"°",value->{options.blurDirection=value;preview.changed();});
  backgroundControls.addView(label("0° 横向 · 90° 纵向；只调整相机背景",11,MUTED));
  clearSizeSwitch=toggle(layoutCard,"默认清晰大小",checked->{options.clearBodySize=checked;refresh();});
  layoutCard.addView(label("按主图与机模原始像素密度自动缩放；低分辨率机模会更小",11,MUTED));
  bodySize=new Slider(layoutCard,"机模大小",10,115,value->{options.bodyScale=value/100f;preview.changed();});
  controls.addView(layoutCard);gap(controls,14);
  LinearLayout screenCard=card();scannerScreenNote=label("扫描照片完整显示在设备旁，无需屏幕效果。",13,INK);screenCard.addView(scannerScreenNote);scannerScreenNote.setVisibility(View.GONE);
  screenCard.addView(label("屏幕质感预设",14,INK));
  screenPresetPicker=new Spinner(this);screenPresetPicker.setAdapter(textAdapter(ScreenEffects.PRESET_NAMES));screenPresetPicker.setContentDescription("屏幕质感预设");screenCard.addView(screenPresetPicker,new LinearLayout.LayoutParams(-1,dp(48)));
  screenPresetPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){}public void onItemSelected(AdapterView<?> a,View v,int position,long id){if(!updating&&options.screenPreset!=position){options.screenPreset=position;refresh();}}});
  screenPresetAmount=new Slider(screenCard,"预设强度",0,100,value->{options.screenPresetStrength=value/100f;preview.changed();});
  screenCard.addView(label("模拟轻微偏色与显示失真；不对应特定机型。下方可叠加微调。",11,MUTED));
  tintSwitch=toggle(screenCard,"屏幕偏色滤镜",checked->{options.tintScreen=checked;refresh();});
  tintPicker=new Spinner(this);ArrayAdapter<String> tints=textAdapter(ScreenEffects.TINT_NAMES);tintPicker.setAdapter(tints);tintPicker.setContentDescription("屏幕偏色风格");screenCard.addView(tintPicker,new LinearLayout.LayoutParams(-1,dp(48)));
  tintPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(!updating){options.tintPreset=position;preview.changed();}}});
  tintAmount=new Slider(screenCard,"偏色强度",0,100,value->{options.tintStrength=value/100f;preview.changed();});
  reflectionSwitch=toggle(screenCard,"屏幕玻璃反光",checked->{options.reflectScreen=checked;refresh();});
  reflectionAmount=new Slider(screenCard,"反光强度",0,100,value->{options.reflectionStrength=value/100f;preview.changed();});
  reflectionDirection=toggle(screenCard,"反转反光方向",checked->{options.reverseReflection=checked;preview.changed();});
  fadeSwitch=toggle(screenCard,"减淡屏幕照片",checked->{options.fadeScreen=checked;refresh();});
  fadeStrength=new Slider(screenCard,"减淡程度",0,70,value->{options.fadeAmount=value/100f;preview.changed();});
  screenCard.addView(label("效果仅作用于机身屏幕；可单独开启，也可叠加",11,MUTED));controls.addView(screenCard);gap(controls,14);
  LinearLayout textCard=card();
  signatureSwitch=toggle(textCard,"显示相机署名",checked->{options.showSignature=checked;refresh();});
  textCard.addView(label("相机名与镜头名；修改不影响机模识别",11,MUTED));gap(textCard,14);
  textCard.addView(label("相机名称",12,MUTED));cameraName=field("从 EXIF 读取相机名");textCard.addView(cameraName);gap(textCard,10);textCard.addView(label("镜头名称",12,MUTED));lensName=field("从 EXIF 读取，可补充镜头名");textCard.addView(lensName);
  TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){if(!updating){options.camera=cameraName.getText().toString();options.lens=lensName.getText().toString();preview.changed();}}public void afterTextChanged(Editable e){}};cameraName.addTextChangedListener(watcher);lensName.addTextChangedListener(watcher);
  signatureFont=fontPicker(textCard,value->syncFont(value));
  signatureSize=new Slider(textCard,"统一字号",50,150,value->{syncTextSize(value);});controls.addView(textCard);gap(controls,14);
  LinearLayout exifCard=card();parametersSwitch=toggle(exifCard,"显示拍摄参数",checked->{options.showParameters=checked;refresh();});gap(exifCard,8);details=label("焦距 —     快门 —     光圈 —     ISO —",13,INK);details.setTextIsSelectable(true);exifCard.addView(details);gap(exifCard,8);exifCard.addView(label("仅控制作品中的显示；原始数值不可修改",11,MUTED));
  parameterFont=fontPicker(exifCard,value->syncFont(value));
  parameterSize=new Slider(exifCard,"统一字号",50,150,value->{syncTextSize(value);});controls.addView(exifCard);gap(controls,14);
  sections=new LinearLayout[]{layoutCard,screenCard,textCard,exifCard};
  sectionPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){}public void onItemSelected(AdapterView<?> a,View v,int position,long id){activeSection=position;detailButton.setVisibility(position==1&&!scannerMode()?View.VISIBLE:View.GONE);for(int i=0;i<sections.length;i++)sections[i].setVisibility(i==position?View.VISIBLE:View.GONE);settingsScroll.scrollTo(0,0);}});
  activeSection=restore==null?1:restore.getInt("activeSection",1);sectionPicker.setSelection(activeSection);for(int i=0;i<sections.length;i++)sections[i].setVisibility(i==activeSection?View.VISIBLE:View.GONE);
  // Spacers between hidden cards would otherwise consume the short drawer viewport.
  for(int i=0;i<controls.getChildCount();i++)if(!(controls.getChildAt(i) instanceof LinearLayout))controls.getChildAt(i).setVisibility(View.GONE);
  refresh();
 }
 private void updateChrome(){if(importButton==null)return;stage.setActionsHidden(keyboardVisible);exportInfo.setVisibility(keyboardVisible?View.GONE:View.VISIBLE);drawerHandle.setVisibility(keyboardVisible?View.GONE:View.VISIBLE);}
 private String swipeHint(){if(photo==null)return "导入照片后上滑调整 · 双击放大";if(focusCamera)return "↓ 下滑收起 · 双击放大 · 点击此处收起";return "↑ 上滑调整 · 双击放大 · 点击此处调整";}
 private void syncFont(int value){if(value==options.signatureFont&&value==options.parameterFont)return;options.signatureFont=options.parameterFont=value;updating=true;if(signatureFont!=null)signatureFont.setSelection(value);if(parameterFont!=null)parameterFont.setSelection(value);updating=false;preview.changed();}
 private void syncTextSize(int value){options.signatureSize=options.parameterSize=value/100f;if(signatureSize!=null)signatureSize.set(options.signatureSize);if(parameterSize!=null)parameterSize.set(options.parameterSize);preview.changed();}
 private void showZoom(){showZoom(false);}
 private void showZoom(boolean screen){if(photo==null||busy||(zoomDialog!=null&&zoomDialog.isShowing()))return;try{PosterRenderer renderer=new PosterRenderer();Bitmap image=renderer.render(photo,body,model,metadata,options,3000);float[] size=renderer.size(photo,options);RectF region=screen?renderer.focusBounds(photo,body,model,options,true):preview.region();float scale=image.getWidth()/size[0];region.set(region.left*scale,region.top*scale,region.right*scale,region.bottom*scale);zoomDialog=new ZoomPreview(this,image,region);zoomDialog.show();}catch(OutOfMemoryError e){error("暂时无法放大","请关闭其他应用后重试。");}}
 private ArrayAdapter<String> textAdapter(String[] names){return textAdapter(names,false);}
 private ArrayAdapter<String> textAdapter(String[] names,boolean multiline){return new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names){
  private View styled(View v){TextView t=(TextView)v;t.setTextColor(INK);t.setTextSize(13);if(multiline){t.setSingleLine(false);t.setMaxLines(6);t.setEllipsize(null);t.setLayoutParams(new AbsListView.LayoutParams(-1,-2));}t.setBackgroundColor(dark?0xff2b332e:0xfffcfbf7);t.setPadding(dp(10),dp(10),dp(10),dp(10));return t;}
  @Override public View getView(int p,View v,ViewGroup g){return styled(super.getView(p,v,g));}
  @Override public View getDropDownView(int p,View v,ViewGroup g){return styled(super.getDropDownView(p,v,g));}
 };}
 @Override public void onBackPressed(){if(stage!=null&&stage.isOpen()){stage.setOpen(false,true);return;}super.onBackPressed();}
 private interface Change{void apply(boolean checked);}
 private interface NumberChange{void apply(int value);}
 private Spinner fontPicker(LinearLayout parent,NumberChange change){
  gap(parent,14);parent.addView(label("统一字体 · 署名与参数同步",12,MUTED));Spinner spinner=new Spinner(this);
  ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,FontBook.NAMES){
   private View style(View view,int position){TextView text=(TextView)view;text.setTypeface(FontBook.get(position));text.setBackgroundColor(dark?0xff2b332e:0xfffcfbf7);text.setTextColor(INK);text.setTextSize(16);text.setPadding(dp(10),dp(12),dp(10),dp(12));return view;}
   @Override public View getView(int position,View convert,ViewGroup group){return style(super.getView(position,convert,group),position);}
   @Override public View getDropDownView(int position,View convert,ViewGroup group){return style(super.getDropDownView(position,convert,group),position);}
  };adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);parent.addView(spinner,new LinearLayout.LayoutParams(-1,dp(52)));
  spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> view){}public void onItemSelected(AdapterView<?> view,View item,int position,long id){if(!updating)change.apply(position);}});return spinner;
 }
 private final class Slider {
  final TextView title;final SeekBar seek;final String name,unit;final int min;
  Slider(LinearLayout parent,String name,int min,int max,NumberChange change){this(parent,name,min,max,"%",change);}
  Slider(LinearLayout parent,String name,int min,int max,String unit,NumberChange change){this.name=name;this.unit=unit;this.min=min;gap(parent,10);title=label(name,12,MUTED);parent.addView(title);seek=new SeekBar(MainActivity.this);seek.setMax(max-min);seek.setContentDescription(name);parent.addView(seek,new LinearLayout.LayoutParams(-1,dp(44)));seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int progress,boolean fromUser){title.setText(name+"  ·  "+(progress+min)+unit);if(fromUser&&!updating)change.apply(progress+min);}});}
  void set(float value){int percent=Math.round(unit.equals("°")?value:value*100);seek.setProgress(percent-min);title.setText(name+"  ·  "+percent+unit);}
 }
 private Switch toggle(LinearLayout parent,String title,Change change){Switch s=new Switch(this);s.setText(title);s.setTextSize(13);s.setTextColor(INK);s.setPadding(0,dp(7),0,dp(7));s.setMinHeight(dp(48));parent.addView(s,new LinearLayout.LayoutParams(-1,-2));s.setOnCheckedChangeListener((v,checked)->{if(!updating)change.apply(checked);});return s;}
 private EditText field(String hint){EditText e=new EditText(this);e.setTextSize(14);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setHint(hint);e.setSingleLine(true);e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});e.setBackground(shape(BG,10));e.setPadding(dp(12),dp(12),dp(12),dp(12));return e;}
 private void refresh(){
  boolean loaded=photo!=null;exportButton.setEnabled(loaded&&model!=null&&!busy);exportButton.setAlpha(exportButton.isEnabled()?1:.45f);importButton.setEnabled(!busy);
  setEnabledDeep(controls,loaded&&!busy);controls.setAlpha(loaded?1:.5f);
  verticalButton.setBackground(shape(options.vertical?GREEN:SECONDARY,14));verticalButton.setTextColor(options.vertical?Color.WHITE:INK);horizontalButton.setBackground(shape(!options.vertical?GREEN:SECONDARY,14));horizontalButton.setTextColor(!options.vertical?Color.WHITE:INK);
  updating=true;portraitFlipSwitch.setChecked(options.flipPortraitBody);portraitFlipSwitch.setVisibility(loaded&&!scannerMode()&&photo.getHeight()>photo.getWidth()?View.VISIBLE:View.GONE);reverseSwitch.setChecked(options.reverse);textSwitch.setChecked(options.swapText);blurSwitch.setChecked(options.blur);
  backgroundControls.setVisibility(options.blur?View.VISIBLE:View.GONE);blurPicker.setSelection(options.blurType);blurAmount.set(options.blurStrength);blurDirection.set(options.blurDirection);blurDirection.title.setVisibility(options.blurType==1?View.VISIBLE:View.GONE);blurDirection.seek.setVisibility(options.blurType==1?View.VISIBLE:View.GONE);backgroundControls.getChildAt(backgroundControls.getChildCount()-1).setVisibility(options.blurType==1?View.VISIBLE:View.GONE);
  signatureSwitch.setChecked(options.showSignature);parametersSwitch.setChecked(options.showParameters);fadeSwitch.setChecked(options.fadeScreen);
  signatureFont.setSelection(options.signatureFont);parameterFont.setSelection(options.parameterFont);signatureSize.set(options.signatureSize);parameterSize.set(options.parameterSize);bodySize.set(options.bodyScale);fadeStrength.set(options.fadeAmount);
  fadeStrength.seek.setEnabled(loaded&&!busy&&options.fadeScreen);fadeStrength.title.setAlpha(options.fadeScreen?1:.45f);
  screenPresetPicker.setSelection(options.screenPreset);screenPresetAmount.set(options.screenPresetStrength);screenPresetAmount.seek.setEnabled(loaded&&!busy&&options.screenPreset!=0);screenPresetAmount.title.setAlpha(options.screenPreset==0?.45f:1);
  tintSwitch.setChecked(options.tintScreen);reflectionSwitch.setChecked(options.reflectScreen);reflectionDirection.setChecked(options.reverseReflection);tintPicker.setSelection(options.tintPreset);tintAmount.set(options.tintStrength);reflectionAmount.set(options.reflectionStrength);
  tintPicker.setEnabled(loaded&&!busy&&options.tintScreen);tintAmount.seek.setEnabled(loaded&&!busy&&options.tintScreen);tintAmount.title.setAlpha(options.tintScreen?1:.45f);
  reflectionAmount.seek.setEnabled(loaded&&!busy&&options.reflectScreen);reflectionAmount.title.setAlpha(options.reflectScreen?1:.45f);reflectionDirection.setEnabled(loaded&&!busy&&options.reflectScreen);
  clearSizeSwitch.setChecked(options.clearBodySize);bodySize.seek.setVisibility(options.clearBodySize?View.GONE:View.VISIBLE);
  if(options.clearBodySize&&photo!=null)bodySize.title.setText("机模大小  ·  自动 "+Math.round(PosterRenderer.effectiveBodyScale(photo,body,options)*100)+"%");
  collageButton.setBackground(shape(options.cameraOnly?SECONDARY:GREEN,14));collageButton.setTextColor(options.cameraOnly?INK:Color.WHITE);
  cameraOnlyButton.setBackground(shape(options.cameraOnly?GREEN:SECONDARY,14));cameraOnlyButton.setTextColor(options.cameraOnly?Color.WHITE:INK);
  collageButton.setEnabled(!busy);cameraOnlyButton.setEnabled(!busy);
  verticalButton.setEnabled(loaded&&!busy&&!options.cameraOnly);horizontalButton.setEnabled(loaded&&!busy&&!options.cameraOnly);reverseSwitch.setEnabled(loaded&&!busy&&!options.cameraOnly);
  stage.configure(options.vertical,options.reverse,loaded&&!busy);exportInfo.setEnabled(loaded&&!busy);exportInfo.setText(swipeHint());updateChrome();
  scannerScreenNote.setVisibility(scannerMode()?View.VISIBLE:View.GONE);
  if(sections!=null&&scannerMode())setEnabledDeep(sections[1],false);
  detailButton.setVisibility(activeSection==1&&!scannerMode()?View.VISIBLE:View.GONE);
  parametersSwitch.setEnabled(loaded&&!busy&&!scannerMode());
  parametersSwitch.setText(scannerMode()?"扫描文件无可确认的拍摄参数":"显示拍摄参数");
  signatureSwitch.setText(scannerMode()?"显示扫描仪署名":"显示相机署名");
  cameraName.setHint(scannerMode()?"扫描仪名称":"相机名称");lensName.setVisibility(scannerMode()?View.GONE:View.VISIBLE);
  cameraOnlyButton.setText(scannerMode()?"仅扫描仪":"仅相机");
  updating=false;preview.requestLayout();preview.changed();
 }
 private void setEnabledDeep(View v,boolean enabled){v.setEnabled(enabled);if(v instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)v).getChildCount();i++)setEnabledDeep(((android.view.ViewGroup)v).getChildAt(i),enabled);}
 private void updateImportSource(){importSourceButton.setText(importSource==PhotoImport.ALBUM?"相册 ▾":"文件 ▾");importSourceButton.setContentDescription("导入方式："+(importSource==PhotoImport.ALBUM?"相册":"文件管理")+"，点击切换");}
 private void showImportSource(){
  importSourceDialog=new AlertDialog.Builder(this).setTitle("导入方式")
   .setSingleChoiceItems(new String[]{"文件管理","相册"},importSource,(dialog,which)->{
    boolean saved=getSharedPreferences(PhotoImport.PREFS,MODE_PRIVATE).edit().putInt(PhotoImport.KEY,which).commit();importSource=which;updateImportSource();dialog.dismiss();if(!saved)Toast.makeText(this,"导入方式暂未保存，请检查设备存储空间",Toast.LENGTH_LONG).show();
   }).setNegativeButton("取消",null).create();importSourceDialog.show();
 }
 private void pick(){
  try{startActivityForResult(PhotoImport.intent(importSource,Build.VERSION.SDK_INT),PICK);}
  catch(ActivityNotFoundException e){
   if(importSource==PhotoImport.ALBUM){try{startActivityForResult(PhotoImport.fallbackAlbum(),PICK);return;}catch(ActivityNotFoundException ignored){}}
   error("无法打开导入入口","此设备没有可用的"+(importSource==PhotoImport.ALBUM?"相册":"文件管理")+"应用，请切换另一种导入方式。");
  }
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null){if(request==SAVE){busy=false;pendingOptions=null;refresh();}return;}Uri uri=data.getData();if(request==PICK){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}load(uri,false);}else if(request==SAVE)export(uri);}
 private Bitmap decode(Uri uri,int edge)throws IOException{return decode(uri,edge,null);}
 private Bitmap decode(Uri uri,int edge,int[] originalSize)throws IOException{
  ImageDecoder.Source source=ImageDecoder.createSource(getContentResolver(),uri);
  return ImageDecoder.decodeBitmap(source,(decoder,info,src)->{int w=info.getSize().getWidth(),h=info.getSize().getHeight();if(originalSize!=null){originalSize[0]=w;originalSize[1]=h;}double scale=Math.min(1,(double)edge/Math.max(w,h));decoder.setTargetSize(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));});
 }
 private void load(Uri uri,boolean restoring){
  busy=true;status.setText("正在读取照片与 EXIF…");refresh();
  worker.execute(()->{Bitmap newPhoto=null,newBody=null;try{
   PhotoMetadata meta;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("无法打开照片");meta=PhotoMetadata.read(in);}
   final int[] originalSize=new int[2];newPhoto=decode(uri,1800,originalSize);CameraCatalog.Model matched=catalog.match(meta);if(matched!=null)newBody=catalog.load(this,matched);
   final Bitmap np=newPhoto,nb=newBody;main.post(()->{if(isDestroyed()){np.recycle();if(nb!=null)nb.recycle();return;}if(photo!=null)photo.recycle();if(body!=null)body.recycle();photo=np;body=nb;model=matched;metadata=meta;selected=uri;
    boolean mode=options.cameraOnly;options=new PosterRenderer.Options();options.cameraOnly=mode;options.vertical=photo.getWidth()>=photo.getHeight();options.camera=meta.cameraName();options.lens=meta.lens;options.sourcePhotoWidth=originalSize[0];options.scannerDevice=matched!=null&&matched.scanner;if(options.scannerDevice){options.cameraOnly=false;options.camera=matched.genericArtwork?(meta.model.isEmpty()?meta.make+" · "+meta.software:meta.cameraName()):matched.name;options.lens="";options.showParameters=false;}
    if(restoring&&restore!=null){options.cameraOnly=restore.getBoolean("cameraOnly");options.vertical=restore.getBoolean("vertical",options.vertical);options.reverse=restore.getBoolean("reverse");options.swapText=restore.getBoolean("swapText");options.flipPortraitBody=restore.getBoolean("flipPortraitBody");options.blur=restore.getBoolean("blur");options.blurType=restore.getInt("blurType",0);options.blurStrength=restore.getFloat("blurStrength",.6f);options.blurDirection=restore.getFloat("blurDirection",0);options.camera=restore.getString("camera",options.camera);options.lens=restore.getString("lens",options.lens);
     options.showSignature=restore.getBoolean("showSignature",true);options.showParameters=restore.getBoolean("showParameters",true);options.fadeScreen=restore.getBoolean("fadeScreen");options.signatureFont=restore.getInt("signatureFont",0);options.parameterFont=options.signatureFont;options.signatureSize=restore.getFloat("signatureSize",1);options.parameterSize=options.signatureSize;options.bodyScale=restore.getFloat("bodyScale",1);options.fadeAmount=restore.getFloat("fadeAmount",.3f);options.clearBodySize=restore.getBoolean("clearBodySize");options.tintScreen=restore.getBoolean("tintScreen");options.reflectScreen=restore.getBoolean("reflectScreen");options.reverseReflection=restore.getBoolean("reverseReflection");options.screenPreset=restore.getInt("screenPreset",0);options.screenPresetStrength=restore.getFloat("screenPresetStrength",.65f);options.tintPreset=restore.getInt("tintPreset",0);options.tintStrength=restore.getFloat("tintStrength",.45f);options.reflectionStrength=restore.getFloat("reflectionStrength",.35f);boolean reopen=restore.getBoolean("editorOpen");restore=null;stage.post(()->stage.setOpen(reopen,false));}
    updating=true;cameraName.setText(options.camera);lensName.setText(options.lens);updating=false;details.setText(scannerMode()?meta.scanDescription():meta.focal+"     "+meta.shutter+"\n"+meta.aperture+"     "+meta.iso);if(scannerMode())options.showParameters=false;
    status.setText(matched!=null?(matched.genericArtwork?"已识别扫描流程 · 硬件型号未记录（通用示意）":"已自动匹配  ·  "+matched.name):meta.unmatchedReason());status.setTextColor(matched==null?(dark?0xffefbb8a:0xffa56839):(dark?0xff9ec9b1:GREEN));importButton.setText("＋  更换照片");busy=false;refresh();
   });
  }catch(Exception|OutOfMemoryError e){if(newPhoto!=null)newPhoto.recycle();if(newBody!=null)newBody.recycle();main.post(()->{if(isDestroyed())return;busy=false;status.setText("导入失败，已保留原作品");refresh();error("无法导入", "请使用可正常解码的 JPG、PNG、WebP 或 HEIF 原图。RAW 请先导出为保留 EXIF 的 JPG。\n"+e.getMessage());});}});
 }
 private void chooseExport(){if(model==null||busy)return;new AlertDialog.Builder(this).setTitle("导出作品").setItems(new String[]{"JPG · 长边 4096 px · 画质 95%","PNG · 长边 4096 px · 无损","JPG · 长边 2048 px · 轻量"},(d,w)->{
  pendingFormat=w==1?"image/png":"image/jpeg";exportEdge=w==2?2048:4096;pendingOptions=options.copy();busy=true;refresh();Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(pendingFormat);i.putExtra(Intent.EXTRA_TITLE,getString(R.string.app_name)+"_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+(w==1?".png":".jpg"));startActivityForResult(i,SAVE);
 }).show();}
 private void export(Uri uri){
  if(pendingOptions==null||selected==null||model==null){busy=false;refresh();error("导出已取消","作品状态发生变化，请重新导出。");return;}
  final Uri original=selected;final CameraCatalog.Model chosen=model;final PhotoMetadata meta=metadata;final PosterRenderer.Options opts=pendingOptions;final int edge=exportEdge;final String format=pendingFormat;status.setText("正在生成高清作品…");
  worker.execute(()->{Bitmap full=null,out=null;try{full=decode(original,edge);out=new PosterRenderer().render(full,body,chosen,meta,opts,edge);try(OutputStream stream=getContentResolver().openOutputStream(uri,"w")){if(stream==null||!out.compress(format.equals("image/png")?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.JPEG,95,stream))throw new IOException("无法写入文件");}
    final String dimensions=out.getWidth()+" × "+out.getHeight();main.post(()->{if(isDestroyed())return;busy=false;pendingOptions=null;status.setText("已导出 · "+dimensions);refresh();Toast.makeText(this,"作品已保存",Toast.LENGTH_LONG).show();});
   }catch(Exception|OutOfMemoryError e){main.post(()->{if(isDestroyed())return;busy=false;pendingOptions=null;status.setText("导出失败，请重试或选择 2048 px");refresh();error("无法导出",e.getMessage());});}finally{if(full!=null)full.recycle();if(out!=null)out.recycle();}
  });
 }
 private void error(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("知道了",null).show();}
 private void showCatalog(){
  catalogIndex=new CatalogIndex(catalog.library);catalogBrand=null;catalogSeries=null;catalogList=new ListView(this);catalogList.setDividerHeight(0);
  LinearLayout catalogPanel=new LinearLayout(this);catalogPanel.setOrientation(LinearLayout.VERTICAL);catalogPanel.setPadding(dp(12),0,dp(12),0);catalogPanel.setFocusableInTouchMode(true);
  int genericCount=0;for(CameraCatalog.Model item:catalog.models)if(item.genericArtwork)genericCount++;TextView inventory=new TextView(this);String version="";try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}inventory.setText(getString(R.string.app_name)+" "+version+" · "+(catalog.models.size()-genericCount)+" 款机模 · "+genericCount+" 种通用扫描示意");inventory.setTextSize(12);inventory.setPadding(dp(4),dp(8),0,dp(4));catalogPanel.addView(inventory);
  catalogSearch=new EditText(this);catalogSearch.setSingleLine(true);catalogSearch.setTextSize(15);catalogSearch.setHint("搜索品牌或型号，如 907X、M240");catalogSearch.setContentDescription("搜索机模库");catalogPanel.addView(catalogSearch,new LinearLayout.LayoutParams(-1,dp(48)));catalogPanel.addView(catalogList,new LinearLayout.LayoutParams(-1,dp(360)));catalogPanel.requestFocus();
  catalogSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){if(catalogDialog!=null&&catalogDialog.isShowing())updateCatalog();}public void afterTextChanged(Editable e){}});
  catalogDialog=new AlertDialog.Builder(this).setTitle("机模库 · 品牌").setView(catalogPanel).setNegativeButton("关闭",null).setNeutralButton("返回",null).create();
  catalogDialog.setOnShowListener(d->{catalogDialog.getButton(-3).setOnClickListener(v->catalogBack());updateCatalog();});
  catalogDialog.setOnKeyListener((d,key,e)->{if(key==KeyEvent.KEYCODE_BACK&&e.getAction()==KeyEvent.ACTION_UP){catalogBack();return true;}return key==KeyEvent.KEYCODE_BACK;});catalogDialog.show();
 }
 private void catalogBack(){if(catalogSearch!=null&&!catalogSearch.getText().toString().isEmpty()){catalogSearch.setText("");return;}if(catalogSeries!=null)catalogSeries=null;else if(catalogBrand!=null)catalogBrand=null;else{catalogDialog.dismiss();return;}updateCatalog();}
 private void updateCatalog(){
  ArrayList<String> keys=new ArrayList<>(),rows=new ArrayList<>();
  boolean searching=catalogSearch!=null&&!catalogSearch.getText().toString().trim().isEmpty();
  if(searching){List<CameraCatalog.Model> found=catalogIndex.search(catalogSearch.getText().toString());for(CameraCatalog.Model item:found)rows.add(item.name+"\n"+item.series+" · "+(item.available?(item.genericArtwork?"通用示意 · 未确定硬件型号":"已适配 · EXIF 自动匹配"):item.status)+(item.qualityNote.isEmpty()?"":"\n"+item.qualityNote));if(rows.isEmpty())rows.add("未找到该型号 · 请检查型号或品牌名称");catalogDialog.setTitle("机模库 · 搜索结果 "+found.size());}
  else if(catalogBrand==null){for(String brand:catalogIndex.brands.keySet()){keys.add(brand);int count=0,pending=0,generic=0;for(List<CameraCatalog.Model> group:catalogIndex.brands.get(brand).values())for(CameraCatalog.Model m:group){if(m.genericArtwork)generic++;else if(m.available)count++;else pending++;}rows.add(brand+"   ·   "+count+" 已适配"+(generic>0?" / "+generic+" 通用示意":"")+(pending>0?" / "+pending+" 待适配":"")+"  ›");}catalogDialog.setTitle("机模库 · 品牌");}
  else if(catalogSeries==null){for(String series:catalogIndex.brands.get(catalogBrand).keySet()){keys.add(series);rows.add(series+"   ·   "+catalogIndex.brands.get(catalogBrand).get(series).size()+" 款  ›");}catalogDialog.setTitle(catalogBrand+" · 系列");}
  else{for(CameraCatalog.Model item:catalogIndex.brands.get(catalogBrand).get(catalogSeries)){rows.add(item.name+(item==model?"  · 当前照片":"")+"\n"+(item.available?(item.genericArtwork?"通用示意 · 未确定硬件型号":"已适配 · 根据照片 EXIF 自动匹配")+(item.qualityNote.isEmpty()?"":"\n"+item.qualityNote):item.status));}catalogDialog.setTitle(catalogBrand+" / "+catalogSeries);}
  catalogList.setAdapter(textAdapter(rows.toArray(new String[0]),true));catalogList.setOnItemClickListener((parent,view,position,id)->{if(searching)return;if(catalogBrand==null)catalogBrand=keys.get(position);else if(catalogSeries==null)catalogSeries=keys.get(position);else return;updateCatalog();});catalogDialog.getButton(-3).setVisibility(catalogBrand==null&&!searching?View.GONE:View.VISIBLE);catalogList.setSelection(0);
 }
 @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putBoolean("cameraOnly",options.cameraOnly);b.putBoolean("editorOpen",stage.isOpen());b.putInt("activeSection",activeSection);if(selected!=null)b.putString("uri",selected.toString());b.putBoolean("vertical",options.vertical);b.putBoolean("reverse",options.reverse);b.putBoolean("swapText",options.swapText);b.putBoolean("flipPortraitBody",options.flipPortraitBody);b.putBoolean("blur",options.blur);b.putInt("blurType",options.blurType);b.putFloat("blurStrength",options.blurStrength);b.putFloat("blurDirection",options.blurDirection);b.putString("camera",options.camera);b.putString("lens",options.lens);b.putBoolean("showSignature",options.showSignature);b.putBoolean("showParameters",options.showParameters);b.putBoolean("fadeScreen",options.fadeScreen);b.putInt("signatureFont",options.signatureFont);b.putInt("parameterFont",options.parameterFont);b.putFloat("signatureSize",options.signatureSize);b.putFloat("parameterSize",options.parameterSize);b.putFloat("bodyScale",options.bodyScale);b.putFloat("fadeAmount",options.fadeAmount);b.putBoolean("clearBodySize",options.clearBodySize);b.putBoolean("tintScreen",options.tintScreen);b.putBoolean("reflectScreen",options.reflectScreen);b.putBoolean("reverseReflection",options.reverseReflection);b.putInt("screenPreset",options.screenPreset);b.putFloat("screenPresetStrength",options.screenPresetStrength);b.putInt("tintPreset",options.tintPreset);b.putFloat("tintStrength",options.tintStrength);b.putFloat("reflectionStrength",options.reflectionStrength);}
 @Override protected void onDestroy(){if(zoomDialog!=null)zoomDialog.dismiss();if(catalogDialog!=null)catalogDialog.dismiss();if(importSourceDialog!=null)importSourceDialog.dismiss();if(preview!=null)preview.renderer.clearCache();super.onDestroy();worker.shutdown();/* queued work may still own bitmaps; let GC reclaim after it completes */}
 private final class Preview extends View implements EditorStage.PreviewGeometry{
  final PosterRenderer renderer=new PosterRenderer();final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  Bitmap rendered;
  void changed(){if(rendered!=null){rendered.recycle();rendered=null;}invalidate();}
  Preview(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_HARDWARE,null);setOnClickListener(v->{if(photo==null)pick();});}
  public RectF protectedBounds(int width,int height){if(photo==null)return new RectF(0,0,width,height*.4f);float[] size=renderer.size(photo,options);float k=Math.min((width-dp(12))/size[0],(height-dp(12))/size[1]);RectF area=renderer.panelBounds(photo,options);area.set(area.left*k+(width-size[0]*k)/2,area.top*k+(height-size[1]*k)/2,area.right*k+(width-size[0]*k)/2,area.bottom*k+(height-size[1]*k)/2);return area;}
  RectF region(){float[] size=renderer.size(photo,options);return new RectF(0,0,size[0],size[1]);}
  @Override protected void onMeasure(int ws,int hs){setMeasuredDimension(MeasureSpec.getSize(ws),MeasureSpec.getSize(hs));}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();paint.setColor(dark?0xff26342e:0xffe6e9e0);if(photo==null)c.drawRoundRect(0,0,w,h,dp(18),dp(18),paint);
   if(photo==null){paint.setColor(0xffd4dccf);c.drawCircle(w*.8f,h*.17f,w*.27f,paint);paint.setColor(0xffc5d2c0);c.drawCircle(w*.1f,h*.97f,w*.46f,paint);
    float cx=w/2,cy=h*.44f;paint.setColor(0xff627b69);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));c.drawRoundRect(cx-dp(43),cy-dp(31),cx+dp(43),cy+dp(31),dp(8),dp(8),paint);c.drawRect(cx-dp(32),cy-dp(21),cx+dp(17),cy+dp(21),paint);c.drawCircle(cx+dp(30),cy+dp(9),dp(5),paint);paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dp(16));paint.setTypeface(Typeface.create("sans-serif-medium",0));c.drawText("每一张照片，都有来处",cx,cy+dp(67),paint);paint.setTextSize(dp(11));paint.setTypeface(Typeface.DEFAULT);c.drawText("点击这里，或导入一张原图",cx,cy+dp(94),paint);return;}
   float[] size=renderer.size(photo,options);RectF region=region();
   float k=Math.min((w-dp(12))/region.width(),(h-dp(12))/region.height());if(k<=0)return;c.save();c.translate((w-region.width()*k)/2,(h-region.height()*k)/2);c.scale(k,k);c.clipRect(0,0,region.width(),region.height());c.translate(-region.left,-region.top);if(rendered==null)rendered=renderer.render(photo,body,model,metadata,options,1600);
   c.drawBitmap(rendered,null,new RectF(0,0,size[0],size[1]),paint);c.restore();
  }
 }
}
