package local.hyperceiler.custombgoverlayprobe;
import android.app.Activity;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;

/** Android View compositing prerequisite; does not execute production hooks. */
public final class MainActivity extends Activity {
 private final Handler main=new Handler(Looper.getMainLooper());
 private final JSONArray checks=new JSONArray(),frames=new JSONArray();
 private int failures,phase;private ImageView original,overlay;private FrameLayout card;private File dir;
 private void check(String name,boolean ok){try{checks.put(new JSONObject().put("name",name).put("passed",ok));if(!ok)failures++;}catch(Exception e){throw new RuntimeException(e);}}
 public void onCreate(Bundle b){super.onCreate(b);dir=new File(getFilesDir(),"results");dir.mkdirs();FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);card=new FrameLayout(this);root.addView(card,new FrameLayout.LayoutParams(256,160,Gravity.CENTER));original=new ImageView(this);original.setBackgroundColor(Color.RED);original.setAlpha(.5f);card.addView(original,new FrameLayout.LayoutParams(-1,-1));setContentView(root);main.postDelayed(this::next,500);}
 private void next(){
  if(phase==1){overlay=new ImageView(this);overlay.setBackgroundColor(Color.BLUE);overlay.setAlpha(.5f);card.addView(overlay,new FrameLayout.LayoutParams(-1,-1));original.setTransitionAlpha(0);check("suppression preserves original alpha",original.getAlpha()==.5f);check("suppression changes only transition multiplier",original.getTransitionAlpha()==0);}
  if(phase==2){original.setAlpha(.25f);overlay.setAlpha(original.getAlpha());check("OEM alpha remains writable while suppressed",original.getAlpha()==.25f&&original.getTransitionAlpha()==0);}
  if(phase==3){card.removeView(overlay);original.setTransitionAlpha(1);check("cleanup restores transition multiplier without reverting new alpha",original.getAlpha()==.25f&&original.getTransitionAlpha()==1);}
  if(phase==4){original.setTransitionAlpha(.4f);float saved=original.getTransitionAlpha();original.setTransitionAlpha(0);overlay.setAlpha(original.getAlpha()*saved);card.addView(overlay,new FrameLayout.LayoutParams(-1,-1));check("independent transition factor retained",saved==.4f&&original.getAlpha()==.25f);}
  if(phase==5){card.removeView(overlay);original.setTransitionAlpha(.4f);}
  if(phase>=6){finishReport();return;}
  main.postDelayed(()->{int[] xy=new int[2];card.getLocationInWindow(xy);Bitmap b=Bitmap.createBitmap(256,160,Bitmap.Config.ARGB_8888);String[] names={"original-half","overlay-half","overlay-quarter","original-quarter-restored","overlay-product","original-product-restored"};String name=names[phase];PixelCopy.request(getWindow(),new Rect(xy[0],xy[1],xy[0]+256,xy[1]+160),b,result->{check(name+" PixelCopy",result==0);int c=b.getPixel(128,80),r=Color.red(c),blue=Color.blue(c);int[] levels={128,128,64,64,26,26};boolean wantBlue=phase==1||phase==2||phase==4;check(name+" single layer compositing",Math.abs((wantBlue?blue:r)-levels[phase])<=2&&(wantBlue?r:blue)<=2);try{String png=name+".png";try(FileOutputStream out=new FileOutputStream(new File(dir,png))){b.compress(Bitmap.CompressFormat.PNG,100,out);}frames.put(new JSONObject().put("png",png).put("red",r).put("blue",blue));}catch(Exception e){check("save "+e,false);}b.recycle();phase++;next();},main);},100);
 }
 private void finishReport(){try{JSONObject d=new JSONObject().put("passed",failures==0).put("checks",checks).put("check_count",checks.length()).put("failed_checks",failures).put("frames",frames).put("sdk",Build.VERSION.SDK_INT).put("fingerprint",Build.FINGERPRINT).put("source_sha256",BuildMetadata.SOURCE_SHA256).put("device_status","NOT_TESTED").put("scope","Actual Android API37 View alpha and transitionAlpha composition on two sibling ImageViews with hardware PixelCopy; prerequisite only, no production CustomBackgroundOS4 hooks or Xiaomi SystemUI.");try(FileOutputStream out=new FileOutputStream(new File(dir,"report.json"))){out.write(d.toString(2).getBytes("UTF-8"));}}catch(Exception e){android.util.Log.e("OverlayProbe","report",e);}}
}
