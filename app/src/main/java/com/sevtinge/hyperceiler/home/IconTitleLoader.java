package com.sevtinge.hyperceiler.home;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.sevtinge.hyperceiler.utils.AppIconCache;
import com.sevtinge.hyperceiler.utils.ThreadUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IconTitleLoader {

    private static final Map<String, CharSequence> sLabelCache = new ConcurrentHashMap<>();

    public record AppInfo(Drawable icon, CharSequence label) {
    }

    /**
     * 同步查缓存，供首页预加载后的场景直接复用。
     * 图标缓存按包名和尺寸区分，避免不同列表互相串尺寸。
     */
    public static AppInfo getCached(Context context, String pkg, int iconSizePx) {
        CharSequence label = sLabelCache.get(pkg);
        Drawable icon = AppIconCache.getCached(context, pkg, iconSizePx);
        if (label == null || icon == null) {
            return null;
        }
        return new AppInfo(icon, label);
    }

    public static void load(Context context, String pkg, int iconSizePx, LoadCallback callback) {
        Context appContext = context.getApplicationContext();
        AppInfo cached = getCached(appContext, pkg, iconSizePx);
        if (cached != null) {
            callback.onReady(cached);
            return;
        }

        ThreadUtils.getBackgroundExecutor().execute(() -> {
            try {
                PackageManager pm = appContext.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                CharSequence label = sLabelCache.computeIfAbsent(pkg, ignored -> info.loadLabel(pm));
                Drawable icon = AppIconCache.loadIcon(appContext, pkg, iconSizePx);
                if (icon == null) {
                    icon = info.loadIcon(pm);
                }
                AppInfo result = new AppInfo(icon, label);
                ThreadUtils.postOnMainThread(() -> callback.onReady(result));
            } catch (Exception ignored) {}
        });
    }

    /**
     * 批量预加载首页会用到的 label 和指定尺寸图标，全部完成后回到主线程。
     * 已缓存的包名会被跳过。
     */
    public static void preloadAll(Context context, List<String> packageNames, int iconSizePx, Runnable onComplete) {
        Context appContext = context.getApplicationContext();
        // 过滤出 label 或当前尺寸图标尚未缓存的包名
        List<String> toLoad = packageNames.stream()
            .filter(pkg -> pkg != null && (sLabelCache.get(pkg) == null || AppIconCache.getCached(appContext, pkg, iconSizePx) == null))
            .toList();

        if (toLoad.isEmpty()) {
            onComplete.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(toLoad.size());
        for (String pkg : toLoad) {
            ThreadUtils.getBackgroundExecutor().execute(() -> {
                try {
                    PackageManager pm = appContext.getPackageManager();
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    sLabelCache.put(pkg, info.loadLabel(pm));
                    AppIconCache.loadIcon(appContext, pkg, iconSizePx);
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
