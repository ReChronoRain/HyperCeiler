package com.sevtinge.hyperceiler.libhook.base;
import java.lang.reflect.*;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import com.android.systemui.statusbar.policy.NetworkSpeedController;
public abstract class BaseHook {
    public static boolean legacy;
    public static IMethodHook registered;
    public static String hookedName;
    public static String failedField;
    public abstract void init();
    public static Class<?> findClass(String name) {
        if (name.equals("com.android.systemui.statusbar.policy.NetworkSpeedController"))
            return legacy ? LegacyController.class : NetworkSpeedController.class;
        // Only these deterministic fixture classes may be selected by a hook plan.
        return switch (name) {
            case "com.android.systemui.statusbar.policy.NetworkSpeedController$CombinedHandler" ->
                NetworkSpeedController.CombinedHandler.class;
            case "com.android.systemui.statusbar.policy.NetworkSpeedController$SeparateUiHandler" ->
                NetworkSpeedController.SeparateUiHandler.class;
            default -> throw new IllegalArgumentException("Unknown fixture class: " + name);
        };
    }
    public static void findAndHookMethod(String cls, String name, Object... parameters) {
        Class<?>[] types = new Class<?>[parameters.length-1];
        for (int i=0; i<types.length; i++) types[i] = (Class<?>) parameters[i];
        try { findClass(cls).getDeclaredMethod(name, types); } catch (Exception e) { throw new IllegalStateException(e); }
        registered = (IMethodHook) parameters[parameters.length-1];
        hookedName = name;
    }
    public static Object getObjectField(Object target, String name) {
        if (name.equals(failedField)) throw new IllegalStateException("injected field failure");
        try { return target.getClass().getField(name).get(target); } catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static boolean getBooleanField(Object target, String name) { return (Boolean)getObjectField(target,name); }
    public static Object callMethod(Object target, String name, Object... args) {
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount()==args.length) {
                try { return m.invoke(target,args); } catch (Exception e) { throw new IllegalStateException(e); }
            }
        }
        throw new AssertionError(name);
    }
    public static class LegacyController {
        public void postUpdateNetworkSpeedDelay(long delay) { /* Only the hooked signature is used. */ }
    }
}
