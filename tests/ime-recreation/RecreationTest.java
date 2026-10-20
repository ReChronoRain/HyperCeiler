import com.sevtinge.hyperceiler.common.log.ModuleLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.app.Others.VariousThirdApps;
import com.sevtinge.hyperceiler.libhook.appbase.input.MiuiImeSupport;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import java.util.Set;

public class RecreationTest {
    // The stock manager resets this cache during onDestroy, then reuses its classloader.
    public static class Manager {
        static int sIsImeSupport = -1;
        static void onDestroy() { sIsImeSupport = -1; }
        static boolean isImeSupport() { sIsImeSupport = 0; return false; }
        static boolean addMiuiBottomView() { return sIsImeSupport == 1; }
    }
    public static class OlderManager {
        static int sIsImeSupport = 0;
        static void addMiuiBottomView() {}
    }
    public static class PredicateOnly {
        static boolean isImeSupport() { return false; }
    }
    static int checks;
    static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    static HookParam before(Class<?> owner, String name) {
        var param = new HookParam();
        BaseHook.hooks.get(owner.getName()+"."+name).before(param);
        return param;
    }
    static void route(String style, boolean selected) {
        BaseLoad.loaded.clear(); PrefsBridge.values.clear(); AppsTool.queries=0;
        if (style != null) PrefsBridge.values.put("various_unlock_ime_style", style);
        PrefsBridge.values.put("various_unlock_ime_apps", selected ? Set.of("test.ime") : Set.of());
        new VariousThirdApps().onPackageLoaded();
    }
    public static void main(String[] args) {
        MiuiImeSupport.install(Manager.class);
        check(Manager.addMiuiBottomView());
        for (int i=0;i<100;i++) {
            Manager.onDestroy();
            check(Boolean.TRUE.equals(before(Manager.class,"isImeSupport").result));
            before(Manager.class,"addMiuiBottomView");
            check(Manager.addMiuiBottomView());
        }
        check(ModuleLog.logs.stream().filter(s->s.contains("restored support")).count()==1);
        MiuiImeSupport.install(OlderManager.class);
        OlderManager.sIsImeSupport=-1;
        before(OlderManager.class,"addMiuiBottomView");
        check(OlderManager.sIsImeSupport==1);
        MiuiImeSupport.install(PredicateOnly.class);
        check(Boolean.TRUE.equals(before(PredicateOnly.class,"isImeSupport").result));
        BaseHook.cleanups.forEach(Runnable::run);
        check(Manager.sIsImeSupport==-1 && OlderManager.sIsImeSupport==0);
        route("1",true);
        check(BaseLoad.loaded.contains("UnlockIme") && AppsTool.queries==0);
        route("2",true);
        check(BaseLoad.loaded.contains("MiAospIme") && !BaseLoad.loaded.contains("UnlockIme") && AppsTool.queries==0);
        route("0",true);
        check(!BaseLoad.loaded.contains("UnlockIme") && !BaseLoad.loaded.contains("MiAospIme"));
        route("1",false);
        check(!BaseLoad.loaded.contains("InputMethodClassLoaderDispatcher"));
        route(null,true);
        PrefsBridge.values.put("various_unlock_ime",true);
        new VariousThirdApps().onPackageLoaded();
        check(BaseLoad.loaded.contains("UnlockIme"));
        System.out.println("IME recreation: " + checks + " checks passed (100 service resets, inlined cache path, old predicate/field variants, cleanup, selected cold-start routing, disabled/unselected/legacy)");
    }
}
