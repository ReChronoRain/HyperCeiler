import android.app.TaskInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import com.android.wm.shell.recents.IRecentTasks;
import com.android.wm.shell.recents.IRecentTasks.Group;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.*;
import com.sevtinge.hyperceiler.libhook.utils.api.*;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
import java.util.*;

public class Test {
 static int checks;static void ok(boolean v,String name){checks++;if(!v)throw new AssertionError(name);}
 public static class VendorRenamedService extends IRecentTasks.Stub {
  public Group[] tasks;public RuntimeException failure;public Runnable during;public int originals;
  public Group[] getRecentTasks(int max,int flags,int user){originals++;if(failure!=null)throw failure;if(during!=null){var r=during;during=null;r.run();}return tasks;}
  public void registerRecentTasksListener(IRecentTasks.Listener l){}
  public void unregisterRecentTasksListener(IRecentTasks.Listener l){}
  public void invalidate(){}
 }
 public static class Receiver implements IRecentTasks.Listener {int changed;public void onRecentTasksChanged(){changed++;}}
 public static class LegacyGroup {TaskInfo a,b;LegacyGroup(TaskInfo a,TaskInfo b){this.a=a;this.b=b;}public TaskInfo getTaskInfo1(){return a;}public TaskInfo getTaskInfo2(){return b;}}
 static final Class<?>[] QUERY={int.class,int.class,int.class};
 static TaskInfo task(String name){var t=new TaskInfo();t.realActivity=name==null?null:new ComponentName(name);return t;}
 static Object query(VendorRenamedService service)throws Throwable{return BaseHook.invoke(service,"getRecentTasks",QUERY,99,63,0);}
 static void choose(Set<String> packages){var r=ContextUtils.app.getContentResolver();r.values.put("prefs_key_home_recent_hide_card",packages);r.change();ThreadPoolManager.drain();}
 static void forwarding()throws Throwable {
  var service=new VendorRenamedService();service.tasks=new Group[0];
  var method=VendorRenamedService.class.getMethod("getRecentTasks",QUERY);
  Object[] args={1000,63,0};int[] originals={0},replacements={0},after={0};
  var raw=new BaseHook.Invocation(service,method,args){
   public Object proceed()throws Throwable{originals[0]++;return super.proceed();}
   public Object proceed(Object[] v){replacements[0]++;return v;}
   public Object proceedWith(Object target,Object[] v){replacements[0]++;return v;}
  };
  var observer=com.sevtinge.hyperceiler.libhook.base.OriginalArgumentsHook.of(new io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook(){
   public void after(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){after[0]++;}
  });
  ok(observer.intercept(raw)==service.tasks&&originals[0]==1&&replacements[0]==0&&after[0]==1,"actual EzHookTool forwards boxed primitives through original proceed once");
  var modified=com.sevtinge.hyperceiler.libhook.base.OriginalArgumentsHook.of(new io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook(){
   public void before(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){p.getArgs()[0]=Integer.valueOf(2000);}
  });
  ok(((Object[])modified.intercept(raw))[0].equals(2000)&&replacements[0]==1&&originals[0]==1,"explicit argument replacement retains framework validation path");
  var skipped=com.sevtinge.hyperceiler.libhook.base.OriginalArgumentsHook.of(new io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook(){
   public void before(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){p.setResult("skip");}
  });
  ok(skipped.intercept(raw).equals("skip")&&originals[0]==1,"early result still skips original");
  service.failure=new IllegalStateException("original");
  try{observer.intercept(raw);ok(false,"original failure");}catch(IllegalStateException e){ok(e==service.failure&&originals[0]==2&&after[0]==2,"original exception and after stage retained once");}
 }
 public static void main(String[] args)throws Throwable {
  forwarding();
  var plan=new HomeRecentCardsFilter.Plan(Group[].class,TaskInfo.class);var a=task("app.keep");var b=task("app.hide");
  Group visible=new Group(a),hidden=new Group(b),split=new Group(a,b),unknown=new Group(task(null));Group[] original={visible,hidden,split,unknown,null};
  Set<String> selected=HomeRecentCardsFilter.packages(Set.of("app.hide"));
  Group[] filtered=(Group[])plan.filter(original,selected);
  ok(filtered.length==3&&filtered[0]==visible&&filtered[1]==unknown&&filtered[2]==null,"stable order and opaque groups");
  ok(original.length==5&&split.tasks.size()==2&&split.tasks.get(1)==b,"no source array, group or task mutation");
  ok(plan.filter(original,Set.of())==original&&plan.filter(original,Set.of("app.missing"))==original,"off and no match preserve identity");
  ok(plan.filter(null,selected)==null&&((Group[])plan.filter(new Group[]{hidden,split},selected)).length==0,"null and all hidden");
  var intentTask=task(null);intentTask.baseIntent=new Intent();intentTask.baseIntent.c=new ComponentName("app.hide");
  ok(((Group[])plan.filter(new Group[]{new Group(intentTask)},selected)).length==0,"real activity absent, explicit base intent component");
  intentTask.baseIntent.c=null;intentTask.baseIntent.p="app.hide";
  ok(((Group[])plan.filter(new Group[]{new Group(intentTask)},selected)).length==0,"explicit intent package fallback");
  intentTask.realActivity=new ComponentName("app.keep");
  ok(plan.filter(new Group[]{new Group(intentTask)},selected) instanceof Group[] v&&v.length==1,"real activity wins over unrelated intent");
  var legacy=new HomeRecentCardsFilter.Plan(LegacyGroup[].class,TaskInfo.class);
  ok(((LegacyGroup[])legacy.filter(new LegacyGroup[]{new LegacyGroup(a,b)},selected)).length==0,"older paired public getter contract");
  for(String invalid:new String[]{"app|1","app/name",".app","app..name","a".repeat(256)})try{HomeRecentCardsFilter.packages(Set.of(invalid));ok(false,"invalid package");}catch(IllegalArgumentException expected){checks++;}

  var loader=new HomeHideRecentCardsOS4();loader.init();var service=new VendorRenamedService();service.tasks=original;
  BaseHook.constructed(service);BaseHook.constructed(service);
  var receiver=new Receiver();BaseHook.invoke(service,"registerRecentTasksListener",new Class<?>[]{IRecentTasks.Listener.class},receiver);ThreadPoolManager.drain();
  ok(BaseHook.hooks.size()==5,"actual concrete class hooked once, constructor repeat stays bounded");
  ok(query(service)==original,"default selection empty");
  choose(Set.of("app.hide"));ok(receiver.changed==1,"configuration update notifies actual recent listener");
  ok(((Group[])query(service)).length==3,"shipping wrapper filters selected groups");
  int reads=ContextUtils.app.resolver.reads,queries=ContextUtils.app.resolver.queries,logs=com.sevtinge.hyperceiler.common.log.ModuleLog.logs;
  for(int i=0;i<10000;i++)ok(((Group[])query(service)).length==3,"warm reply");
  ok(ContextUtils.app.resolver.reads==reads&&ContextUtils.app.resolver.queries==queries&&ThreadPoolManager.queue.isEmpty(),"10000 callbacks perform no provider/file reads or scheduled polling");
  ok(com.sevtinge.hyperceiler.common.log.ModuleLog.logs==logs,"no callback log spam");
  for(int uid:new int[]{0,1000,10140}){Binder.uid=uid;ok(query(service)==original,"other callers untouched");}
  Binder.uid=110139;ok(((Group[])query(service)).length==3,"launcher in another Android user");Binder.uid=10139;
  var error=new SecurityException("original permission denial");service.failure=error;
  try{query(service);ok(false,"exception propagated");}catch(SecurityException actual){ok(actual==error,"original permission exception identity");}service.failure=null;
  service.during=()->choose(Set.of("app.keep"));ok(query(service)==original,"configuration change during original query cannot mix snapshots");
  ok(((Group[])query(service))[0]==hidden,"new snapshot used by next call");
  ContextUtils.app.resolver.values.put("prefs_key_allow_hook",false);ContextUtils.app.resolver.change();ThreadPoolManager.drain();
  ok(query(service)==original,"explicit module disable preserves original reply");ContextUtils.app.resolver.values.remove("prefs_key_allow_hook");
  ContextUtils.app.resolver.duringQuery=()->{ContextUtils.app.resolver.values.put("prefs_key_home_recent_hide_card",Set.of("app.hide"));ContextUtils.app.resolver.change();};
  choose(Set.of("app.keep"));ok(((Group[])query(service)).length==3,"in-flight preference read superseded without stale publication");
  Group broken=new Group(a);broken.broken=true;service.tasks=new Group[]{hidden,broken};
  ok(query(service)==service.tasks,"malformed getter preserves whole original reply, never a partial filtered result");service.tasks=original;
  ContextUtils.app.resolver.unavailable=true;ContextUtils.app.resolver.change();ThreadPoolManager.drain();
  ok(query(service)==original&&ThreadPoolManager.queue.isEmpty(),"provider failure is stock with bounded retry");
  ContextUtils.app.resolver.unavailable=false;android.os.SystemClock.now+=2001;query(service);ThreadPoolManager.drain();
  ok(((Group[])query(service)).length==3,"unavailable provider recovers asynchronously on a later query");
  BaseHook.invoke(service,"unregisterRecentTasksListener",new Class<?>[]{IRecentTasks.Listener.class},receiver);int notified=receiver.changed;
  choose(Set.of());ok(receiver.changed==notified&&query(service)==original,"unregister and empty selection restore normal cards");
  // Optional extension remains from one physical getAll revision and leaves schema/columns unchanged.
  var values=new HashMap<String,Object>();values.put("prefs_key_home_recent_hide_card",Set.of("app.hide"));int[] snapshots={0};
  try(var c=HomeLayoutPrefsSnapshot.cursor(()->{snapshots[0]++;return values;})){
   ok(snapshots[0]==1&&c.getColumnCount()==HomeLayoutPrefsSnapshot.specs().length+1,"one physical revision, old schema intact");
   ok(HomeLayoutPrefsSnapshot.decodeTitleRecords(c.getExtras().getString(HomeLayoutPrefsSnapshot.RECENT_CARDS_KEY)).equals(Set.of("app.hide")),"selection extension round trip");
  }
  values.put("prefs_key_home_recent_hide_card",42);try(var c=HomeLayoutPrefsSnapshot.cursor(()->values)){
   ok(c!=null&&c.getExtras().getInt(HomeLayoutPrefsSnapshot.RECENT_CARDS_VERSION,0)==0,"malformed optional selection cannot break unrelated layout");
  }
  BaseHook.invoke(service,"registerRecentTasksListener",new Class<?>[]{IRecentTasks.Listener.class},receiver);
  BaseHook.invoke(service,"invalidate",new Class<?>[]{});notified=receiver.changed;
  choose(Set.of("app.hide"));ok(receiver.changed==notified,"retired Shell binder does not retain a stale launcher listener");
  var replacement=new VendorRenamedService();replacement.tasks=original;BaseHook.constructed(replacement);
  BaseHook.invoke(replacement,"registerRecentTasksListener",new Class<?>[]{IRecentTasks.Listener.class},receiver);
  choose(Set.of("app.keep"));ok(receiver.changed==notified+1,"launcher reconnect reuses validated method hook and registers fresh listener");
  service.tasks=new Group[4097];ok(query(service)==service.tasks,"oversized task reply retains original without unbounded filtering");service.tasks=original;
  choose(Set.of("app.hide"));for(var cleanup:BaseHook.cleanup)cleanup.run();
  ok(ContextUtils.app.resolver.observer==null&&query(service)==original,"cleanup unregisters observer, late callbacks preserve original");
  BaseHook.hooks.clear();BaseHook.cleanup.clear();ThreadPoolManager.queue.clear();ContextUtils.app=new android.app.Application();
  var resolver=ContextUtils.app.resolver;resolver.registerEntered=new java.util.concurrent.CountDownLatch(1);resolver.registerRelease=new java.util.concurrent.CountDownLatch(1);
  var racing=new HomeHideRecentCardsOS4();Thread starting=new Thread(racing::init);starting.start();
  ok(resolver.registerEntered.await(2,java.util.concurrent.TimeUnit.SECONDS),"observer registration entered");
  Thread retiring=new Thread(BaseHook.cleanup.get(0));retiring.start();
  var closedField=HomeHideRecentCardsOS4.class.getDeclaredField("closed");closedField.setAccessible(true);
  long limit=System.nanoTime()+2_000_000_000L;while(!closedField.getBoolean(racing)&&System.nanoTime()<limit)Thread.yield();
  ok(closedField.getBoolean(racing),"cleanup started before observer registration returns");
  resolver.registerRelease.countDown();starting.join(2000);retiring.join(2000);ThreadPoolManager.drain();
  ok(!starting.isAlive()&&!retiring.isAlive()&&resolver.observer==null,"concurrent registration and cleanup leave no late observer");
  System.out.println("OS4 hidden recent cards PASS "+checks+" checks, actual Provider/filter/hook, renamed runtime implementation, caller isolation, 10000 no-I/O warm callbacks, coherent updates and original exceptions");
 }
}
