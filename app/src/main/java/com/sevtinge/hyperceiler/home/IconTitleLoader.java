package com.sevtinge.hyperceiler.home;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IconTitleLoader {

    private static final Map<String, AppInfo> sCache = new ConcurrentHashMap<>();
    private static final ExecutorService sExecutor = Executors.newFixedThreadPool(4);
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    public static class AppInfo {
        public final Drawable icon;
        public final CharSequence label;
        public AppInfo(Drawable icon, CharSequence label) {
            this.icon = icon;
            this.label = label;
        }
    }

    public static void load(Context context, String pkg, LoadCallback callback) {
        // 1. 命中缓存直接返回
        if (sCache.containsKey(pkg)) {
            callback.onReady(sCache.get(pkg));
            return;
        }

        // 2. 异步加载
        sExecutor.execute(() -> {
            try {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                AppInfo result = new AppInfo(info.loadIcon(pm), info.loadLabel(pm));

                sCache.put(pkg, result); // 存入缓存
                sHandler.post(() -> callback.onReady(result));
            } catch (Exception ignored) {}
        });
    }

    public interface LoadCallback {
        void onReady(AppInfo info);
    }
}
