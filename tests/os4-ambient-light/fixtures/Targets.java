import android.content.Context;import android.view.*;import android.widget.ImageView;import androidx.constraintlayout.widget.ConstraintLayout;import kotlin.Pair;import kotlin.Unit;import kotlin.coroutines.Continuation;
public final class Targets {
 public static class Data {public final Boolean isPlaying;public final int color;public Data(int n,Boolean p){color=n;isPlaying=p;}}
 public static class Aod {public static volatile boolean mEnableFullAod;}
 public static class HN {public ImageView mediaBg;public final ConstraintLayout player=new ConstraintLayout(new Context());public HN(){mediaBg=new ImageView(player.context);mediaBg.setId(709);mediaBg.setLayoutParams(new ConstraintLayout.LayoutParams(0,0));player.addView(mediaBg,0);}}
 public static class HI {public ImageView mediaBgView;public final ConstraintLayout player=new ConstraintLayout(new Context());public HI(){mediaBgView=new ImageView(player.context);mediaBgView.setId(710);mediaBgView.setLayoutParams(new ConstraintLayout.LayoutParams(0,0));player.addView(mediaBgView,0);}}
 public static Runnable insideAttach,insideDetach,insideBind;public static boolean failAttach,failDetach,failBind;
 public static class NC {public final Context context=new Context();public HN holder;public Data mediaData;public int attaches,detaches,binds,foregrounds,aods;
 public void attach(HN h){attaches++;holder=h;if(insideAttach!=null)insideAttach.run();if(failAttach)throw new IllegalArgumentException("original attach");}
 public void detach(){detaches++;holder=null;mediaData=null;if(insideDetach!=null)insideDetach.run();if(failDetach)throw new IllegalArgumentException("original detach");}
 public void bindMediaData(Data d){binds++;if(d==null||holder==null)return;mediaData=d;if(insideBind!=null)insideBind.run();if(failBind)throw new IllegalArgumentException("original bind");}
 public void updateForegroundColors(){foregrounds++;}public void onFullAodStateChanged(boolean aod){aods++;if(holder!=null){holder.mediaBg.params.width=-1;holder.mediaBg.params.height=aod?60:100;}}
 }
 public static class DI {public final Context context=new Context();public HI holder,dummyHolder;public Data mediaData;public float mediaBgTransYOffset;public int attaches,detaches,binds;
 public void attach(HI h,HI d){attaches++;holder=h;dummyHolder=d;if(insideAttach!=null)insideAttach.run();if(failAttach)throw new IllegalArgumentException("original attach");}
 public void detach(){detaches++;holder=null;dummyHolder=null;mediaData=null;if(insideDetach!=null)insideDetach.run();if(failDetach)throw new IllegalArgumentException("original detach");}
 public void bindMediaData(Data d){binds++;if(holder==null)return;mediaData=d;if(insideBind!=null)insideBind.run();if(failBind)throw new IllegalArgumentException("original bind");}
 }
 public static class Collector {public final DI this$0;public final HI $holder,$dummyHolder;public int emits;public Collector(DI o){this$0=o;$holder=o.holder;$dummyHolder=o.dummyHolder;}
 public Object emit(Object value,Continuation<?> continuation){emits++;Pair<?,?>p=(Pair<?,?>)value;String name=(String)p.getFirst();if(name.equals("pull_down_type_update")){this$0.mediaBgTransYOffset=((Number)p.getSecond()).floatValue();if($dummyHolder!=null)$dummyHolder.mediaBgView.translationY=this$0.mediaBgTransYOffset;}else if(name.equals("pull_down_type_finish"))this$0.mediaBgTransYOffset=0;return Unit.INSTANCE;}}
 public static class SubNC extends NC {}
 public static class BadNC {public static void attach(HN h){}public void detach(){}}
}
