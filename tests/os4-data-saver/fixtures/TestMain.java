import java.lang.reflect.*;import java.util.*;
import android.content.res.Resources;
import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.app.SystemUI.SystemUIB;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.DataSaverIconOS4;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
public class TestMain {
 static final String P="com.android.systemui.statusbar.";
 static final Map<Method,XposedInterface.Hooker> hooks=new LinkedHashMap<>();
 static int passes,failures,originalCalls;
 public enum Location {HOME,KEYGUARD,QS_FAKE,QS,SHADE_CARRIER_GROUP}
 public static class Icon {public boolean visible=true;}
 public static class Holder {public int type;public Icon icon=new Icon();}
 public static class ForeignHolder extends Holder{}
 public static class IconList {public Holder holder;public Holder getIconHolder(int tag,String slot){check(tag==0&&slot.equals("data_saver"));return holder;}}
 public interface ControllerInterface{}
 public static class Controller implements ControllerInterface {
  public IconList mStatusBarIconList=new IconList();public int creations,visibilityCalls,groupCalls,refreshes;public String description;public int drawable;public boolean throwCreate;
  public void setIcon(CharSequence text,String slot,int id){if(throwCreate)throw new IllegalStateException("original create failed");creations++;description=text.toString();drawable=id;check(slot.equals("data_saver"));if(mStatusBarIconList.holder==null)mStatusBarIconList.holder=new Holder();}
  public void setIconVisibility(String slot,boolean visible){visibilityCalls++;if(mStatusBarIconList.holder!=null)mStatusBarIconList.holder.icon.visible=visible;}
  public void addIconGroup(Manager group){groupCalls++;group.controller=this;}
  public void refreshIconGroup(Manager group){refreshes++;}
 }
 public static class Manager {
  public final Location mLocation;public final ArrayList<Object>mBlockList=new ArrayList<>();public Controller controller;public int updates;public Object received;
  Manager(Location where){mLocation=where;}
  public void setBlockList(List<?>input){received=input;updates++;mBlockList.clear();mBlockList.addAll(input);if(controller!=null)controller.refreshIconGroup(this);}
 }
 public static class Policy {
  public final String mSlotDataSaver="data_saver";public final ControllerInterface mIconController;public final Resources mResources=new Resources();public int callbacks;public boolean lastArg,throwCallback;
  Policy(Controller owner){mIconController=owner;}
  public void onDataSaverChanged(boolean enabled){callbacks++;lastArg=enabled;if(throwCallback)throw new IllegalArgumentException("original callback failed");((Controller)mIconController).setIconVisibility(mSlotDataSaver,enabled);}
 }
 public static class SubPolicy extends Policy {SubPolicy(Controller owner){super(owner);}}
 public static class BadPolicy {public final String mSlotDataSaver="data_saver";public final Object mIconController=null;public final Resources mResources=new Resources();public void onDataSaverChanged(boolean x){}}
 public static class BadManager {public final Object mLocation=null;public final ArrayList<Object>mBlockList=new ArrayList<>();public void setBlockList(List<?> x){}}
 public static class StaticCallback extends Policy {StaticCallback(Controller c){super(c);}public static void onDataSaverChanged(Object x){}}
 static void check(boolean b){if(!b)throw new AssertionError();}
 interface Test {void run()throws Throwable;}
 static void test(String name,Test f){reset();try{f.run();passes++;System.out.println("PASS "+name);}catch(Throwable t){failures++;System.out.println("FAIL "+name+": "+t);t.printStackTrace(System.out);}}
 static void reset(){hooks.clear();BaseHook.classes.clear();BaseHook.errors.clear();BaseHook.installs=0;BaseHook.failAt=0;BaseHook.throwInstall=false;PrefsBridge.mode=0;Resources.noSlot=false;Resources.slot="data_saver";Resources.SYSTEM.lookups=0;com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.version=4f;originalCalls=0;
  BaseHook.classes.put(P+"phone.PhoneStatusBarPolicy",Policy.class);BaseHook.classes.put(P+"phone.ui.StatusBarIconControllerImpl",Controller.class);BaseHook.classes.put(P+"phone.ui.StatusBarIconController",ControllerInterface.class);BaseHook.classes.put(P+"phone.ui.IconManager",Manager.class);BaseHook.classes.put(P+"phone.StatusBarLocation",Location.class);BaseHook.classes.put(P+"phone.ui.StatusBarIconList",IconList.class);BaseHook.classes.put(P+"phone.StatusBarIconHolder",Holder.class);BaseHook.classes.put("com.android.internal.statusbar.StatusBarIcon",Icon.class);
 }
 static void start(int mode){PrefsBridge.mode=mode;new SystemUIB().onPackageLoaded();}
 static boolean blocked(int mode,Location region){return mode==2||(mode==11&&region!=Location.QS)||(mode==12&&region==Location.QS);}
 static final class Chain implements XposedInterface.Chain {
  final Method method;final Object target;final Object[]args;Chain(Method m,Object t,Object[]a){method=m;target=t;args=a;}
  public Executable getExecutable(){return method;}public Object getThisObject(){return target;}public List<Object>getArgs(){return Arrays.asList(args);}public Object getArg(int i){return args[i];}
  public Object proceed()throws Throwable{return proceedWith(target,args);}public Object proceed(Object[]a)throws Throwable{return proceedWith(target,a);}public Object proceedWith(Object t)throws Throwable{return proceedWith(t,args);}
  public Object proceedWith(Object t,Object[]a)throws Throwable{originalCalls++;try{return method.invoke(t,a);}catch(InvocationTargetException e){throw e.getCause();}}
 }
 static Object dispatch(Object target,String name,Class<?>[]types,Object...args)throws Throwable{Method m=target.getClass().getMethod(name,types);Chain chain=new Chain(m,target,args);return hooks.containsKey(m)?hooks.get(m).intercept(chain):chain.proceed();}
 static void change(Policy target,boolean value)throws Throwable{dispatch(target,"onDataSaverChanged",new Class<?>[]{boolean.class},value);}
 static void block(Manager target,List<?>value)throws Throwable{dispatch(target,"setBlockList",new Class<?>[]{List.class},value);}
 static void attach(Controller target,Manager value)throws Throwable{dispatch(target,"addIconGroup",new Class<?>[]{Manager.class},value);}
 static Object proxy(Class<?>type,InvocationHandler h){return Proxy.newProxyInstance(TestMain.class.getClassLoader(),new Class<?>[]{type},h);}
 static void setup(){XposedInterface api=(XposedInterface)proxy(XposedInterface.class,(p,m,a)->{if(m.getName().equals("hook")){Method target=(Method)a[0];return proxy(XposedInterface.HookBuilder.class,(builder,b,ba)->{if(b.getName().equals("intercept")){check(!hooks.containsKey(target));hooks.put(target,(XposedInterface.Hooker)ba[0]);return proxy(XposedInterface.HookHandle.class,(handle,hm,ha)->{if(hm.getName().equals("unhook"))hooks.remove(target);if(hm.getName().equals("getExecutable"))return target;return null;});}return builder;});}if(m.getReturnType()==int.class)return 102;if(m.getReturnType()==long.class)return 0L;if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==String.class)return "host-api102";return null;});EzXposed.setBase$hook_xposed_102(api);EzXposed.setHotReloadEnabled(false);}
 public static void main(String[]args){setup();
  for(int mode:new int[]{0,3,-1,10,13,99})test("default_or_invalid_no_hook_"+mode,()->{start(mode);check(hooks.isEmpty()&&BaseHook.installs==0&&Resources.SYSTEM.lookups==0);});
  for(int mode:new int[]{1,2,11,12})test("os3_route_unchanged_"+mode,()->{com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.version=3f;start(mode);check(hooks.isEmpty()&&BaseHook.installs==0);});
  for(int mode:new int[]{1,2,11,12}){
   test("three_exact_hooks_"+mode,()->{start(mode);check(hooks.size()==3);check(hooks.keySet().stream().map(Method::getName).collect(java.util.stream.Collectors.toSet()).equals(Set.of("setBlockList","addIconGroup","onDataSaverChanged")));});
   for(boolean actual:new boolean[]{false,true})test("initial_callback_and_missing_holder_"+mode+"_"+actual,()->{start(mode);Controller c=new Controller();Policy policy=new SubPolicy(c);boolean want=mode==1||(mode!=2&&actual);change(policy,actual);check(policy.callbacks==1&&policy.lastArg==want&&c.visibilityCalls==1);check(c.creations==(want?1:0));if(want)check(c.mStatusBarIconList.holder.icon.visible&&c.drawable==0x7f081a1e&&c.description.equals("流量节省模式"));});
   test("existing_holder_and_repeated_real_state_"+mode,()->{start(mode);Controller c=new Controller();Holder h=new Holder();c.mStatusBarIconList.holder=h;Policy p=new Policy(c);for(int i=0;i<20;i++){boolean state=i%2==0;change(p,state);check(h.icon.visible==(mode==1||(mode!=2&&state)));}check(p.callbacks==20&&c.visibilityCalls==20&&c.creations==0&&c.mStatusBarIconList.holder==h);});
   for(Location region:new Location[]{Location.HOME,Location.KEYGUARD,Location.QS_FAKE,Location.QS}){
    test("initial_group_region_"+mode+"_"+region,()->{start(mode);Controller c=new Controller();Manager m=new Manager(region);m.mBlockList.add("vpn");m.mBlockList.add("data_saver");attach(c,m);check(c.groupCalls==1&&m.controller==c&&m.mBlockList.contains("data_saver")==blocked(mode,region)&&m.mBlockList.contains("vpn"));});
    test("reactive_repeated_lists_"+mode+"_"+region,()->{start(mode);Controller c=new Controller();Manager m=new Manager(region);attach(c,m);for(int i=0;i<6;i++){List<?>input=i%2==0?List.of("vpn","data_saver","data_saver"):List.of("alarm_clock");List<?>copy=new ArrayList<>(input);block(m,input);check(input.equals(copy)&&m.received!=input&&m.mBlockList.contains("data_saver")==blocked(mode,region));check(Collections.frequency(m.mBlockList,"data_saver")== (blocked(mode,region)?1:0));for(Object x:input)if(!x.equals("data_saver"))check(m.mBlockList.contains(x));}check(m.updates==6&&c.refreshes==6&&c.groupCalls==1);});
   }
   test("foreign_region_untouched_"+mode,()->{start(mode);Manager m=new Manager(Location.SHADE_CARRIER_GROUP);List<?>input=List.of("data_saver","vpn");block(m,input);check(m.received==input&&m.mBlockList.equals(input));Controller c=new Controller();attach(c,m);check(m.mBlockList.equals(input));});
  }
  for(int mode:new int[]{1,11,12}){
   test("unknown_holder_type_not_replaced_"+mode,()->{start(mode);Controller c=new Controller();Holder h=new Holder();h.type=8;c.mStatusBarIconList.holder=h;Policy p=new Policy(c);change(p,false);check(!p.lastArg&&c.creations==0&&c.mStatusBarIconList.holder==h);});
   test("foreign_holder_subclass_not_replaced_"+mode,()->{start(mode);Controller c=new Controller();Holder h=new ForeignHolder();c.mStatusBarIconList.holder=h;change(new Policy(c),false);check(c.creations==0&&!h.icon.visible);});
  }
  test("missing_drawable_does_not_force_false_to_true",()->{start(1);Controller c=new Controller();Policy p=new Policy(c);p.mResources.noDrawable=true;change(p,false);check(!p.lastArg&&c.creations==0);});
  test("missing_description_does_not_create",()->{start(1);Controller c=new Controller();Policy p=new Policy(c);p.mResources.noDescription=true;change(p,false);check(!p.lastArg&&c.creations==0);});
  test("creation_exception_keeps_original_callback",()->{start(1);Controller c=new Controller();c.throwCreate=true;Policy p=new Policy(c);change(p,false);check(!p.lastArg&&p.callbacks==1&&c.creations==0&&BaseHook.errors.size()==1);});
  test("original_callback_exception_propagates",()->{start(1);Controller c=new Controller();Policy p=new Policy(c);p.throwCallback=true;try{change(p,false);throw new AssertionError();}catch(IllegalArgumentException e){check(e.getMessage().equals("original callback failed")&&p.callbacks==1);}});
  test("slot_mismatch_is_untouched",()->{start(1);Controller c=new Controller();Policy p=new Policy(c);Field f=Policy.class.getField("mSlotDataSaver");f.setAccessible(true);f.set(p,"other_slot");change(p,false);check(!p.lastArg&&c.creations==0);});
  test("no_global_data_saver_state_or_controller_hook",()->{start(1);check(hooks.keySet().stream().noneMatch(m->m.getName().equals("setIconVisibility")||m.getName().equals("setIcon")));});
  test("mode_changes_require_restart_not_half_live_update",()->{start(11);PrefsBridge.mode=12;Controller c=new Controller();Manager m=new Manager(Location.HOME);attach(c,m);check(m.mBlockList.contains("data_saver"));Policy p=new Policy(c);change(p,false);check(!p.lastArg);});
  for(String name:new String[]{P+"phone.PhoneStatusBarPolicy",P+"phone.ui.StatusBarIconControllerImpl",P+"phone.ui.StatusBarIconController",P+"phone.ui.IconManager",P+"phone.StatusBarLocation",P+"phone.ui.StatusBarIconList",P+"phone.StatusBarIconHolder","com.android.internal.statusbar.StatusBarIcon"})test("missing_class_rejects_"+name,()->{BaseHook.classes.remove(name);try{start(1);throw new AssertionError();}catch(IllegalStateException expected){check(hooks.isEmpty());}});
  test("wrong_policy_field_shape_rejected",()->{BaseHook.classes.put(P+"phone.PhoneStatusBarPolicy",BadPolicy.class);try{start(1);throw new AssertionError();}catch(IllegalStateException expected){check(hooks.isEmpty());}});
  test("wrong_manager_field_shape_rejected",()->{BaseHook.classes.put(P+"phone.ui.IconManager",BadManager.class);try{start(1);throw new AssertionError();}catch(IllegalStateException expected){check(hooks.isEmpty());}});
  for(boolean missing:new boolean[]{false,true})test("unknown_framework_slot_rejected_"+missing,()->{Resources.noSlot=missing;Resources.slot="foreign";try{start(1);throw new AssertionError();}catch(IllegalStateException expected){check(hooks.isEmpty());}});
  for(int fail:new int[]{1,2,3})for(boolean throwsBackend:new boolean[]{false,true})test("partial_registration_rollback_"+fail+"_"+throwsBackend,()->{BaseHook.failAt=fail;BaseHook.throwInstall=throwsBackend;try{start(1);throw new AssertionError();}catch(IllegalStateException expected){check(hooks.isEmpty());}});
  System.out.println("RESULT passes="+passes+" failures="+failures);if(failures>0)System.exit(1);
 }
}
