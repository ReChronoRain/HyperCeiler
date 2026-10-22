package com.sevtinge.hyperceiler.libhook.base;

import android.content.BroadcastReceiver;
import android.content.Context;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseHook {
    public final String TAG = getClass().getSimpleName();

    public static final List<String> hookTargets = new ArrayList<>();
    public static final List<IMethodHook> installedHooks = new ArrayList<>();
    private static final Map<String, Object> runtimeState = new HashMap<>();
    private static final List<Runnable> cleanups = new ArrayList<>();

    public abstract void init();

    public String getPackageName() {
        return "com.android.systemui";
    }

    public static void hookAllMethods(String className, String methodName, IMethodHook callback) {
        hookTargets.add(className + "#" + methodName);
        installedHooks.add(callback);
    }

    public static void putHotReloadRuntimeState(String key, Object value) {
        runtimeState.put(key, value);
    }

    public static <T> T getHotReloadRuntimeState(String key, Class<T> type) {
        Object value = runtimeState.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public static void registerHotReloadCleanup(Runnable cleanup) {
        cleanups.add(cleanup);
    }

    public static void registerReceiverHotReloadCleanup(Context context, BroadcastReceiver receiver) {
        // 真机实现会在热重载时注销 receiver；测试保留宿主侧的 receiver 以便复现旧版本下的双回调。
    }

    /** 模拟 BaseHook.prepareHotReload()：运行并清空热重载清理任务。 */
    public static void runHotReloadCleanups() {
        List<Runnable> snapshot = new ArrayList<>(cleanups);
        cleanups.clear();
        runtimeState.clear();
        for (Runnable cleanup : snapshot) {
            cleanup.run();
        }
    }

    /** 模拟被 hook 的方法执行：参数与 thisObject 都是同一个状态栏 view。 */
    public static void driveHooks(Object statusBarView) {
        HookParam param = new HookParam(statusBarView, new Object[]{statusBarView});
        for (IMethodHook hook : installedHooks) {
            hook.after(param);
        }
    }
}
