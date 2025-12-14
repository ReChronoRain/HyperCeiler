/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.model.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.LruCache;

import java.util.List;

/**
 * 单例模式 + LruCache 管理应用信息
 */
public class AppInfoCache {

    // 上下文（使用Application Context避免内存泄漏）
    private final Context mContext;
    // LruCache缓存（key：包名，value：ApplicationInfo）
    private final LruCache<String, ApplicationInfo> mAppInfoCache;
    // 单例实例（volatile保证多线程可见性）
    private static volatile AppInfoCache sInstance;

    // 私有构造方法，初始化缓存
    private AppInfoCache(Context context) {
        mContext = context.getApplicationContext();
        // 计算缓存大小：默认最多缓存50个应用信息（可根据需求调整）
        int maxSize = 500;
        mAppInfoCache = new LruCache<>(maxSize) {
            @Override
            protected int sizeOf(String key, ApplicationInfo value) {
                // 每个条目占1个单位（也可根据实际数据大小计算，如序列化后的字节数）
                return 1;
            }
        };
    }

    /**
     * 获取单例实例（线程安全）
     */
    public static AppInfoCache getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AppInfoCache.class) {
                if (sInstance == null) {
                    sInstance = new AppInfoCache(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * 初始化所有应用信息并缓存（建议在子线程调用，避免阻塞主线程）
     */
    public void initAllAppInfos() {
        PackageManager pm = mContext.getPackageManager();
        // 获取所有已安装应用
        List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        // 存入缓存
        for (ApplicationInfo appInfo : allApps) {
            mAppInfoCache.put(appInfo.packageName, appInfo);
        }
    }

    /**
     * 根据包名获取应用信息（优先从缓存读取）
     */
    public ApplicationInfo getAppInfo(String packageName) {
        // 先查缓存
        ApplicationInfo appInfo = mAppInfoCache.get(packageName);
        if (appInfo == null) {
            // 缓存未命中，直接查询并加入缓存
            try {
                appInfo = mContext.getPackageManager().getApplicationInfo(packageName, 0);
                mAppInfoCache.put(packageName, appInfo);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return appInfo;
    }

    /**
     * 获取所有缓存的应用信息
     */
    public List<ApplicationInfo> getAllCachedAppInfos() {
        return mAppInfoCache.snapshot().values().stream().toList();
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        mAppInfoCache.evictAll();
    }
}
