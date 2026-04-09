package com.sevtinge.hyperceiler.home;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.LocaleList;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.utils.AppIconCache;
import com.sevtinge.hyperceiler.utils.LanguageHelper;
import com.sevtinge.hyperceiler.utils.ThreadUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IconTitleLoader {

    private static final Map<String, CharSequence> sLabelCache = new ConcurrentHashMap<>();
    private static final AtomicInteger sLabelCacheGeneration = new AtomicInteger();

    public record AppInfo(Drawable icon, CharSequence label) {
    }

    /**
     * 同步查缓存，供首页预加载后的场景直接复用。
     * 图标缓存按包名和尺寸区分，避免不同列表互相串尺寸。
     */
    public static AppInfo getCached(Context context, String pkg, int iconSizePx) {
        CharSequence label = sLabelCache.get(buildLabelCacheKey(pkg, getLocaleTag(context)));
        Drawable icon = AppIconCache.getCached(context, pkg, iconSizePx);
        if (label == null || icon == null) {
            return null;
        }
        return new AppInfo(icon, label);
    }

    public static void load(Context context, String pkg, int iconSizePx, LoadCallback callback) {
        Context labelContext = context;
        Context appContext = context.getApplicationContext();
        Locale locale = getLocaleSnapshot(labelContext);
        String localeTag = locale.toLanguageTag();
        int generation = sLabelCacheGeneration.get();
        AppInfo cached = getCached(labelContext, pkg, iconSizePx);
        if (cached != null) {
            callback.onReady(cached);
            return;
        }

        ThreadUtils.getBackgroundExecutor().execute(() -> {
            try {
                PackageManager pm = labelContext.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                String labelCacheKey = buildLabelCacheKey(pkg, localeTag);
                CharSequence label = sLabelCache.computeIfAbsent(labelCacheKey, ignored -> loadLabel(labelContext, info, pm, locale));
                Drawable icon = AppIconCache.loadIcon(appContext, pkg, iconSizePx);
                if (icon == null) {
                    icon = info.loadIcon(pm);
                }
                AppInfo result = new AppInfo(icon, label);
                ThreadUtils.postOnMainThread(() -> {
                    if (generation == sLabelCacheGeneration.get()) {
                        callback.onReady(result);
                    }
                });
            } catch (Exception ignored) {}
        });
    }

    /**
     * 批量预加载首页会用到的 label 和指定尺寸图标，全部完成后回到主线程。
     * 已缓存的包名会被跳过。
     */
    public static void preloadAll(Context context, List<String> packageNames, int iconSizePx, Runnable onComplete) {
        Context labelContext = context;
        Context appContext = context.getApplicationContext();
        Locale locale = getLocaleSnapshot(labelContext);
        String localeTag = locale.toLanguageTag();
        int generation = sLabelCacheGeneration.get();
        // 过滤出 label 或当前尺寸图标尚未缓存的包名
        List<String> toLoad = packageNames.stream()
            .filter(pkg -> pkg != null && (sLabelCache.get(buildLabelCacheKey(pkg, localeTag)) == null
                || AppIconCache.getCached(appContext, pkg, iconSizePx) == null))
            .toList();

        if (toLoad.isEmpty()) {
            onComplete.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(toLoad.size());
        for (String pkg : toLoad) {
            ThreadUtils.getBackgroundExecutor().execute(() -> {
                try {
                    PackageManager pm = labelContext.getPackageManager();
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    CharSequence label = loadLabel(labelContext, info, pm, locale);
                    if (generation == sLabelCacheGeneration.get()) {
                        sLabelCache.put(buildLabelCacheKey(pkg, localeTag), label);
                    }
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
            ThreadUtils.postOnMainThread(() -> {
                if (generation == sLabelCacheGeneration.get()) {
                    onComplete.run();
                }
            });
        });
    }

    public static void clearLabelCache() {
        sLabelCache.clear();
        sLabelCacheGeneration.incrementAndGet();
    }

    private static String buildLabelCacheKey(String pkg, String localeTag) {
        // 首页标题会被应用 label 覆盖，所以缓存必须按 locale 分桶。
        return pkg + "#" + localeTag;
    }

    private static CharSequence loadLabel(Context context, ApplicationInfo info, PackageManager pm, Locale locale) {
        if (info.nonLocalizedLabel != null) {
            return info.nonLocalizedLabel;
        }
        if (info.labelRes != 0) {
            try {
                Context packageContext = context.createPackageContext(info.packageName, 0);
                Configuration configuration = new Configuration(packageContext.getResources().getConfiguration());
                LocaleList localeList = new LocaleList(locale);
                configuration.setLocales(localeList);
                configuration.setLocale(locale);
                // 目标包资源要套用当前页面语言，否则会退回系统语言标题。
                Context localizedPackageContext = packageContext.createConfigurationContext(configuration);
                CharSequence localized = localizedPackageContext.getText(info.labelRes);
                if (!TextUtils.isEmpty(localized)) {
                    return localized;
                }
            } catch (Exception ignored) {
            }
        }

        CharSequence fallback = info.loadLabel(pm);
        return TextUtils.isEmpty(fallback) ? info.packageName : fallback;
    }

    private static Locale getLocaleSnapshot(Context context) {
        return LanguageHelper.getCurrentLocale(context);
    }

    private static String getLocaleTag(Context context) {
        return getLocaleSnapshot(context).toLanguageTag();
    }

    public interface LoadCallback {
        void onReady(AppInfo info);
    }
}
