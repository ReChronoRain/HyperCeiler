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
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

/**
 * 应用图标异步加载与 LRU 缓存。
 */
public class AppIconCache {

    private static final LruCache<String, Drawable> sCache;

    static {
        int maxMemoryKB = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // 用最大内存的 1/8 作为图标缓存
        sCache = new LruCache<>(maxMemoryKB / 8);
    }

    private AppIconCache() {}

    /**
     * 同步获取缓存中的图标，无则返回 null。
     */
    @Nullable
    public static Drawable getCached(@NonNull String packageName) {
        return sCache.get(packageName);
    }

    /**
     * 异步加载图标，加载完成后在主线程回调。
     * 如果缓存命中则立即回调。
     */
    public static void loadIconAsync(@NonNull Context context, @NonNull String packageName,
                                     @NonNull IconCallback callback) {
        Drawable cached = sCache.get(packageName);
        if (cached != null) {
            callback.onIconLoaded(cached);
            return;
        }

        ThreadUtils.getBackgroundExecutor().execute(() -> {
            try {
                PackageManager pm = context.getApplicationContext().getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                Drawable icon = info.loadIcon(pm);
                if (icon != null) {
                    sCache.put(packageName, icon);
                }
                ThreadUtils.postOnMainThread(() -> callback.onIconLoaded(icon));
            } catch (Exception e) {
                ThreadUtils.postOnMainThread(() -> callback.onIconLoaded(null));
            }
        });
    }

    public interface IconCallback {
        void onIconLoaded(@Nullable Drawable icon);
    }
}
