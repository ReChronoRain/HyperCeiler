import android.widget.TextView;
import java.util.*;
public final class Targets {
 public static float scale=1;public static boolean enqueue;
 public static TextView view(float size){var v=new TextView();v.size=size;v.scaledDensity=scale;v.density=1;return v;}
 public static class NC {
  public HN mediaViewHolder=new HN();public int layouts,heightUpdates;public boolean fail;
  public void updateLayout$1(){layouts++;if(fail)throw new IllegalArgumentException("original layout");if(mediaViewHolder!=null){if(mediaViewHolder.titleText!=null)mediaViewHolder.titleText.size=18*scale;if(mediaViewHolder.artistText!=null)mediaViewHolder.artistText.size=12*scale;}}
  public static class configurationCallback$1 {public NC this$0;public configurationCallback$1(NC c){this$0=c;}public void onDensityOrFontScaleChanged(){this$0.heightUpdates++;}}
 }
 public static class HN {public TextView titleText=view(18),artistText=view(12),elapsedTimeView=view(12),totalTimeView=view(12);}
 public static class HI {public TextView titleText=view(18),artistText=view(12),elapsedTimeView=view(12),totalTimeView=view(12);}
 public static class DI {
  public HI miuiPlayerHolder,miuiDummyPlayerHolder;public int resets;public boolean fail;public final List<Runnable>queue=new ArrayList<>();
  public void resetPlayerHolder(){resets++;if(fail)throw new IllegalArgumentException("original island");miuiPlayerHolder=new HI();miuiDummyPlayerHolder=new HI();}
  public void resetDummyOnly(){miuiDummyPlayerHolder=new HI();}
  public static class configurationCallback$1 {public DI this$0;public configurationCallback$1(DI c){this$0=c;}public void onDensityOrFontScaleChanged(){if(enqueue)this$0.queue.add(()->{try{TestMain.dispatch(this$0,"resetPlayerHolder");}catch(Throwable t){throw new RuntimeException(t);}});}}
 }
 public static class SubNC extends NC {}
 public static class BadVoid {public HN mediaViewHolder;public int updateLayout$1(){return 1;}}
 public static class StaticVoid {public HN mediaViewHolder;public static void updateLayout$1(){}}
 public static class BadField {public String mediaViewHolder;public void updateLayout$1(){}}
 public static class StaticField {public static HN mediaViewHolder;public void updateLayout$1(){}}
 public static class BadHolder {public TextView titleText,artistText,elapsedTimeView;public String totalTimeView;}
 public static class StaticHolder {public static TextView titleText;public TextView artistText,elapsedTimeView,totalTimeView;}
 public static class WrongCallback {public String this$0;public void onDensityOrFontScaleChanged(){}}
 public static class StaticCallback {public static NC this$0;public void onDensityOrFontScaleChanged(){}}
 public static class ThrowText extends TextView {public void setTextSize(int u,float s){throw new IllegalStateException("view failure");}}
}
