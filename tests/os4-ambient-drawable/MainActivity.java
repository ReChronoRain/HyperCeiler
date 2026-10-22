package local.hyperceiler.ambientprobe;

import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.drawable.AmbientLightDrawable;

public final class MainActivity extends Activity {
 private final Handler main = new Handler(Looper.getMainLooper());
 private final JSONArray checks = new JSONArray(), frames = new JSONArray();
 private int failures, phase; private ImageView image; private AmbientLightDrawable drawing;
 private final File[] directory = new File[1]; private int fullMaximum; private int[] fullPixels;
 private interface Action { void run() throws Exception; }
 private void check(String name, boolean pass) { try { checks.put(new JSONObject().put("name",name).put("passed",pass)); if(!pass) failures++; } catch(JSONException e){throw new RuntimeException(e);} }
 private Object field(Object d,String name) throws Exception { Field f=d.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(d); }
 private void test(String name, Action action) { try { action.run(); } catch(Throwable e) { check(name+" threw "+e.getClass().getSimpleName()+": "+e.getMessage(),false); } }
 @Override public void onCreate(Bundle b) {
  super.onCreate(b); directory[0]=new File(getFilesDir(),BuildMetadata.VARIANT);directory[0].mkdirs();
  FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);image=new ImageView(this);image.setBackgroundColor(Color.BLACK);root.addView(image,new FrameLayout.LayoutParams(512,512,Gravity.CENTER));setContentView(root);
  main.postDelayed(()->{ stateTests(); drawing=new AmbientLightDrawable(true);drawing.setLightMode(false);image.setImageDrawable(drawing);drawing.start();drawing.setGradientColor(Color.RED,true); main.postDelayed(this::nextFrame,850); },300);
 }
 private void stateTests(){
  test("shader readiness",()->{AmbientLightDrawable d=new AmbientLightDrawable(true);check("real Android RuntimeShader compiled",field(d,"runtimeShader")!=null);boolean ready=false;try{ready=(Boolean)d.getClass().getMethod("isReady").invoke(d);}catch(NoSuchMethodException missing){}check("public readiness reflects shader",ready);});
  test("snap then reset",()->{AmbientLightDrawable d=new AmbientLightDrawable(true);d.setGradientColor(Color.RED,true);check("snapped target synchronized",((Integer)field(d,"targetColor"))==Color.RED);d.setGradientColor(Color.TRANSPARENT,true);check("snap red then transparent clears",((Integer)field(d,"currentColor"))==Color.TRANSPARENT);check("clear target transparent",((Integer)field(d,"targetColor"))==Color.TRANSPARENT);});
  test("nonanimated repeat colors",()->{AmbientLightDrawable d=new AmbientLightDrawable(false);int[] colors={Color.RED,Color.BLUE,Color.RED,Color.TRANSPARENT,0x80ff0000,Color.TRANSPARENT};for(int i=0;i<colors.length;i++){d.setGradientColor(colors[i],false);check("nonanimated color "+i,((Integer)field(d,"currentColor"))==colors[i]&&((Integer)field(d,"targetColor"))==colors[i]);}});
  test("stop clears pending animations",()->{AmbientLightDrawable d=new AmbientLightDrawable(true);image.setImageDrawable(d);d.setBounds(0,0,512,512);d.start();d.setGradientColor(Color.RED,false);d.animateNextResize();d.setBounds(0,0,512,420);d.stop();check("stop color animation",field(d,"colorState").toString().equals("DONE"));check("stop resize animation",field(d,"resizeState").toString().equals("DONE"));check("stop freezes target color",field(d,"targetColor").equals(field(d,"currentColor")));check("stop freezes target height",field(d,"targetHeight").equals(field(d,"currentHeight")));check("stop pauses time",(Boolean)field(d,"pause"));});
  test("restart clock",()->{AmbientLightDrawable d=new AmbientLightDrawable(true);d.start();d.pause();SystemClock.sleep(20);d.start();check("start resets accumulated pause",((Long)field(d,"pauseDuration"))==0L);check("start resets old pause timestamp",((Long)field(d,"pauseTime"))==0L);check("start resumes",!(Boolean)field(d,"pause"));});
  test("stopped clock stable",()->{AmbientLightDrawable d=new AmbientLightDrawable(true);d.start();d.pause();SystemClock.sleep(10);d.resume();long accumulated=(Long)field(d,"pauseDuration");d.stop();check("stop preserves elapsed pause history",((Long)field(d,"pauseDuration"))==accumulated);});
  test("paused mode redraw",()->{AmbientLightDrawable d=new AmbientLightDrawable(true);int[] invalidates={0};d.setCallback(new Drawable.Callback(){public void invalidateDrawable(Drawable ignored){invalidates[0]++;}public void scheduleDrawable(Drawable a,Runnable r,long t){}public void unscheduleDrawable(Drawable a,Runnable r){}});d.pause();int before=invalidates[0];d.setLightMode(true);check("light mode invalidates paused drawable",invalidates[0]>before);int changed=invalidates[0];d.setLightMode(true);check("same light mode does not invalidate",invalidates[0]==changed);});
 }
 private void nextFrame(){
  String[] names={"opaque-red","half-alpha-red","transparent-after-red","opaque-blue-after-clear","light-mode-paused"};
  if(phase>=names.length){finishReport();return;}
  if(phase==0)drawing.pause();
  String name=names[phase]; int[] xy=new int[2];image.getLocationInWindow(xy);Rect rect=new Rect(xy[0],xy[1],xy[0]+image.getWidth(),xy[1]+image.getHeight());Bitmap b=Bitmap.createBitmap(rect.width(),rect.height(),Bitmap.Config.ARGB_8888);
  PixelCopy.request(getWindow(),rect,b,result->{
   check(name+" actual hardware PixelCopy",result==PixelCopy.SUCCESS);
   if(result==PixelCopy.SUCCESS){
    int[] pixels=new int[b.getWidth()*b.getHeight()];b.getPixels(pixels,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());int max=0,nonblack=0;long sum=0;
    for(int c:pixels){int value=Math.max(Color.red(c),Math.max(Color.green(c),Color.blue(c)));max=Math.max(max,value);if(value>2)nonblack++;sum+=value;}
    try{String png=name+".png";try(FileOutputStream out=new FileOutputStream(new File(directory[0],png))){b.compress(Bitmap.CompressFormat.PNG,100,out);}frames.put(new JSONObject().put("name",name).put("png",png).put("maximum",max).put("nonblack_pixels",nonblack).put("sum_max_rgb",sum).put("width",b.getWidth()).put("height",b.getHeight()));}catch(Exception e){check(name+" save "+e,false);}
    if(phase==0){fullPixels=pixels;fullMaximum=max;check("opaque color reaches rendered surface",nonblack>500&&max>20);}
    if(phase==1){long before=0,after=0;int samples=0;for(int i=0;i<pixels.length;i++){int c=fullPixels[i];int v=Math.max(Color.red(c),Math.max(Color.green(c),Color.blue(c)));if(v>=20&&v<=150){int q=pixels[i];before+=v;after+=Math.max(Color.red(q),Math.max(Color.green(q),Color.blue(q)));samples++;}}double ratio=before==0?Double.NaN:(double)after/before;try{frames.getJSONObject(frames.length()-1).put("unsaturated_sample_count",samples).put("unsaturated_alpha_ratio",Double.isFinite(ratio)?ratio:-1);}catch(JSONException e){throw new RuntimeException(e);}check("half alpha scales unsaturated actual pixels",samples>500&&ratio>=0.45&&ratio<=0.55);}
    if(phase==2)check("transparent produces black background",max<=2&&nonblack==0);
    if(phase==3)check("color after clear renders",nonblack>500&&max>20);
    if(phase==4)check("light mode renders while paused",nonblack>500&&max>20);
   }
   b.recycle();phase++;
   if(phase==1)drawing.setGradientColor(0x80ff0000,true);
   if(phase==2)drawing.setGradientColor(Color.TRANSPARENT,true);
   if(phase==3)drawing.setGradientColor(Color.BLUE,true);
   if(phase==4)drawing.setLightMode(true);
   main.postDelayed(this::nextFrame,180);
  },main);
 }
 private void finishReport(){
  drawing.stop();try{JSONObject report=new JSONObject().put("passed",failures==0).put("checks",checks).put("check_count",checks.length()).put("failed_checks",failures).put("frames",frames).put("variant",BuildMetadata.VARIANT).put("source_sha256",BuildMetadata.SOURCE_SHA256).put("math_source_sha256",BuildMetadata.MATH_SHA256).put("sdk",Build.VERSION.SDK_INT).put("fingerprint",Build.FINGERPRINT).put("model",Build.MODEL).put("xposed_injection","NOT_TESTED").put("xiaomi_device_status","NOT_TESTED").put("scope","Full production drawable and MathUtils on real Android API37 ART/RuntimeShader/PixelCopy in Google x86_64 SwiftShader emulator; no Xiaomi SystemUI or hook runtime.");try(FileOutputStream out=new FileOutputStream(new File(directory[0],"report.json"))){out.write(report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}android.util.Log.i("AmbientProbe","RESULT "+BuildMetadata.VARIANT+" passed="+(checks.length()-failures)+" failed="+failures);}catch(Exception e){android.util.Log.e("AmbientProbe","report failure",e);}
 }
}
