import java.lang.reflect.*;import java.util.*;
import android.view.View;
import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.CustomCarrierText;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.utils.api.PropUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;
import com.android.systemui.controlcenter.shade.*;
import com.android.systemui.plugins.miui.statusbar.MiuiPrivacyController;
public class TestMain{
 static final Map<Method,XposedInterface.Hooker>hooks=new LinkedHashMap<>();static int passes,failures,originalCalls;
 static final String P="com.android.systemui.controlcenter.shade.";
 static void check(boolean b){if(!b)throw new AssertionError();}
 interface Test{void run()throws Throwable;}
 static void test(String n,Test f){reset();try{f.run();passes++;System.out.println("PASS "+n);}catch(Throwable t){failures++;System.out.println("FAIL "+n+": "+t);t.printStackTrace(System.out);}}
 static void reset(){hooks.clear();BaseHook.classes.clear();BaseHook.errors.clear();BaseHook.installs=BaseHook.failAt=0;BaseHook.throwInstall=false;PrefsBridge.mode=0;DeviceHelper.System.version=4;PropUtils.name="My Phone";PropUtils.fail=false;originalCalls=0;
  BaseHook.classes.put(P+"ControlCenterCarrierText$mCarrierTextCallback$1",ControlCenterCarrierText$mCarrierTextCallback$1.class);BaseHook.classes.put(P+"MiuiCarrierTextLayout",MiuiCarrierTextLayout.class);BaseHook.classes.put(P+"ControlCenterHeaderController",ControlCenterHeaderController.class);BaseHook.classes.put("com.android.systemui.plugins.miui.statusbar.MiuiPrivacyController$MiuiPromptInfo",MiuiPrivacyController.MiuiPromptInfo.class);BaseHook.classes.put("com.android.systemui.plugins.miui.statusbar.MiuiPrivacyController$AndroidPromptInfo",MiuiPrivacyController.AndroidPromptInfo.class);
 }
 static void start(int mode){PrefsBridge.mode=mode;CustomCarrierText.INSTANCE.init();}
 static final class Chain implements XposedInterface.Chain{final Method method;final Object target;final Object[]args;Chain(Method m,Object t,Object[]a){method=m;target=t;args=a;}public Executable getExecutable(){return method;}public Object getThisObject(){return target;}public List<Object>getArgs(){return Arrays.asList(args);}public Object getArg(int i){return args[i];}public Object proceed()throws Throwable{return proceedWith(target,args);}public Object proceed(Object[]a)throws Throwable{return proceedWith(target,a);}public Object proceedWith(Object t)throws Throwable{return proceedWith(t,args);}public Object proceedWith(Object t,Object[]a)throws Throwable{originalCalls++;try{return method.invoke(t,a);}catch(InvocationTargetException e){throw e.getCause();}}}
 static Object dispatch(Object t,String n,Class<?>[]sig,Object...args)throws Throwable{Method m=t.getClass().getMethod(n,sig);Chain c=new Chain(m,t,args);return hooks.containsKey(m)?hooks.get(m).intercept(c):c.proceed();}
 static void change(ControlCenterCarrierText$mCarrierTextCallback$1 c,int status,String text,int slot)throws Throwable{dispatch(c,"onCarrierTextChanged",new Class<?>[]{int.class,String.class,int.class},status,text,slot);}
 static void measure(MiuiCarrierTextLayout t)throws Throwable{dispatch(t,"onMeasure",new Class<?>[]{int.class,int.class},123,456);}
 static void privacy(ControlCenterHeaderController t,MiuiPrivacyController.MiuiPromptInfo a,MiuiPrivacyController.AndroidPromptInfo b)throws Throwable{dispatch(t,"updateCarrierAndPrivacyVisible",new Class<?>[]{MiuiPrivacyController.MiuiPromptInfo.class,MiuiPrivacyController.AndroidPromptInfo.class},a,b);}
 static Object proxy(Class<?>t,InvocationHandler h){return Proxy.newProxyInstance(TestMain.class.getClassLoader(),new Class<?>[]{t},h);}
 static void setup(){XposedInterface api=(XposedInterface)proxy(XposedInterface.class,(p,m,a)->{if(m.getName().equals("hook")){Method target=(Method)a[0];return proxy(XposedInterface.HookBuilder.class,(b,bm,ba)->{if(bm.getName().equals("intercept")){check(!hooks.containsKey(target));hooks.put(target,(XposedInterface.Hooker)ba[0]);return proxy(XposedInterface.HookHandle.class,(h,hm,ha)->{if(hm.getName().equals("unhook"))hooks.remove(target);if(hm.getName().equals("getExecutable"))return target;return null;});}return b;});}if(m.getReturnType()==int.class)return 102;if(m.getReturnType()==long.class)return 0L;if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==String.class)return "host-api102";return null;});EzXposed.setBase$hook_xposed_102(api);EzXposed.setHotReloadEnabled(false);}
 // Direct transcription of the full ROM fireCarrierTextChanged dedup/cache order.
 static final class Producer{String[]current={null,null};int available,listenersSize,delivered;List<ControlCenterCarrierText$mCarrierTextCallback$1>listeners=new ArrayList<>();
  void fire(int count,String text,int slot)throws Throwable{if(slot<0||slot>=current.length)return;if(Objects.equals(current[slot],text)&&available==count&&listeners.size()==listenersSize)return;current[slot]=text;available=count;listenersSize=listeners.size();for(var listener:listeners){change(listener,count,text,slot);delivered++;}}
  void update(int count,String left,String right)throws Throwable{fire(count,left,0);fire(count,right,1);}
  void add(ControlCenterCarrierText$mCarrierTextCallback$1 listener)throws Throwable{listeners.add(listener);int slot=listener.acceptedSlot;if(slot>=0&&slot<current.length)change(listener,available,current[slot],slot);/* two-argument default callback is no-op for this target */}
 }
 public static class BadCallback{public void onCarrierTextChanged(int a,int b,String c){}}
 public static class StaticCallback{public static void onCarrierTextChanged(int a,String b,int c){}}
 public static class BadLayout extends View{public Object carrierSeparatorView,carrierSeparatorText;public void onMeasure(int w,int h){}}
 public static void main(String[]args){setup();
  for(int m:new int[]{0,-1,4,99})test("default_invalid_"+m,()->{start(m);check(hooks.isEmpty());});
  for(int mode:new int[]{1,2,3}){
   test("exact_hooks_no_HD_or_empty_legacy_"+mode,()->{start(mode);check(hooks.size()==(mode==3?1:2));for(Method m:hooks.keySet())check(!m.getName().equals("isVisible")&&!m.getDeclaringClass().getName().contains("MiuiKeyguardStatusBarView"));});
   for(int status:new int[]{0,1,2})for(int slot:new int[]{0,1})test("mapping_"+mode+"_"+status+"_"+slot,()->{start(mode);var c=new ControlCenterCarrierText$mCarrierTextCallback$1();String input="中国  |  移动 | e&";change(c,status,input,slot);String want=mode==1?"中国移动e&":mode==2?"":status==0?input:slot==0?"My Phone":"";check(c.calls==1&&c.status==status&&c.slot==slot&&Objects.equals(c.text,want)&&originalCalls==1);});
   for(int status:new int[]{-1,3,999})test("unknown_status_"+mode+"_"+status,()->{start(mode);var c=new ControlCenterCarrierText$mCarrierTextCallback$1();change(c,status,"SIM",0);check(c.text.equals("SIM")&&c.status==status&&c.slot==0);});
   for(int slot:new int[]{-1,2,999})test("unknown_slot_"+mode+"_"+slot,()->{start(mode);var c=new ControlCenterCarrierText$mCarrierTextCallback$1();change(c,1,"SIM",slot);check(c.text.equals("SIM")&&c.slot==slot);});
   for(String text:new String[]{null,""})test("empty_slot_"+mode+"_"+text,()->{start(mode);var c=new ControlCenterCarrierText$mCarrierTextCallback$1();change(c,1,text,0);check(Objects.equals(c.text,mode==3?"My Phone":text)&&c.calls==1);});
   test("original_exception_"+mode,()->{start(mode);var c=new ControlCenterCarrierText$mCarrierTextCallback$1();c.fail=true;try{change(c,1,"SIM",0);throw new AssertionError();}catch(IllegalArgumentException expected){check(c.calls==1&&expected.getMessage().equals("original"));}});
  }
  for(int presentSlot:new int[]{0,1})test("single_sim_producer_pair_"+presentSlot,()->{start(3);var left=new ControlCenterCarrierText$mCarrierTextCallback$1();var right=new ControlCenterCarrierText$mCarrierTextCallback$1();change(left,1,presentSlot==0?"SIM":"",0);change(right,1,presentSlot==1?"SIM":"",1);check(left.text.equals("My Phone")&&right.text.equals(""));check(left.status==1&&right.status==1&&left.slot==0&&right.slot==1);});
  test("two_sim_producer_pair",()->{start(3);var left=new ControlCenterCarrierText$mCarrierTextCallback$1();var right=new ControlCenterCarrierText$mCarrierTextCallback$1();change(left,2,"SIM A",0);change(right,2,"SIM B",1);check(left.text.equals("My Phone")&&right.text.equals("")&&left.status==2&&right.status==2&&left.slot==0&&right.slot==1);});
  test("zero_carrier_fallback_pair",()->{start(3);var left=new ControlCenterCarrierText$mCarrierTextCallback$1();var right=new ControlCenterCarrierText$mCarrierTextCallback$1();change(left,0,"",0);change(right,0,"Emergency calls only",1);check(left.text.equals("")&&right.text.equals("Emergency calls only"));});
  test("producer_dedup_transitions_fixed_display_slot",()->{start(3);var p=new Producer();var left=new ControlCenterCarrierText$mCarrierTextCallback$1();left.acceptedSlot=0;var right=new ControlCenterCarrierText$mCarrierTextCallback$1();right.acceptedSlot=1;p.add(left);p.add(right);
   int[]counts={2,1,2,1,1,0,1,2,0,1};String[][]texts={{"A","B"},{"","B"},{"A","B"},{"A",""},{"","B"},{"","Emergency"},{"","B"},{"C","B"},{"","Flight mode"},{"A",""}};
   for(int i=0;i<counts.length;i++){p.update(counts[i],texts[i][0],texts[i][1]);check(left.text.equals(counts[i]==0?texts[i][0]:"My Phone")&&right.text.equals(counts[i]==0?texts[i][1]:""));check(Arrays.equals(p.current,texts[i])&&p.available==counts[i]);int before=p.delivered;p.update(counts[i],texts[i][0],texts[i][1]);check(p.delivered==before);}
   var lateLeft=new ControlCenterCarrierText$mCarrierTextCallback$1();lateLeft.acceptedSlot=0;var lateRight=new ControlCenterCarrierText$mCarrierTextCallback$1();lateRight.acceptedSlot=1;p.add(lateLeft);p.add(lateRight);check(lateLeft.text.equals("My Phone")&&lateRight.text.equals(""));
  });
  test("producer_unconfigured_name_preserves_all_raw_texts",()->{PropUtils.name="";start(3);var p=new Producer();var left=new ControlCenterCarrierText$mCarrierTextCallback$1();left.acceptedSlot=0;var right=new ControlCenterCarrierText$mCarrierTextCallback$1();right.acceptedSlot=1;p.add(left);p.add(right);p.update(2,"A","B");p.update(1,"","B");check(left.text.equals("")&&right.text.equals("B"));});
  for(String name:new String[]{null,"","  "})test("unconfigured_name_"+name,()->{PropUtils.name=name;start(3);var c=new ControlCenterCarrierText$mCarrierTextCallback$1();for(int slot=0;slot<2;slot++){change(c,1,"SIM",slot);check(c.text.equals("SIM"));}});
  test("property_failure_preserves_text",()->{PropUtils.fail=true;start(3);var c=new ControlCenterCarrierText$mCarrierTextCallback$1();change(c,1,"SIM",0);check(c.text.equals("SIM")&&BaseHook.errors.size()==1);});
  test("mode_captured_at_init",()->{start(3);PrefsBridge.mode=2;var c=new ControlCenterCarrierText$mCarrierTextCallback$1();change(c,1,"SIM",0);check(c.text.equals("My Phone"));});
  for(int visibility:new int[]{View.VISIBLE,View.INVISIBLE,View.GONE})test("original_measure_preserved_"+visibility,()->{start(1);var t=new MiuiCarrierTextLayout();t.originalVisibility=visibility;for(int i=0;i<4;i++)measure(t);check(t.measures==4&&t.width==123&&t.height==456&&t.carrierSeparatorView.getVisibility()==(visibility==View.VISIBLE?View.INVISIBLE:visibility)&&t.carrierSeparatorText.getVisibility()==t.carrierSeparatorView.getVisibility());});
  test("measurement_failure_preserved",()->{start(1);var t=new MiuiCarrierTextLayout();t.fail=true;try{measure(t);throw new AssertionError();}catch(IllegalArgumentException expected){check(t.measures==1&&t.carrierSeparatorText.getVisibility()==View.VISIBLE);}});
  for(int visibility:new int[]{View.VISIBLE,View.INVISIBLE,View.GONE})test("privacy_and_visibility_preserved_"+visibility,()->{start(2);var t=new ControlCenterHeaderController();t.originalVisibility=visibility;var a=new MiuiPrivacyController.MiuiPromptInfo();var b=new MiuiPrivacyController.AndroidPromptInfo();privacy(t,a,b);check(t.updates==1&&t.miui==a&&t.android==b&&t.carrierLayout.getVisibility()==(visibility==0?4:visibility));});
  test("privacy_failure_preserved",()->{start(2);var t=new ControlCenterHeaderController();t.fail=true;try{privacy(t,null,null);throw new AssertionError();}catch(IllegalArgumentException expected){check(t.updates==1&&t.carrierLayout.getVisibility()==View.VISIBLE);}});
  for(Class<?>shape:new Class<?>[]{BadCallback.class,StaticCallback.class})test("bad_callback_"+shape,()->{BaseHook.classes.put(P+"ControlCenterCarrierText$mCarrierTextCallback$1",shape);try{start(3);throw new AssertionError();}catch(IllegalStateException expected){check(hooks.isEmpty());}});
  test("bad_separator_shape",()->{BaseHook.classes.put(P+"MiuiCarrierTextLayout",BadLayout.class);try{start(1);throw new AssertionError();}catch(IllegalStateException expected){check(hooks.isEmpty());}});
  for(int mode:new int[]{1,2,3})for(int fail=1;fail<=(mode==3?1:2);fail++){final int index=fail;for(boolean throwing:new boolean[]{false,true})test("registration_rollback_"+mode+"_"+fail+"_"+throwing,()->{BaseHook.failAt=index;BaseHook.throwInstall=throwing;try{start(mode);throw new AssertionError();}catch(IllegalStateException expected){check(hooks.isEmpty());}});}
  test("os3_original_registration_path",()->{DeviceHelper.System.version=3;start(3);check(hooks.keySet().stream().anyMatch(m->m.getName().equals("isVisible")));check(hooks.keySet().stream().noneMatch(m->m.getName().equals("onMeasure")));});
  System.out.println("RESULT passes="+passes+" failures="+failures);if(failures>0)System.exit(1);
 }
}
