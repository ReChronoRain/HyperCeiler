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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.List;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

/**
 * DexKit 工具类 — 薄 Java 静态门面
 * <p>
 * 所有实际工作委托给 {@link DexKitCacheManager}，后者基于 DexKit 2.2.0
 * 的 {@code DexKitCacheBridge} + {@code RecyclableBridge} 实现
 * 缓存与原生桥生命周期管理。
 *
 * @author 焕晨HChen
 * @co-author Ling Qiqi
 */
public class DexKit {

    private DexKit() {}

    /**
     * 准备 DexKit 会话
     */
    public static void ready(PackageReadyParam param, String tag) {
        DexKitCacheManager.INSTANCE.init(param, tag);
    }

    /**
     * 查找单个成员 (Method / Field / Class)
     */
    public static <T> T findMember(@NonNull String key, IDexKit iDexKit) {
        return DexKitCacheManager.INSTANCE.findMember(key, iDexKit);
    }

    /**
     * 查找成员列表
     */
    public static <T> List<T> findMemberList(@NonNull String key, IDexKitList iDexKitList) {
        return DexKitCacheManager.INSTANCE.findMemberList(key, iDexKitList);
    }

    /**
     * 关闭 DexKit 会话，释放原生桥并刷新缓存到磁盘
     */
    public static void close() {
        DexKitCacheManager.INSTANCE.releaseBridge();
    }

    /**
     * 删除所有缓存文件（从设置界面调用）
     *
     * @param scopeList 作用域包名列表，通常来自 ScopeManager.getScopeSync()
     */
    public static void deleteAllCache(Context context, Collection<String> scopeList) {
        if (context == null) return;
        DexKitCacheManager.INSTANCE.deleteAllCacheFiles(context, scopeList);
    }
}
