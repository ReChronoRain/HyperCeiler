/* SPDX-License-Identifier: AGPL-3.0-or-later */
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.*;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashMonitor;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.AmbientNativeColorOS4;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface.*;
import io.github.lingqiqi5211.ezhooktool.xposed.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class InitPrefsTest {
    static int installed, observed;
    static boolean failRule;
    static String packageName="com.android.systemui";
    static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    static void equal(int actual,int expected,String message) { check(actual==expected,message+": got="+actual+", expected="+expected); }

    static final class FixtureModule extends BaseLoad {
        @Override public void onPackageLoaded() {
            // The consumer is reached through real BaseLoad.onLoad/executeHook.
            observed=PrefsBridge.getInt("fixture_value",-1);
            installed++;
            if(failRule)throw new IllegalStateException("simulated rule installation failure");
        }
    }
    static final class Entry extends XposedInitEntry {
        @Override protected void invokeInit(PackageTarget target) { new FixtureModule().onLoad(target); }
        void initializePreferences() { initPrefs(); }
    }
    static final class Remote implements java.lang.reflect.InvocationHandler {
        final AtomicInteger calls=new AtomicInteger();
        final Map<String,Object> values=new HashMap<>();
        int failures, nulls;
        boolean failReads, failFramework;
        Remote(int failures) {
            this.failures=failures;
            values.put("prefs_key_allow_hook",true);
            values.put("prefs_key_fixture_value",43);
        }
        final SharedPreferences prefs=(SharedPreferences)Proxy.newProxyInstance(
            getClass().getClassLoader(),new Class<?>[]{SharedPreferences.class},(p,m,a)->{
                if(failReads && m.getName().startsWith("get"))throw new SecurityException("remote read unavailable");
                if(m.getName().equals("getAll"))return new HashMap<>(values);
                if(m.getName().equals("contains"))return values.containsKey(a[0]);
                if(m.getName().startsWith("get"))return values.getOrDefault(a[0],a[1]);
                throw new AssertionError("Unexpected SharedPreferences operation: "+m);
            });
        public Object invoke(Object p,java.lang.reflect.Method m,Object[] a) {
            if (failFramework && m.getName().startsWith("getFramework")) throw new IllegalStateException("framework identity unavailable");
            return switch(m.getName()) {
                case "getRemotePreferences" -> {
                    equal(((String)a[0]).compareTo(PrefsBridge.REMOTE_PREFS_GROUP),0,"preference group");
                    int n=calls.incrementAndGet();
                    if(n<=failures)throw new IllegalStateException("simulated service not ready");
                    yield n<=failures+nulls?null:prefs;
                }
                case "getFrameworkName" -> "FixtureFramework";
                case "getFrameworkVersion" -> "102-test";
                case "getFrameworkVersionCode" -> 102L;
                case "getApiVersion" -> 102;
                default -> throw new AssertionError("Unexpected framework call: "+m);
            };
        }
        Entry entry() {
            Entry e=new Entry();
            // Real API 102 wrapper forwards its final getRemotePreferences to this framework.
            e.attachFramework((XposedInterface)Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{XposedInterface.class},this),()->{});
            return e;
        }
    }
    record ModuleParam(boolean isSystemServer,String getProcessName) implements ModuleLoadedParam {}
    record PackageParam(boolean isFirstPackage) implements PackageReadyParam {
        public String getPackageName(){return packageName;}
        public ApplicationInfo getApplicationInfo(){return new ApplicationInfo();}
        public ClassLoader getDefaultClassLoader(){return getClass().getClassLoader();}
        public ClassLoader getClassLoader(){return getDefaultClassLoader();}
        public android.app.AppComponentFactory getAppComponentFactory(){return null;}
    }
    static final class ReloadParam implements HotReloadedParam {
        public boolean isSystemServer(){return false;}
        public String getProcessName(){return "com.android.systemui";}
        public android.os.Bundle getExtras(){return null;}
        public Object getSavedInstanceState(){return null;}
        public List<XposedInterface.HookHandle> getOldHookHandles(){return List.of();}
    }
    static void boot(Entry e,boolean system) { e.onModuleLoaded(new ModuleParam(system,system?"system_server":"com.android.systemui")); }
    static void ready(Entry e,boolean system) {
        if(system)e.onSystemServerStarting(()->InitPrefsTest.class.getClassLoader());
        else e.onPackageReady(new PackageParam(true));
    }
    static void recovered(Remote r) {
        equal(r.calls.get(),2,"one bootstrap request plus one target-ready retry");
        equal(installed,1,"one initialized rule consumer");
        equal(observed,43,"consumer sees recovered preferences");
        check(BaseLoad.getHotReloadBlockReason()==null,"successful recovery must not poison generation");
        check(EzXposed.dispatcher.getState()==TargetReadyState.SUCCEEDED,"dispatcher succeeded");
    }
    public static void main(String[] args) throws Exception {
        String name=args[0];
        switch(name) {
            case "package_transient", "system_transient" -> {
                boolean system=name.equals("system_transient"); Remote r=new Remote(1);Entry e=r.entry();
                boot(e,system);equal(installed,0,"bootstrap does not install target rules");ready(e,system);recovered(r);
                equal(CrashMonitor.instances,system?1:0,"system entry hooks dispatch normally");
            }
            case "healthy_idempotent" -> {
                Remote r=new Remote(0);Entry e=r.entry();boot(e,false);
                PrefsBridge.putHookCache("fixture_value",99);
                ready(e,false);ready(e,false);
                equal(r.calls.get(),1,"healthy preference handle must not be rebound");
                equal(installed,1,"duplicate target-ready must not duplicate hooks");
                equal(observed,99,"idempotent retry must preserve live hook cache");
                equal(EzXposed.callbacksRegistered,1,"one callback registration");
            }
            case "secondary_package" -> {
                Remote r=new Remote(1);Entry e=r.entry();boot(e,false);
                e.onPackageReady(new PackageParam(false));equal(r.calls.get(),1,"secondary package ignored");
                equal(installed,0,"secondary package cannot initialize rule consumer");ready(e,false);recovered(r);
            }
            case "persistent_failure" -> {
                Remote r=new Remote(2);Entry e=r.entry();boot(e,false);ready(e,false);
                equal(r.calls.get(),2,"target-ready retry attempted");equal(installed,0,"failed preferences cannot run rules");
                check(BaseLoad.getHotReloadBlockReason()!=null,"failed initialization is recorded");
                check(EzXposed.dispatcher.getState()==TargetReadyState.FAILED,"dependency preserves failed generation");
                ready(e,false);equal(r.calls.get(),2,"no invented asynchronous or duplicate retry");
            }
            case "allow_hook_gate", "framework_gate" -> {
                Remote r=new Remote(1);
                if(name.equals("allow_hook_gate"))r.values.put("prefs_key_allow_hook",false);
                else {
                    r.values.put("prefs_key_framework_api_allow_hook",false);
                    r.values.put("prefs_key_framework_check_reason","blocked by real validation");
                    r.values.put("prefs_key_framework_check_name","FixtureFramework");
                    r.values.put("prefs_key_framework_check_version","102-test");
                    r.values.put("prefs_key_framework_check_version_code",102L);
                }
                Entry e=r.entry();boot(e,false);ready(e,false);equal(r.calls.get(),2,"initialization still retries");
                equal(installed,0,"existing module/framework denial remains effective");
                check(EzXposed.dispatcher.getState()==TargetReadyState.SUCCEEDED,"legitimate denial is not initialization failure");
            }
            case "hot_reload_transient" -> {
                Remote r=new Remote(1);Entry e=r.entry();e.onHotReloaded(new ReloadParam());recovered(r);
                equal(EzXposed.publishedReloads,1,"restoration completes only after recovered initialization");
            }
            case "hot_reload_persistent" -> {
                Remote r=new Remote(2);Entry e=r.entry();boolean rejected=false;
                try {e.onHotReloaded(new ReloadParam());}catch(IllegalStateException expected){rejected=true;}
                check(rejected,"persistent failure rejects reload");equal(r.calls.get(),2,"new generation retries at restored target");
                equal(installed,0,"failed reload initializes no rules");equal(EzXposed.publishedReloads,0,"failed callback cannot complete restoration");
                check(BaseLoad.getHotReloadBlockReason()!=null,"failed new generation recorded");
            }
            case "null_remote" -> {
                Remote r=new Remote(0);r.nulls=1;Entry e=r.entry();boot(e,false);ready(e,false);recovered(r);
            }
            case "concurrent_init" -> {
                Remote r=new Remote(0);Entry e=r.entry();
                var pool=Executors.newFixedThreadPool(8);var tasks=new ArrayList<Future<?>>();
                for(int i=0;i<32;i++)tasks.add(pool.submit(e::initializePreferences));
                for(var task:tasks)task.get();pool.shutdown();
                equal(r.calls.get(),1,"existing lock preserves single successful binding");
                equal(PrefsBridge.getInt("fixture_value",-1),43,"published handle available");
            }
            case "empty_success" -> {
                Remote r=new Remote(0);r.values.clear();Entry e=r.entry();boot(e,false);ready(e,false);
                equal(r.calls.get(),1,"valid empty preferences count as initialized");
                equal(installed,0,"empty allow_hook uses the existing false default");
                check(BaseLoad.getHotReloadBlockReason()==null,"empty preference map is not a transport failure");
                check(EzXposed.dispatcher.getState()==TargetReadyState.SUCCEEDED,"valid empty map accepted");
            }
            case "rule_failure" -> {
                Remote r=new Remote(0);Entry e=r.entry();failRule=true;boot(e,false);ready(e,false);
                equal(installed,1,"first rule installation attempted");
                check(EzXposed.dispatcher.getState()==TargetReadyState.FAILED,"actual dispatcher retains rule failure");
                failRule=false;ready(e,false);
                equal(installed,1,"failed generation must not attempt duplicate rule installation");
                equal(r.calls.get(),1,"already initialized prefs are not rebound");
            }
            case "home_recovered", "home_gate", "home_framework_gate", "home_healthy" -> {
                packageName="com.miui.home";
                boolean healthy=name.equals("home_healthy"),denied=name.equals("home_gate")||name.equals("home_framework_gate");
                Remote r=new Remote(healthy?0:1);
                if(name.equals("home_gate"))r.values.put("prefs_key_allow_hook",false);
                if(name.equals("home_framework_gate")){
                    r.values.put("prefs_key_framework_api_allow_hook",false);
                    r.values.put("prefs_key_framework_check_reason","blocked");
                    r.values.put("prefs_key_framework_check_name","FixtureFramework");
                    r.values.put("prefs_key_framework_check_version","102-test");
                    r.values.put("prefs_key_framework_check_version_code",102L);
                }
                Entry e=r.entry();e.onModuleLoaded(new ModuleParam(false,packageName));ready(e,false);
                equal(r.calls.get(),healthy?1:2,"Home transport initialization");
                equal(com.sevtinge.hyperceiler.libhook.rules.home.other.NativeHomeHooksOS4.recoveries,
                    healthy||denied?0:1,"native recovery only after a real retry and allowed gates");
                equal(installed,denied?0:1,"Home rule respects gates");
            }
            case "home_bound_read_failure" -> {
                packageName="com.miui.home";Remote r=new Remote(0);r.failReads=true;Entry e=r.entry();
                e.onModuleLoaded(new ModuleParam(false,packageName));
                check(PrefsBridge.getSharedPreferences()!=null,"transport already bound before read failure");
                check(!com.sevtinge.hyperceiler.libhook.rules.home.other.NativeHomeHooksOS4.bootstrapReady,
                    "native probe must receive explicit failed bootstrap state despite a non-null handle");
                r.failReads=false;ready(e,false);recovered(r);
                equal(com.sevtinge.hyperceiler.libhook.rules.home.other.NativeHomeHooksOS4.recoveries,1,"failed bootstrap native recovery notified");
            }
            case "framework_changed_name", "framework_changed_version", "framework_changed_code", "framework_missing_reason", "framework_runtime_unavailable", "framework_whitespace_same", "framework_global_disabled", "framework_allowed_no_identity", "framework_missing_checked_fields" -> {
                Remote r=new Remote(0);
                r.values.put("prefs_key_framework_api_allow_hook",false);
                r.values.put("prefs_key_framework_check_reason","SERVICE_ERROR");
                r.values.put("prefs_key_framework_check_name","FixtureFramework");
                r.values.put("prefs_key_framework_check_version","102-test");
                r.values.put("prefs_key_framework_check_version_code",102L);
                int expected=0;
                switch(name){
                    case "framework_changed_name" -> {r.values.put("prefs_key_framework_check_name","old");expected=1;}
                    case "framework_changed_version" -> {r.values.put("prefs_key_framework_check_version","old");expected=1;}
                    case "framework_changed_code" -> {r.values.put("prefs_key_framework_check_version_code",101L);expected=1;}
                    case "framework_missing_reason" -> {r.values.remove("prefs_key_framework_check_reason");r.values.put("prefs_key_framework_check_name","old");}
                    case "framework_runtime_unavailable" -> {r.failFramework=true;r.values.put("prefs_key_framework_check_name","old");}
                    case "framework_whitespace_same" -> {r.values.put("prefs_key_framework_check_name"," FixtureFramework ");r.values.put("prefs_key_framework_check_version"," 102-test ");}
                    case "framework_global_disabled" -> {r.values.put("prefs_key_allow_hook",false);r.values.put("prefs_key_framework_check_name","old");}
                    case "framework_allowed_no_identity" -> {r.values.put("prefs_key_framework_api_allow_hook",true);r.failFramework=true;expected=1;}
                    case "framework_missing_checked_fields" -> {r.values.remove("prefs_key_framework_check_name");r.values.remove("prefs_key_framework_check_version");r.values.remove("prefs_key_framework_check_version_code");expected=1;}
                }
                Entry e=r.entry();boot(e,true);ready(e,true);
                equal(installed,expected,"shared framework decision preserves existing entry semantics");
                check(EzXposed.dispatcher.getState()==TargetReadyState.SUCCEEDED,"allowed/denied policy is a successful initialization decision");
            }
            case "ambient_loaded_reload", "ambient_unloaded_reload", "ambient_rule_failure_reload",
                 "ambient_save_denied_reload", "ambient_save_throws_reload", "ambient_worker_failure_reload",
                 "ambient_other_package_reload", "ambient_system_server_reload" -> {
                boolean other=name.equals("ambient_other_package_reload");
                boolean system=name.equals("ambient_system_server_reload");
                if(other)packageName="com.example.other";
                Remote r=new Remote(0); Entry e=r.entry();boot(e,system);ready(e,system);
                equal(installed,1,"original generation initialized");
                AmbientNativeColorOS4.loaded=name.equals("ambient_loaded_reload") || other || system;
                if(name.equals("ambient_rule_failure_reload"))BaseLoad.recordHookInitializationFailure("fixture",new IllegalStateException());
                if(name.equals("ambient_save_denied_reload"))EzXposed.acceptSave=false;
                if(name.equals("ambient_save_throws_reload"))EzXposed.throwSave=true;
                if(name.equals("ambient_worker_failure_reload"))ThreadPoolManager.shutdownOk=false;
                HotReloadingParam before=new HotReloadingParam(){
                    public android.os.Bundle getExtras(){return null;}
                    public void setSavedInstanceState(Object state){}
                };
                boolean accepted=e.onHotReloading(before);
                if(other||system){
                    check(accepted,"unrelated host retains existing reload path");
                    equal(AmbientNativeColorOS4.pauses,0,"no ambient gate outside SystemUI");
                    equal(AmbientNativeColorOS4.resumes,0,"no unrelated ambient mutation");
                } else if(name.equals("ambient_loaded_reload")) {
                    check(!accepted,"loaded JNI rejects before teardown");
                    equal(AmbientNativeColorOS4.pauses,1,"gate consulted once");
                    equal(AmbientNativeColorOS4.resumes,0,"rejected loaded gate did not pause");
                    equal(EzXposed.savedReloads,0,"no state publication");
                    equal(ThreadPoolManager.shutdowns,0,"no worker shutdown");
                    equal(BaseHook.cleanups,0,"old hooks and owned views untouched");
                    check(BaseLoad.getHotReloadBlockReason()==null,"no fake initialization failure");
                } else if(name.equals("ambient_unloaded_reload")) {
                    check(accepted&&AmbientNativeColorOS4.paused,"unloaded backend stays frozen until old generation leaves");
                    equal(AmbientNativeColorOS4.pauses,1,"gate consulted once");
                    equal(AmbientNativeColorOS4.resumes,0,"successful transition stays frozen");
                    equal(EzXposed.savedReloads,1,"one normal snapshot");
                    equal(ThreadPoolManager.shutdowns,1,"normal shutdown");equal(BaseHook.cleanups,1,"normal cleanup");
                } else {
                    check(!accepted&&!AmbientNativeColorOS4.paused,"failed transition resumes old backend");
                    equal(AmbientNativeColorOS4.pauses,1,"gate consulted once");
                    equal(AmbientNativeColorOS4.resumes,1,"finally resumes exactly once");
                    equal(BaseHook.cleanups,0,"these failures occur before owned view cleanup");
                }
            }
            default -> throw new AssertionError(name);
        }
        System.out.println("PASS "+name);
    }
}
