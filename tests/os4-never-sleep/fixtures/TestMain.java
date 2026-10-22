import android.app.admin.DevicePolicyManager;
import android.content.Context;
import com.android.settings.KeyguardTimeoutDropDownPreference;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.rules.systemsettings.UnlockNeverSleepScreenOS4;
import com.sevtinge.hyperceiler.libhook.rules.systemsettings.UnlockNeverSleepScreen;
import com.sevtinge.hyperceiler.common.log.XposedLog;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import java.util.*;
public class TestMain {
 static int passed,failed;static boolean baseline;
 interface Case {void run() throws Exception;}
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void test(String name,Case c){
  BaseHook.failField=null;BaseHook.failMethod=null;XposedLog.errors=0;
  try{c.run();passed++;System.out.println("PASS "+name);}catch(Throwable t){failed++;System.out.println("FAIL "+name+": "+t);}
 }
 static KeyguardTimeoutDropDownPreference pref(long max){return new KeyguardTimeoutDropDownPreference(new Context(new DevicePolicyManager(max)));}
 static HookParam invoke(KeyguardTimeoutDropDownPreference p){
  var param=new HookParam(p);if(BaseHook.callback!=null)BaseHook.callback.before(param);
  if(!param.skipped)p.disableUnusableTimeouts();if(BaseHook.callback!=null)BaseHook.callback.after(param);return param;
 }
 static boolean never(KeyguardTimeoutDropDownPreference p){return Arrays.asList(p.values).contains("2147483647");}
 public static void main(String[] args){
  baseline=args.length>0&&args[0].equals("baseline");
  if(baseline){try{new UnlockNeverSleepScreen().init();}catch(Throwable t){System.out.println("EXPECTED_BASELINE_REGISTRATION_FAILURE "+t.getMessage());}}
  else new UnlockNeverSleepScreenOS4().init();
  test("target_registration",()->check(BaseHook.callback!=null&&"disableUnusableTimeouts".equals(BaseHook.registeredName),"new ROM method was not hooked"));
  test("oled_never_preserved",()->{var p=pref(0);invoke(p);check(never(p),"OLED dropped Never");check(p.restrictionUpdates==1,"restriction update count");check(p.providerWrites==0,"hook wrote settings");});
  test("null_policy_service",()->{var p=pref(0);p.mContext.service=null;invoke(p);check(never(p),"null policy did not preserve Never");});
  test("lcd_preserved",()->{var p=pref(0);p.oled=false;var values=p.values;invoke(p);check(p.values==values&&p.restrictionUpdates==1,"LCD options changed");});
  test("admin_limit_retained",()->{var p=pref(30000);invoke(p);check(!never(p)&&Arrays.equals(p.values,new String[]{"15000","30000"}),"admin limit removed");check(p.originalCalls==1,"admin original omitted");});
  test("admin_large_limit_unchanged",()->{var p=pref(Integer.MAX_VALUE);invoke(p);check(!never(p)&&p.originalCalls==1,"nonzero admin policy overridden");});
  test("negative_policy_unchanged",()->{var p=pref(-1);invoke(p);check(!p.enabled&&p.originalCalls==1,"unexpected policy changed");});
  test("config_restriction_retained",()->{var p=pref(0);p.mContext.restricted=true;invoke(p);check(p.disabledByAdmin&&p.restrictionUpdates==1,"configuration restriction removed");});
  test("user_selects_never_through_rom",()->{var p=pref(0);invoke(p);check(never(p),"Never not selectable");p.onPreferenceChange("2147483647");check(p.storedTimeout==2147483647L&&p.providerWrites==1,"ROM selection write changed");});
  test("already_selected_never",()->{var p=pref(0);p.value="2147483647";invoke(p);check(never(p)&&p.value.equals("2147483647"),"selection lost");});
  test("repeat_idempotent",()->{var p=pref(0);for(int i=0;i<50;i++)invoke(p);check(never(p)&&p.values.length==7&&p.restrictionUpdates==50&&p.providerWrites==0,"repeated calls changed options");});
  test("policy_added_on_next_call",()->{var p=pref(0);invoke(p);((DevicePolicyManager)p.mContext.service).limit=60000;invoke(p);check(!never(p)&&p.values.length==3&&p.originalCalls==1,"later admin policy bypassed");});
  test("separate_instances",()->{var a=pref(0);var b=pref(30000);invoke(a);invoke(b);invoke(a);check(never(a)&&!never(b),"instances shared state");});
  test("missing_never_uses_original",()->{var p=pref(0);p.entries=new String[]{"30秒"};p.values=new String[]{"30000"};invoke(p);check(p.originalCalls==1&&!never(p),"invented unknown option");});
  test("duplicate_never_uses_original",()->{var p=pref(0);p.entries=new String[]{"30秒","A","B"};p.values=new String[]{"30000","2147483647","2147483647"};invoke(p);check(p.originalCalls==1&&!never(p),"ambiguous Never entries changed");});
  test("oversized_value_uses_original",()->{var p=pref(0);p.entries=new String[]{"30秒","Never","future"};p.values=new String[]{"30000","2147483647","2147483648"};invoke(p);check(p.originalCalls==1&&p.values.length==1,"unknown future value accepted");});
  test("nonpositive_value_uses_original",()->{var p=pref(0);p.entries=new String[]{"zero","Never"};p.values=new String[]{"0","2147483647"};invoke(p);check(p.originalCalls==1&&!never(p),"unexpected zero value accepted");});
  test("mismatched_arrays_use_original",()->{var p=pref(0);p.entries=new String[]{"one"};try{invoke(p);}catch(IndexOutOfBoundsException expected){}check(p.originalCalls==1,"array mismatch short-circuited");});
  test("null_entries_use_original",()->{var p=pref(0);p.entries=null;try{invoke(p);}catch(NullPointerException expected){}check(p.originalCalls==1,"null entries short-circuited");});
  test("invalid_number_uses_original",()->{var p=pref(0);p.values[0]="invalid";try{invoke(p);}catch(NumberFormatException expected){}check(p.originalCalls==1,"invalid value changed");});
  test("missing_context_uses_original",()->{var p=pref(0);BaseHook.failField="mContext";invoke(p);check(p.originalCalls==1&&!never(p),"missing field changed original");});
  test("read_failure_uses_original",()->{var p=pref(0);BaseHook.failMethod="getEntries";invoke(p);check(p.originalCalls==1&&!never(p),"read failure changed original");});
  test("update_failure_uses_original",()->{var p=pref(0);BaseHook.failMethod="updateDisableState";invoke(p);check(p.originalCalls==1&&!never(p),"failed restriction update skipped original");});
  test("policy_read_failure_uses_original",()->{var p=pref(0);((DevicePolicyManager)p.mContext.service).failures=1;try{invoke(p);}catch(SecurityException expected){}check(p.originalCalls==1,"policy read failure skipped original");});
  test("unknown_service_uses_original",()->{var p=pref(0);p.mContext.service=new Object();try{invoke(p);}catch(ClassCastException expected){}check(p.originalCalls==1,"unknown policy service skipped original");});
  test("null_context_uses_original",()->{var p=pref(0);p.mContext=null;try{invoke(p);}catch(NullPointerException expected){}check(p.originalCalls==1,"null context skipped original");});
  System.out.println("RESULT passed="+passed+" failed="+failed+" total="+(passed+failed)+" baseline="+baseline);
  if(failed!=0)System.exit(1);
 }
}
