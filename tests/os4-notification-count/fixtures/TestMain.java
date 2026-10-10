import java.lang.reflect.*;
import java.util.*;
import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.app.SystemUI.SystemUIB;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.RemoveNotifNumLimitOS4;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;

public class TestMain {
 static final String C="com.android.systemui.statusbar.notification.collection.coordinator.CountLimitCoordinator";
 static final String E="com.android.systemui.statusbar.notification.collection.NotificationEntry";
 static final Map<Method,XposedInterface.Hooker> hooks=new LinkedHashMap<>();
 static int passes,failures,originalCalls;
 public static class Entry { }
 public static class Coordinator {
  public Object pipeline;public int bindings,quotaCalls,dismissals;
  public void attach(Object pipeline){this.pipeline=pipeline;bindings++;}
  public void dismissNotification(Entry e){dismissals++;}
 }
 public static class Callback {
  public Coordinator f$0;public int count;
  public void onViewBound$1(Entry e){f$0.quotaCalls++;if(count>10)f$0.dismissNotification(e);}
  public void onViewBound(Entry e){throw new AssertionError("legacy-name decoy must never be selected");}
  public void onViewBound$1(Object e){throw new AssertionError("overload decoy must never be selected");}
 }
 public static class WrongOwner {public Object f$0;public void onViewBound$1(Entry e){} }
 public static class WrongReturn {public Coordinator f$0;public int onViewBound$1(Entry e){return 1;} }
 public static class StaticCallback {public Coordinator f$0;public static void onViewBound$1(Entry e){} }
 public static class MissingField {public void onViewBound$1(Entry e){} }
 public static class LegacyOnly {public Coordinator f$0;public void onViewBound(Entry e){} }
 public static class OtherParam {public Coordinator f$0;public void onViewBound$1(Object e){} }
 public static class OtherListener {public int called;public void onViewBound$1(Entry e){called++;} }
 static void check(boolean b){if(!b)throw new AssertionError();}
 interface Test {void run()throws Throwable;}
 static void test(String name,Test f){reset();try{f.run();passes++;System.out.println("PASS "+name);}catch(Throwable t){failures++;System.out.println("FAIL "+name+": "+t);t.printStackTrace(System.out);}}
 static void reset(){hooks.clear();BaseHook.classes.clear();BaseHook.classes.put(C,Coordinator.class);BaseHook.classes.put(C+"$$ExternalSyntheticLambda0",Callback.class);BaseHook.classes.put(E,Entry.class);BaseHook.nullHandle=false;BaseHook.throwInstall=false;PrefsBridge.enabled=false;com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.version=4f;originalCalls=0;}
 static void rejected(Class<?> c){BaseHook.classes.put(C+"$$ExternalSyntheticLambda0",c);try{new RemoveNotifNumLimitOS4().init();throw new AssertionError("accepted unknown shape");}catch(IllegalStateException expected){check(hooks.isEmpty());}}
 static final class Chain implements XposedInterface.Chain {
  final Method method;final Object target;final Object[] args;
  Chain(Method m,Object t,Object[] a){method=m;target=t;args=a;}
  public Executable getExecutable(){return method;}public Object getThisObject(){return target;}public List<Object>getArgs(){return Arrays.asList(args);}public Object getArg(int i){return args[i];}
  public Object proceed()throws Throwable{return proceedWith(target,args);}public Object proceed(Object[]a)throws Throwable{return proceedWith(target,a);}public Object proceedWith(Object t)throws Throwable{return proceedWith(t,args);}
  public Object proceedWith(Object t,Object[]a)throws Throwable{originalCalls++;try{return method.invoke(t,a);}catch(InvocationTargetException e){throw e.getCause();}}
 }
 static Object call(Object target,String name,Class<?> p,Object value)throws Throwable{
  Method m=target.getClass().getMethod(name,p);Chain c=new Chain(m,target,new Object[]{value});return hooks.containsKey(m)?hooks.get(m).intercept(c):c.proceed();
 }
 static Object proxy(Class<?> type,InvocationHandler h){return Proxy.newProxyInstance(TestMain.class.getClassLoader(),new Class<?>[]{type},h);}
 static void setupRealEzHook(){
  XposedInterface api=(XposedInterface)proxy(XposedInterface.class,(p,m,a)->{
   if(m.getName().equals("hook")){
    Method target=(Method)a[0];
    return proxy(XposedInterface.HookBuilder.class,(builder,b,ba)->{
     if(b.getName().equals("intercept")){
      check(!hooks.containsKey(target));hooks.put(target,(XposedInterface.Hooker)ba[0]);
      return proxy(XposedInterface.HookHandle.class,(handle,hm,ha)->{if(hm.getName().equals("unhook"))hooks.remove(target);if(hm.getName().equals("getExecutable"))return target;return null;});
     }
     return builder;
    });
   }
   if(m.getReturnType()==int.class)return 102;
   if(m.getReturnType()==long.class)return 0L;
   if(m.getReturnType()==boolean.class)return false;
   if(m.getReturnType()==String.class)return "host-api102";
   return null;
  });
  EzXposed.setBase$hook_xposed_102(api);EzXposed.setHotReloadEnabled(false);
 }
 public static void main(String[]args){setupRealEzHook();
  test("route_default_keeps_original_quota",()->{new SystemUIB().onPackageLoaded();check(hooks.isEmpty());Coordinator c=new Coordinator();Callback l=new Callback();l.f$0=c;l.count=11;call(l,"onViewBound$1",Entry.class,new Entry());check(c.quotaCalls==1&&c.dismissals==1&&originalCalls==1);});
  test("route_enabled_exact_callback_only",()->{PrefsBridge.enabled=true;new SystemUIB().onPackageLoaded();check(hooks.size()==1);Method m=hooks.keySet().iterator().next();check(m.getDeclaringClass()==Callback.class&&m.getName().equals("onViewBound$1")&&m.getParameterTypes()[0]==Entry.class);});
  for(boolean enabled:new boolean[]{false,true})test("os3_route_preserved_pref_"+enabled,()->{com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.version=3f;PrefsBridge.enabled=enabled;new SystemUIB().onPackageLoaded();check(hooks.isEmpty());Coordinator c=new Coordinator();Callback l=new Callback();l.f$0=c;l.count=11;call(l,"onViewBound$1",Entry.class,new Entry());check(c.quotaCalls==1&&c.dismissals==1&&originalCalls==1);});
  test("real_ezhook_replacement_skips_original",()->{new RemoveNotifNumLimitOS4().init();Coordinator c=new Coordinator();Callback l=new Callback();l.f$0=c;l.count=11;check(call(l,"onViewBound$1",Entry.class,new Entry())==null);check(c.quotaCalls==0&&c.dismissals==0&&originalCalls==0);});
  test("attach_pipeline_and_listener_registration_preserved",()->{new RemoveNotifNumLimitOS4().init();Coordinator c=new Coordinator();Object pipeline=new Object();call(c,"attach",Object.class,pipeline);check(c.pipeline==pipeline&&c.bindings==1&&originalCalls==1);});
  test("other_bind_listener_preserved",()->{new RemoveNotifNumLimitOS4().init();OtherListener l=new OtherListener();call(l,"onViewBound$1",Entry.class,new Entry());check(l.called==1&&originalCalls==1);});
  test("ordinary_dismiss_preserved",()->{new RemoveNotifNumLimitOS4().init();Coordinator c=new Coordinator();call(c,"dismissNotification",Entry.class,new Entry());check(c.dismissals==1&&originalCalls==1);});
  test("repeated_bind_callbacks_have_no_new_state",()->{new RemoveNotifNumLimitOS4().init();Coordinator c=new Coordinator();Callback l=new Callback();l.f$0=c;l.count=21;for(int i=0;i<100;i++)call(l,"onViewBound$1",Entry.class,new Entry());check(hooks.size()==1&&c.quotaCalls==0&&c.dismissals==0&&originalCalls==0);});
  test("null_entry_does_not_call_original",()->{new RemoveNotifNumLimitOS4().init();Callback l=new Callback();check(call(l,"onViewBound$1",Entry.class,null)==null&&originalCalls==0);});
  for(String name:new String[]{C,C+"$$ExternalSyntheticLambda0",E})test("missing_class_"+name,()->{BaseHook.classes.remove(name);try{new RemoveNotifNumLimitOS4().init();throw new AssertionError();}catch(IllegalStateException expected){check(hooks.isEmpty());}});
  test("foreign_owner_rejected",()->rejected(WrongOwner.class));test("non_void_rejected",()->rejected(WrongReturn.class));test("static_callback_rejected",()->rejected(StaticCallback.class));test("no_owner_field_rejected",()->rejected(MissingField.class));test("legacy_name_no_fallback",()->rejected(LegacyOnly.class));test("wrong_parameter_no_fallback",()->rejected(OtherParam.class));
  test("null_hook_handle_is_failure",()->{BaseHook.nullHandle=true;try{new RemoveNotifNumLimitOS4().init();throw new AssertionError();}catch(IllegalStateException expected){check(hooks.isEmpty());}});
  test("backend_failure_is_not_success",()->{BaseHook.throwInstall=true;try{new RemoveNotifNumLimitOS4().init();throw new AssertionError();}catch(IllegalStateException expected){check(hooks.isEmpty());}});
  test("unhook_restores_original_dispatch",()->{new RemoveNotifNumLimitOS4().init();hooks.clear();Coordinator c=new Coordinator();Callback l=new Callback();l.f$0=c;l.count=11;call(l,"onViewBound$1",Entry.class,new Entry());check(c.quotaCalls==1&&c.dismissals==1);});
  System.out.println("RESULT passes="+passes+" failures="+failures);if(failures>0)System.exit(1);
 }
}
