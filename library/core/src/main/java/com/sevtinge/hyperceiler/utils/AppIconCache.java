/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import me.zhanghai.android.appiconloader.AppIconLoader;

/**
 * 按目标尺寸标准化加载应用图标，并基于 Bitmap 做 LRU 缓存。
 */
public class AppIconCache {

    private static final int DEFAULT_ICON_SIZE_DP = 40;
    private static final float RELOADED_ICON_CONTENT_SCALE = 1.16f;

    private static final LruCache<CacheKey, Bitmap> sCache;
    private static final Map<Integer, AppIconLoader> sLoaders = new ConcurrentHashMap<>();

    private static volatile Boolean sShouldShrink;

    static {
        int maxMemoryKB = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // 使用最大内存的 1/8 作为位图缓存上限，sizeOf 以 KB 计。
        sCache = new LruCache<>(Math.max(1024, maxMemoryKB / 8)) {
            @Override
            protected int sizeOf(@NonNull CacheKey key, @NonNull Bitmap value) {
                return Math.max(1, value.getByteCount() / 1024);
            }
        };
    }

    private AppIconCache() {}

    @Nullable
    public static Drawable getCached(@NonNull Context context, @NonNull String packageName, int sizePx) {
        Context appContext = context.getApplicationContext();
        int resolvedSize = resolveSizePx(appContext, sizePx);
        Bitmap bitmap = sCache.get(new CacheKey(packageName, resolvedSize));
        return bitmap != null ? new BitmapDrawable(appContext.getResources(), bitmap) : null;
    }

    @Nullable
    public static Drawable loadIcon(@NonNull Context context, @NonNull String packageName, int sizePx) {
        Context appContext = context.getApplicationContext();
        try {
            PackageManager pm = appContext.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            Bitmap bitmap = getOrLoadBitmap(appContext, info, resolveSizePx(appContext, sizePx));
            return bitmap != null ? new BitmapDrawable(appContext.getResources(), bitmap) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void loadIconAsync(@NonNull Context context, @NonNull String packageName,
                                     int sizePx, @NonNull IconCallback callback) {
        Context appContext = context.getApplicationContext();
        Drawable cached = getCached(appContext, packageName, sizePx);
        if (cached != null) {
            callback.onIconLoaded(cached);
            return;
        }

        ThreadUtils.getBackgroundExecutor().execute(() -> {
            Drawable icon = loadIcon(appContext, packageName, sizePx);
            ThreadUtils.postOnMainThread(() -> callback.onIconLoaded(icon));
        });
    }

    @Nullable
    private static Bitmap getOrLoadBitmap(@NonNull Context context, @NonNull ApplicationInfo info, int sizePx) {
        CacheKey key = new CacheKey(info.packageName, sizePx);
        Bitmap cached = sCache.get(key);
        if (cached != null) {
            return cached;
        }

        // 同一个包在不同列表里会以不同目标尺寸出现，因此缓存键必须带 size。
        Bitmap bitmap = loadBitmapInternal(context, info, sizePx);
        if (bitmap != null) {
            sCache.put(key, bitmap);
        }
        return bitmap;
    }

    @Nullable
    private static Bitmap loadBitmapInternal(@NonNull Context context, @NonNull ApplicationInfo info, int sizePx) {
        try {
            // 复用 AppIconLoader 的系统图标标准化逻辑，避免小图标在不同容器里观感不一致。
            // 对重新加载出来的应用图标请求更大的源尺寸，再居中绘制到目标画布上，
            // 用更清晰的方式补偿系统安全区带来的视觉偏小问题。
            int loaderSize = Math.max(sizePx, Math.round(sizePx * RELOADED_ICON_CONTENT_SCALE));
            AppIconLoader loader = sLoaders.computeIfAbsent(loaderSize,
                ignored -> new AppIconLoader(loaderSize, shouldShrink(context), context));
            Bitmap bitmap = loader.loadIcon(info, false);
            return bitmap != null ? centerCropToTargetSize(bitmap, sizePx) : null;
        } catch (Throwable ignored) {
            try {
                Drawable fallback = info.loadIcon(context.getPackageManager());
                return drawableToBitmap(fallback, sizePx);
            } catch (Throwable ignoredFallback) {
                return null;
            }
        }
    }

    private static boolean shouldShrink(@NonNull Context context) {
        Boolean cached = sShouldShrink;
        if (cached != null) {
            return cached;
        }
        synchronized (AppIconCache.class) {
            if (sShouldShrink == null) {
                boolean shrink = false;
                try {
                    shrink = context.getApplicationInfo().loadIcon(context.getPackageManager()) instanceof AdaptiveIconDrawable;
                } catch (Throwable ignored) {
                }
                sShouldShrink = shrink;
            }
            return Boolean.TRUE.equals(sShouldShrink);
        }
    }

    private static int resolveSizePx(@NonNull Context context, int sizePx) {
        if (sizePx > 0) {
            return sizePx;
        }
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            DEFAULT_ICON_SIZE_DP,
            context.getResources().getDisplayMetrics()
        ));
    }

    @Nullable
    private static Bitmap drawableToBitmap(@Nullable Drawable drawable, int sizePx) {
        if (drawable == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, sizePx, sizePx);
        drawable.draw(canvas);
        return bitmap;
    }

    @NonNull
    private static Bitmap centerCropToTargetSize(@NonNull Bitmap bitmap, int sizePx) {
        if (bitmap.getWidth() == sizePx && bitmap.getHeight() == sizePx) {
            return bitmap;
        }

        Bitmap output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        float left = (sizePx - bitmap.getWidth()) / 2f;
        float top = (sizePx - bitmap.getHeight()) / 2f;
        canvas.drawBitmap(bitmap, left, top, paint);
        return output;
    }

    public interface IconCallback {
        void onIconLoaded(@Nullable Drawable icon);
    }

    private static final class CacheKey {
        private final String packageName;
        private final int sizePx;

        private CacheKey(@NonNull String packageName, int sizePx) {
            this.packageName = packageName;
            this.sizePx = sizePx;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CacheKey other)) return false;
            return sizePx == other.sizePx && packageName.equals(other.packageName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packageName, sizePx);
        }
    }
}
