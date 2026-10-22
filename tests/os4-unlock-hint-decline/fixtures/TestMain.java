import java.lang.reflect.*;
import java.util.*;
import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed;
import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.HideLockScreenHint;

public class TestMain {
    static final List<Method> hooks = new ArrayList<>();
    static int checks;
    static Object proxy(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(TestMain.class.getClassLoader(), new Class<?>[]{type}, handler);
    }
    static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks++;
    }
    public static void main(String[] args) {
        XposedInterface api = (XposedInterface) proxy(XposedInterface.class, (p,m,a) -> {
            if (m.getName().equals("hook")) {
                Method target = (Method)a[0];
                return proxy(XposedInterface.HookBuilder.class, (b,bm,ba) -> {
                    if (bm.getName().equals("intercept")) {
                        hooks.add(target);
                        return proxy(XposedInterface.HookHandle.class, (h,hm,ha) -> null);
                    }
                    return b;
                });
            }
            if (m.getReturnType() == int.class) return 102;
            if (m.getReturnType() == boolean.class) return false;
            return null;
        });
        EzXposed.setBase$hook_xposed_102(api);
        EzXposed.setHotReloadEnabled(false);
        for (float version : new float[]{4f, 4.1f, 5f}) {
            DeviceHelper.System.version = version;
            hooks.clear(); XposedLog.warnings.clear();
            HideLockScreenHint.INSTANCE.init();
            check(hooks.isEmpty(), "No fingerprint hook on OS " + version);
            check(XposedLog.warnings.size() == 1 && XposedLog.warnings.get(0).contains("not adapted"),
                "Decline stays explicit on OS " + version);
        }
        DeviceHelper.System.version = 3f;
        hooks.clear(); XposedLog.warnings.clear();
        HideLockScreenHint.INSTANCE.init();
        check(hooks.size() == 1 && hooks.get(0).getName().equals("fingerprintFeedback"),
            "Legacy static shape selection unchanged below OS4");
        check(XposedLog.warnings.isEmpty(), "No OS4 warning on OS3");
        DeviceHelper.System.version = 4f;
        hooks.clear(); XposedLog.warnings.clear();
        HideLockScreenHint.INSTANCE.init();
        check(hooks.isEmpty(), "OS4 remains declined after legacy class lazy value initialized");
        System.out.println("PASS " + checks);
    }
}
