package local.hyperceiler.custombgdrawableprobe;
import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.CustomBackgroundDrawableOS4;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.MediaViewColorConfig;

public final class MainActivity extends Activity {
 private final Handler main=new Handler(Looper.getMainLooper());
 private final JSONArray checks=new JSONArray(), frames=new JSONArray();
 private int failures, phase; private ImageView image; private CustomBackgroundDrawableOS4 drawing;
 private File directory; private int[] opaque;
 private static MediaViewColorConfig colors(int color){return new MediaViewColorConfig(Color.WHITE,Color.LTGRAY,color,0xff162238);}
 private void check(String name,boolean ok){try{checks.put(new JSONObject().put("name",name).put("passed",ok));if(!ok)failures++;}catch(JSONException e){throw new RuntimeException(e);}}
 @Override public void onCreate(Bundle b){super.onCreate(b);directory=new File(getFilesDir(),"results");directory.mkdirs();FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);image=new ImageView(this);image.setBackgroundColor(Color.BLACK);root.addView(image,new FrameLayout.LayoutParams(384,224,Gravity.CENTER));setContentView(root);main.postDelayed(()->{try{softwareTests();prepare();}catch(Throwable t){check("uncaught "+t,false);finishReport();}},300);}
 private Bitmap render(Drawable d){Bitmap b=Bitmap.createBitmap(200,130,Bitmap.Config.ARGB_8888);d.draw(new Canvas(b));return b;}
 private void softwareTests(){
  for(int style=1;style<=4;style++){
   ColorDrawable art=new ColorDrawable(0xff6688aa);art.setBounds(3,5,7,9);art.setAlpha(191);
   CustomBackgroundDrawableOS4 d=new CustomBackgroundDrawableOS4(art,colors(0xff122436),style,false);d.setBounds(17,23,177,103);
   Bitmap full=render(d);check("style "+style+" shifted bounds outside clear",full.getPixel(16,50)==0&&full.getPixel(177,50)==0&&full.getPixel(90,22)==0&&full.getPixel(90,103)==0);
   check("style "+style+" entire content opaque",Color.alpha(full.getPixel(18,24))==255&&Color.alpha(full.getPixel(176,102))==255);
   d.setAlpha(128);Bitmap half=render(d);check("style "+style+" layer alpha includes fill and gradient",Color.alpha(half.getPixel(18,24))==128&&Color.alpha(half.getPixel(100,50))==128&&Color.alpha(half.getPixel(176,102))==128);
   d.setAlpha(0);Bitmap zero=render(d);boolean empty=true;int[] pixels=new int[200*130];zero.getPixels(pixels,0,200,0,0,200,130);for(int c:pixels)empty&=c==0;check("style "+style+" zero alpha clears whole result",empty);
   check("style "+style+" borrowed artwork bounds alpha preserved",art.getBounds().equals(new Rect(3,5,7,9))&&art.getAlpha()==191);
   d.setAlpha(255);d.setColorFilter(new PorterDuffColorFilter(Color.GREEN,PorterDuff.Mode.SRC_IN));Bitmap filter=render(d);int c=filter.getPixel(100,50);check("style "+style+" filter applies complete composition",Color.red(c)==0&&Color.green(c)==255&&Color.blue(c)==0);
   d.setColorFilter(null);d.update(new ColorDrawable(Color.BLUE),colors(Color.BLACK),false);Bitmap update=render(d);check("style "+style+" nonanimated update renders new artwork",Color.blue(update.getPixel(170,60))>Color.red(update.getPixel(170,60)));
   final int[] invalidations={0};d.setCallback(new Drawable.Callback(){public void invalidateDrawable(Drawable ignored){invalidations[0]++;}public void scheduleDrawable(Drawable a,Runnable r,long when){}public void unscheduleDrawable(Drawable a,Runnable r){}});
   d.setActive(false);int before=invalidations[0];render(d).recycle();render(d).recycle();check("style "+style+" inactive draw no frame loop",invalidations[0]==before);
   d.dispose();before=invalidations[0];d.update(new ColorDrawable(Color.RED),colors(Color.RED),false);Bitmap gone=render(d);check("style "+style+" disposal is terminal and blank",gone.getPixel(100,50)==0&&invalidations[0]==before&&d.getCallback()==null);
   full.recycle();half.recycle();zero.recycle();filter.recycle();update.recycle();gone.recycle();
  }
  boolean rejected=false;try{new CustomBackgroundDrawableOS4(new ColorDrawable(Color.RED),colors(Color.BLACK),5,false);}catch(IllegalArgumentException e){rejected=true;}check("invalid style rejected",rejected);
 }
 private void prepare(){
  if(phase>=8){animationTest();return;}
  int style=phase/2+1;
  if(phase%2==0){if(drawing!=null)drawing.dispose();drawing=new CustomBackgroundDrawableOS4(new ColorDrawable(0xff6688aa),colors(0xff243648),style,false);image.setImageDrawable(drawing);}else drawing.setAlpha(128);
  main.postDelayed(()->capture("style-"+style+(phase%2==0?"-opaque":"-half"),pixels->{
   if(phase%2==0){opaque=pixels;long sum=0;for(int c:pixels)sum+=Color.red(c)+Color.green(c)+Color.blue(c);check("style "+style+" hardware surface visible",sum>100000);}
   else{long a=0,b=0;for(int i=0;i<pixels.length;i++){a+=Color.red(opaque[i])+Color.green(opaque[i])+Color.blue(opaque[i]);b+=Color.red(pixels[i])+Color.green(pixels[i])+Color.blue(pixels[i]);}double ratio=(double)b/a;check("style "+style+" hardware half alpha ratio "+ratio,ratio>0.48&&ratio<0.53);}
   phase++;prepare();}),100);
 }
 private interface Pixels {void run(int[] pixels);}
 private void capture(String name,Pixels done){int[] xy=new int[2];image.getLocationInWindow(xy);Bitmap b=Bitmap.createBitmap(image.getWidth(),image.getHeight(),Bitmap.Config.ARGB_8888);PixelCopy.request(getWindow(),new Rect(xy[0],xy[1],xy[0]+b.getWidth(),xy[1]+b.getHeight()),b,result->{check(name+" PixelCopy",result==PixelCopy.SUCCESS);int[] pixels=new int[b.getWidth()*b.getHeight()];b.getPixels(pixels,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());try{String png=name+".png";try(FileOutputStream out=new FileOutputStream(new File(directory,png))){b.compress(Bitmap.CompressFormat.PNG,100,out);}frames.put(new JSONObject().put("png",png).put("width",b.getWidth()).put("height",b.getHeight()));}catch(Exception e){check("save "+e,false);}b.recycle();done.run(pixels);},main);}
 private void animationTest(){
  drawing.dispose();drawing=new CustomBackgroundDrawableOS4(new ColorDrawable(Color.RED),colors(Color.BLACK),1,true);image.setImageDrawable(drawing);
  main.postDelayed(()->{drawing.update(new ColorDrawable(Color.BLUE),colors(Color.BLACK),false);main.postDelayed(()->capture("transition-middle",pixels->{int c=pixels[pixels.length/2];check("transition has old and new pixels",Color.red(c)>10&&Color.blue(c)>10);main.postDelayed(()->capture("transition-finished",last->{int end=last[last.length/2];check("transition reaches new result",Color.red(end)<3&&Color.blue(end)>250);drawing.update(new ColorDrawable(Color.RED),colors(Color.BLACK),false);drawing.setActive(false);main.postDelayed(()->capture("transition-paused-snap",paused->{int snap=paused[paused.length/2];check("inactive snaps to latest art",Color.red(snap)>250&&Color.blue(snap)<3);drawing.setVisible(false,false);drawing.update(new ColorDrawable(Color.GREEN),colors(Color.BLACK),false);drawing.setVisible(true,false);main.postDelayed(()->capture("hidden-update-visible",shown->{int v=shown[shown.length/2];check("hidden update no stale transition on show",Color.green(v)>250&&Color.red(v)<3);finishReport();}),80);}),80);}),360);}),125);},80);
 }
 private void finishReport(){if(drawing!=null)drawing.dispose();try{JSONObject report=new JSONObject().put("passed",failures==0).put("checks",checks).put("check_count",checks.length()).put("failed_checks",failures).put("frames",frames).put("source_sha256",BuildMetadata.SOURCE_SHA256).put("sdk",Build.VERSION.SDK_INT).put("fingerprint",Build.FINGERPRINT).put("device_status","NOT_TESTED").put("scope","Complete production OS4 custom drawable plus color config on Android API37; software Canvas and hardware PixelCopy. No SystemUI/LSPosed injection.");try(FileOutputStream out=new FileOutputStream(new File(directory,"report.json"))){out.write(report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}android.util.Log.i("CustomBgProbe","complete passed="+(failures==0)+" count="+checks.length());}catch(Exception e){android.util.Log.e("CustomBgProbe","report failed",e);}}
}
