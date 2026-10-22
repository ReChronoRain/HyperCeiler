import java.util.*;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.FlowCollector;
// Explicit transcriptions of the ROM merge/UI consumer, not original DEX execution.
public final class Targets {
 public static boolean itemDismissed;public static int globalZen=1,globalWrites,notificationDismissals;
 public static final Object ZEN=new Object();
 public static class Media { }
 public static final class Notification {public final String name;public final boolean summary,hidden,focus;public Notification(String n,boolean s,boolean h,boolean f){name=n;summary=s;hidden=h;focus=f;}}
 public static class Store {
  public boolean mCanShowOnLockscreen=true,mZenModeOn,mIsUpdateByMedia;public int calls;public Object[] received;public RuntimeException error;
  public List<Object> merge(boolean enabled,Media media,List<Notification> notifications){calls++;received=new Object[]{enabled,media,notifications};if(error!=null)throw error;List<Object> out=new ArrayList<>();
   if(media!=null&&mCanShowOnLockscreen)out.add(media);
   if(enabled)enabled=!itemDismissed;else itemDismissed=false;mZenModeOn=enabled;if(enabled)out.add(ZEN);
   if(mCanShowOnLockscreen)for(Notification n:notifications){if(n.summary||n.hidden)continue;out.add(n);if(out.size()>=4)break;}return out;
  }
 }
 public static final class SubStore extends Store { }
 public static class View {
  public boolean isZenModeEnabled,isNotificationCountViewVisible=true,zenVisible,dividerVisible,countVisible;public int count,requests,textUpdates,otherCalls;public Object lastValue;public final List<Runnable> queue=new ArrayList<>();
  void zen(boolean v){isZenModeEnabled=v;textUpdates++;requests++;queue.add(()->{zenVisible=v;dividerVisible=v&&isNotificationCountViewVisible;});}
  void countVisible(boolean v){isNotificationCountViewVisible=v;requests++;queue.add(()->{countVisible=v;dividerVisible=v&&isZenModeEnabled;});}
  public void drain(){for(Runnable r:List.copyOf(queue))r.run();queue.clear();}
 }
 public static class Consumer implements FlowCollector<Object> {
  public final int $r8$classId;public View $view;public int calls;public Object received,continuation;public Throwable error;public Object result=Unit.INSTANCE;
  public Consumer(int id,View v){$r8$classId=id;$view=v;}
  public Object emit(Object value,Continuation<? super Unit> c){calls++;received=value;continuation=c;if(error!=null)return sneaky(error);switch($r8$classId){case 0:$view.zen((Boolean)value);break;case 3:$view.countVisible((Boolean)value);break;case 4:$view.count=((Number)value).intValue();break;default:$view.otherCalls++;$view.lastValue=value;}return result;}
 }
 public static final class SubConsumer extends Consumer {public SubConsumer(View v){super(0,v);}}
 public static class StaticStore { public static List<Object> merge(boolean v,Media m,List<Notification> n){return List.of();}}
 public static class BadStore { public Object merge(boolean v,Media m,List<Notification> n){return n;}}
 public static class WrongId implements FlowCollector<Object>{public boolean $r8$classId;public View $view;public Object emit(Object o,Continuation<? super Unit> c){return Unit.INSTANCE;}}
 public static class StaticId implements FlowCollector<Object>{public static int $r8$classId;public View $view;public Object emit(Object o,Continuation<? super Unit> c){return Unit.INSTANCE;}}
 public static class WrongView implements FlowCollector<Object>{public int $r8$classId;public Object $view;public Object emit(Object o,Continuation<? super Unit> c){return Unit.INSTANCE;}}
 public static class MissingInterface {public int $r8$classId;public View $view;public Object emit(Object o,Continuation<?> c){return Unit.INSTANCE;}}
 @SuppressWarnings("unchecked")static <T,E extends Throwable>T sneaky(Throwable e)throws E{throw(E)e;}
}
