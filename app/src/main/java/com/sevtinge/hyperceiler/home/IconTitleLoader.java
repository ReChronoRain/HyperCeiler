package com.sevtinge.hyperceiler.home;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.sevtinge.hyperceiler.utils.ThreadUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IconTitleLoader {

    private static final Map<String, AppInfo> sCache = new ConcurrentHashMap<>();

    public static class AppInfo {
        public final Drawable icon;
        public final CharSequence label;
        public AppInfo(Drawable icon, CharSequence label) {
            this.icon = icon;
            this.label = label;
        }
    }

    public static void load(Context context, String pkg, LoadCallback callback) {
        if (sCache.containsKey(pkg)) {
            callback.onReady(sCache.get(pkg));
            return;
        }

        ThreadUtils.getBackgroundExecutor().execute(() -> {
            try {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                AppInfo result = new AppInfo(info.loadIcon(pm), info.loadLabel(pm));
                sCache.put(pkg, result);
                ThreadUtils.postOnMainThread(() -> callback.onReady(result));
            } catch (Exception ignored) {}
        });
    }

    /**
     * 批量预加载图标，全部完成后在主线程回调。
     * 已缓存的包名会被跳过。
     */
    public static void preloadAll(Context context, List<String> packageNames, Runnable onComplete) {
        // 过滤出未缓存的包名
        List<String> toLoad = packageNames.stream()
            .filter(pkg -> pkg != null && !sCache.containsKey(pkg))
            .toList();

        if (toLoad.isEmpty()) {
            onComplete.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(toLoad.size());
        for (String pkg : toLoad) {
            ThreadUtils.getBackgroundExecutor().execute(() -> {
                try {
                    PackageManager pm = context.getPackageManager();
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    sCache.put(pkg, new AppInfo(info.loadIcon(pm), info.loadLabel(pm)));
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        // 在后台等待全部完成，然后回到主线程
        ThreadUtils.getBackgroundExecutor().execute(() -> {
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            ThreadUtils.postOnMainThread(onComplete);
        });
    }

    public interface LoadCallback {
        void onReady(AppInfo info);
    }
}
