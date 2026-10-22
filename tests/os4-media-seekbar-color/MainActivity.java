package local.hyperceiler.seekbarcolorprobe;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import kotlin.Unit;
import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.common.*;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.MediaSeekBarColorOS4;

/** Real Android graphics and production color helper; explicit local dispatch fixture, no LSPosed injection. */
public final class MainActivity extends Activity {
 static final int W=384,H=128;
 final Handler main=new Handler(Looper.getMainLooper());
 final JSONArray checks=new JSONArray(),frames=new JSONArray();
 int failures,scenario,phase;File dir;FrameLayout card;ProbeSeekBar view;Bitmap[] shots=new Bitmap[4];
 final String[] names={"high-progress20","high-progress80","high-rtl","high-glow35","high-paint50","middle-head","middle-primary","not-owned","primary-level0","prior-filter","original-exception"};
 void check(String n,boolean b){try{checks.put(new JSONObject().put("name",n).put("passed",b));if(!b)failures++;}catch(Exception e){throw new RuntimeException(e);}}
 public void onCreate(Bundle b){super.onCreate(b);dir=new File(getFilesDir(),"results");dir.mkdirs();FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);card=new FrameLayout(this);root.addView(card,new FrameLayout.LayoutParams(W,H,Gravity.CENTER));setContentView(root);try{nextScenario();}catch(Throwable t){fatal(t);}}
 void fatal(Throwable t){android.util.Log.e("SeekbarProbe","failure",t);check("unexpected "+t,false);finishReport();}
 void nextScenario()throws Exception {
  if(scenario==names.length){finishReport();return;}
  view=new ProbeSeekBar(this,scenario==5||scenario==6?1:scenario==8?0:2);view.owner=scenario!=7;view.progress=scenario==1?.8f:.2f;view.rtl=scenario==2;view.glow=scenario==3?.35f:1f;view.mPaint.setAlpha(scenario==4?128:255);view.primary=scenario==6;view.throwOriginal=scenario==10;
  if(scenario==9)view.mPaint.setColorFilter(new PorterDuffColorFilter(Color.GREEN,PorterDuff.Mode.SRC_IN));
  card.removeAllViews();card.addView(view,new FrameLayout.LayoutParams(W,H));phase=0;capture();
 }
 void capture(){
  card.setBackgroundColor(phase%2==0?Color.BLACK:Color.WHITE);view.tint=phase>=2;view.invalidate();
  main.postDelayed(()->{
   int[]xy=new int[2];card.getLocationInWindow(xy);Bitmap b=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);
   PixelCopy.request(getWindow(),new Rect(xy[0],xy[1],xy[0]+W,xy[1]+H),b,result->{
    check(names[scenario]+" frame"+phase+" PixelCopy",result==PixelCopy.SUCCESS);shots[phase]=b;
    try{String png=names[scenario]+"-"+phase+".png";try(FileOutputStream out=new FileOutputStream(new File(dir,png))){b.compress(Bitmap.CompressFormat.PNG,100,out);}frames.put(new JSONObject().put("png",png));phase++;if(phase<4)capture();else{evaluate();for(Bitmap shot:shots)shot.recycle();scenario++;nextScenario();}}catch(Throwable t){fatal(t);}
   },main);
  },120);
 }
 void evaluate(){
  String n=names[scenario];int bad=0,active=0,different=0,blueUnchanged=0;
  for(int y=0;y<H;y++)for(int x=0;x<W;x++){
   int a=shots[0].getPixel(x,y),b=shots[1].getPixel(x,y),c=shots[2].getPixel(x,y),d=shots[3].getPixel(x,y);
   if(a!=c)different++;
   if(scenario==7||scenario==8){if(a!=c||b!=d)bad++;continue;}
   if(scenario==6){if(Color.blue(a)>240&&Color.red(a)<3&&Color.green(a)<3&&a==c)blueUnchanged++;continue;}
   int alpha=255-Color.green(b)+Color.green(a);if(alpha>10)active++;
   if(Math.abs(Color.red(c)-alpha)>3||Color.green(c)>3||Color.blue(c)>3||Math.abs(Color.red(d)-255)>3||Math.abs(Color.green(d)-(255-alpha))>3||Math.abs(Color.blue(d)-(255-alpha))>3)bad++;
  }
  if(scenario==7||scenario==8)check(n+" untouched pixels",bad==0);
  else if(scenario==6){check(n+" primary blue region retained",blueUnchanged>500);check(n+" shader head tinted",different>50);}
  else{check(n+" shape alpha and color on black/white",bad==0);check(n+" nonempty shader and effective tint",active>30&&different>30);}
  check(n+" Paint filter identity restored after every draw",view.restored&&view.draws>=4);
  check(n+" original shader and paint alpha retained",view.mPaint.getShader()==view.shader&&view.mPaint.getAlpha()==(scenario==4?128:255));
  check(n+" callback errors absent",view.errors==0);
  if(scenario==10)check(n+" original throwable retained",view.exceptionRetained);
 }
 public static final class ProbeSeekBar extends SeekBar {
  public final int mDeviceLevel;public final Paint mPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
  final RuntimeShader shader;final MediaSeekBarColorOS4 helper;final IMethodHook callback;
  boolean tint,owner=true,rtl,primary,throwOriginal,restored=true,exceptionRetained=true;float progress=.2f,glow=1;int draws,errors;
  ProbeSeekBar(Context c,int level)throws Exception {
   super(c);mDeviceLevel=level;setBackground(null);setPadding(0,0,0,0);setMin(0);setMax(100);
   String file=level==1?"music_player_tracker_middle.frag":"music_player_tracker.frag";
   shader=new RuntimeShader(read(c,file));Bitmap head;try(InputStream in=c.getAssets().open("miuix_hyper_progressbar_light_head.png")){head=BitmapFactory.decodeStream(in);}
   shader.setInputShader("uTex",new BitmapShader(head,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP));shader.setFloatUniform("uHeadSize",head.getWidth(),head.getHeight());
   shader.setFloatUniform("uResolution",W,H);shader.setFloatUniform("uTrackSize",320f,16f);shader.setFloatUniform("uTrackPosition",32f,64f);mPaint.setShader(shader);
   helper=MediaSeekBarColorOS4.Companion.create(ProbeSeekBar.class);callback=helper.callback(target->tint&&owner?Integer.valueOf(Color.RED):null,e->{errors++;return Unit.INSTANCE;});
  }
  static String read(Context c,String f)throws Exception{try(InputStream in=c.getAssets().open(f);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[]buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);return out.toString("UTF-8");}}
  @Override protected void onDraw(Canvas canvas){
   if(primary){Paint base=new Paint(Paint.ANTI_ALIAS_FLAG);base.setColor(Color.BLUE);canvas.drawRoundRect(32,56,352,72,8,8,base);}
   ColorFilter previous=mPaint.getColorFilter();Shader originalShader=mPaint.getShader();int originalAlpha=mPaint.getAlpha();
   InvocationContext state=new InvocationContext(new Chain(helper.getMethod(),this,new Object[]{canvas}));HookParam param=new HookParam(state);
   callback.before(param);if(!param.isSkipped())state.proceedOriginal();state.setAfterStage(true);callback.after(param);
   restored &= mPaint.getColorFilter()==previous&&mPaint.getShader()==originalShader&&mPaint.getAlpha()==originalAlpha;draws++;
   if(throwOriginal)exceptionRetained &= state.getThrowable() instanceof IllegalStateException&&"original fixture failure".equals(state.getThrowable().getMessage());
   else if(state.getThrowable()!=null){errors++;android.util.Log.e("SeekbarProbe","draw",state.getThrowable());}
  }
  public final void drawHighEndMode(Canvas c){shader.setFloatUniform("uTrackProgress",progress);shader.setFloatUniform("uHeadGlowAlpha",glow);shader.setIntUniform("uIsRtl",rtl?1:0);c.drawRect(0,0,W,H,mPaint);if(throwOriginal)throw new IllegalStateException("original fixture failure");}
 }
 static final class Chain implements XposedInterface.Chain {
  final Method method;final Object target;final Object[]args;Chain(Method m,Object t,Object[]a){method=m;target=t;args=a;}
  public Executable getExecutable(){return method;}public Object getThisObject(){return target;}public java.util.List<Object>getArgs(){return Arrays.asList(args);}public Object getArg(int i){return args[i];}
  public Object proceed()throws Throwable{return proceedWith(target,args);}public Object proceed(Object[]a)throws Throwable{return proceedWith(target,a);}public Object proceedWith(Object t)throws Throwable{return proceedWith(t,args);}
  public Object proceedWith(Object t,Object[]a)throws Throwable{try{return method.invoke(t,a);}catch(InvocationTargetException e){throw e.getCause();}}
 }
 void finishReport(){try{JSONObject d=new JSONObject().put("passed",failures==0).put("checks",checks).put("check_count",checks.length()).put("failed_checks",failures).put("frames",frames).put("sdk",Build.VERSION.SDK_INT).put("fingerprint",Build.FINGERPRINT).put("source_sha256",BuildMetadata.SOURCE_SHA256).put("device_status","NOT_TESTED").put("scope","Production MediaSeekBarColorOS4 with actual Android37 Paint and hardware PixelCopy; real EzHook InvocationContext manually dispatched into explicit SeekBar target fixture. Original OTA high/middle AGSL and head texture executed. Tests graph coloring/shape/alpha/restore; not original whole HyperProgressSeekBar, SystemUI or physical LSPosed injection.");try(FileOutputStream out=new FileOutputStream(new File(dir,"report.json"))){out.write(d.toString(2).getBytes("UTF-8"));}}catch(Exception e){android.util.Log.e("SeekbarProbe","report",e);}}
}
