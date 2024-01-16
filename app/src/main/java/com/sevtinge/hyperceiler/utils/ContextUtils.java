package com.sevtinge.hyperceiler.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint({"PrivateApi", "SoonBlockedPrivateApi", "DiscouragedPrivateApi"})
public class ContextUtils {
    private static final String TAG = "[HyperCeiler]";
    // 尝试全部
    public static final int FLAG_ALL = 0;
    // 仅获取当前应用
    public static final int FLAG_CURRENT_APP = 1;
    // 获取 Android 系统
    public static final int FlAG_ONLY_ANDROID = 2;

    public static Context getContext(int flag) {
        try {
            return invokeMethod(flag);
        } catch (Throwable e) {
            Log.e(TAG, "getContext: ", e);
            return null;
        }
    }

    /**
     * 循环获取当前应用的 Context 为了防止过早获取导致的 null.
     * 使用方法:
     * <pre> {@code
     * handler = new Handler();
     * ContextUtils.getWaitContext(new ContextUtils.IContext() {
     *   @Override
     *   public void hadContext(Context context) {
     *      handler.post(new Runnable() {
     *        @Override
     *        public void run() {
     *          ToastHelper.makeText(context, "getContext");
     *        }
     *      });
     *   }
     * });
     * }
     * 当然 Handler 是可选项, 适用于 Toast 显示等场景。
     * @param iContext
     * @author 焕晨HChen
     */
    public static void getWaitContext(IContext iContext) {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        executorService.submit(() -> {
            Context context = getContext(ContextUtils.FLAG_CURRENT_APP);
            if (context == null) {
                int count = 0;
                while (true) {
                    context = getContext(ContextUtils.FLAG_CURRENT_APP);
                    // AndroidLogUtils.LogI(TAG, "getWaitContext: " + context);
                    if (context != null || count > 5) {
                        break;
                    }
                    try {
                        Thread.sleep(500);
                    } catch (Throwable throwable) {
                        AndroidLogUtils.LogE(TAG, "getWaitContext E: ", throwable);
                    }
                    // 防止死循环
                    count = count + 1;
                }
                // context 可能为 null 请注意判断
                iContext.hadContext(context);
            }
        });
    }

    public interface IContext {
        void hadContext(Context context);
    }

    private static Context invokeMethod(int flag) throws Throwable {
        Context context;
        Class<?> clz = Class.forName("android.app.ActivityThread");
        switch (flag) {
            case 0 -> {
                if ((context = currentApp(clz)) == null) {
                    context = android(clz);
                }
            }
            case 1 -> {
                context = currentApp(clz);
            }
            case 2 -> {
                context = android(clz);
            }
            default -> {
                throw new Throwable("Unexpected flag");
            }
        }
        if (context == null) throw new Throwable("Context is null");
        return context;
    }

    private static Context currentApp(Class<?> clz) throws Throwable {
        // 获取当前界面应用 Context
        Method currentApplication = clz.getDeclaredMethod("currentApplication");
        currentApplication.setAccessible(true);
        return (Application) currentApplication.invoke(null);
    }

    private static Context android(Class<?> clz) throws Throwable {
        // 获取 Android
        Context context;
        Method currentActivityThread = clz.getDeclaredMethod("currentActivityThread");
        currentActivityThread.setAccessible(true);
        Object o = currentActivityThread.invoke(null);
        Method getSystemContext = clz.getDeclaredMethod("getSystemContext");
        getSystemContext.setAccessible(true);
        context = (Context) getSystemContext.invoke(o);
        if (context == null) {
            Method getSystemUiContext = clz.getDeclaredMethod("getSystemUiContext");
            getSystemUiContext.setAccessible(true);
            context = (Context) getSystemContext.invoke(o);
        }
        return context;
    }

}
