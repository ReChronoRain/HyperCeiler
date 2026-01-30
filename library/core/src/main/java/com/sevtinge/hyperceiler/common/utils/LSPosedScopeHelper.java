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
package com.sevtinge.hyperceiler.common.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.libxposed.service.XposedService;

public class LSPosedScopeHelper {

    private static final String TAG = "LSPosedScopeHelper";
    private static final String SYSTEM_PKG_ANDROID = "android";
    private static final String SYSTEM_PKG_SYSTEM = "system";
    private static final List<String> SYSTEM_PACKAGES = Arrays.asList(SYSTEM_PKG_ANDROID, SYSTEM_PKG_SYSTEM);

    private static volatile boolean isInitScopeGet = false;
    private static volatile boolean isScopeGetFailed = false;

    public static ArrayList<String> mNoScoped = new ArrayList<>();
    public static ArrayList<String> mUninstallApp = new ArrayList<>();
    public static ArrayList<String> mDisableOrHiddenApp = new ArrayList<>();

    public static List<String> mScope = new ArrayList<>();
    public static List<String> mNotInSelectedScope = new ArrayList<>();

    /**
     * 初始化作用域，通过 ScopeManager 获取
     */
    public static void init() {
        getScope();
    }

    public static boolean isInSelectedScope(Context context, String label, String pkg, String key, SharedPreferences sp) {
        String normLabel = label == null ? "" : label.trim();
        String normPkg = pkg == null ? null : pkg.trim();
        String normPkgForEntry = normPkg == null ? "" : normPkg.toLowerCase();
        String entry = " - " + normLabel + (normPkg != null ? " (" + normPkgForEntry + ")" : "");

        // system 特例：不检查卸载/禁用/隐藏，直接检查 system 或 android 是否在作用域内
        if (isSystemPackage(pkg)) {
            return checkSystemScope(normPkgForEntry, entry);
        }

        // 普通应用：先检查卸载/禁用/隐藏状态
        if (isUninstall(context, pkg) || isDisable(context, pkg) || isHidden(context, pkg)) {
            if (!mUninstallApp.contains(entry)) {
                mUninstallApp.add(entry);
            }
            return false;
        }

        // 检查是否被HyperCeiler 隐藏
        if (isHiddenByHyperceiler(key, sp)) {
            if (!mDisableOrHiddenApp.contains(entry)) {
                mDisableOrHiddenApp.add(entry);
            }
            return false;
        }

        // 最后检查作用域
        return checkScopeOnly(pkg, normPkgForEntry, entry);
    }

    /**
     * 检查 system 框架是否在作用域内
     * mScope 里可能是 "system" 或 "android"，只要有一个在就算在作用域内
     */
    private static boolean checkSystemScope(String normPkgForEntry, String entry) {
        if (!isInitScopeGet || isScopeGetFailed) {
            return true; // 未初始化完成时默认放行
        }

        // 检查 mScope 是否包含 system 或 android
        boolean inScope = mScope.contains(SYSTEM_PKG_SYSTEM) || mScope.contains(SYSTEM_PKG_ANDROID);

        if (!inScope) {
            if (!mNotInSelectedScope.contains(normPkgForEntry)) {
                mNotInSelectedScope.add(normPkgForEntry);
            }
            if (!mNoScoped.contains(entry)) {
                mNoScoped.add(entry);
            }
            return false;
        }
        return true;
    }

    /**
     * 仅检查是否在作用域内（用于普通应用）
     */
    private static boolean checkScopeOnly(String pkg, String normPkgForEntry, String entry) {
        if (pkg != null && !mScope.contains(pkg) && isInitScopeGet && !isScopeGetFailed) {
            if (!mNotInSelectedScope.contains(normPkgForEntry)) {
                mNotInSelectedScope.add(normPkgForEntry);
            }
            if (!mNoScoped.contains(entry)) {
                mNoScoped.add(entry);
            }
            return false;
        }
        return true;
    }

    /**
     * 判断是否为 system 特例包名
     */
    private static boolean isSystemPackage(String pkg) {
        return pkg != null && SYSTEM_PACKAGES.contains(pkg);
    }

    /**
     * 检查 Service 是否可用
     */
    public static boolean isServiceAvailable() {
        try {
            return ScopeManager.getService() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查作用域是否已初始化完成
     */
    public static boolean isScopeReady() {
        return isInitScopeGet && !isScopeGetFailed;
    }

    /**
     * 检查 system 框架是否在作用域内
     */
    public static boolean isSystemInScope() {
        if (!isInitScopeGet || isScopeGetFailed) {
            return false;
        }
        return mScope.contains(SYSTEM_PKG_SYSTEM) || mScope.contains(SYSTEM_PKG_ANDROID);
    }

    /**
     * 重新加载作用域
     */
    public static void reloadScope() {
        isInitScopeGet = false;
        isScopeGetFailed = false;
        mScope.clear();
        mNoScoped.clear();
        mNotInSelectedScope.clear();
        mUninstallApp.clear();
        mDisableOrHiddenApp.clear();
        getScope();
    }

    private static boolean isUninstall(Context context, String pkg) {
        return pkg != null && PackagesUtils.isUninstall(context, pkg);
    }

    private static boolean isDisable(Context context, String pkg) {
        return pkg != null && PackagesUtils.isDisable(context, pkg);
    }

    private static boolean isHidden(Context context, String pkg) {
        return pkg != null && PackagesUtils.isHidden(context, pkg);
    }

    private static boolean isHiddenByHyperceiler(String key, SharedPreferences sp) {
        return !sp.getBoolean(key + "_state", true);
    }

    /**
     * 通过 ScopeManager 获取作用域列表
     */
    private static void getScope() {
        if (isInitScopeGet) return;

        try {
            XposedService service = ScopeManager.getService();
            if (service == null) {
                AndroidLog.w(TAG, "XposedService not available, skip get scope.");
                isScopeGetFailed = true;
                isInitScopeGet = true;
                return;
            }

            List<String> scope = service.getScope();
            if (scope != null) {
                mScope = new ArrayList<>(scope);
                AndroidLog.d(TAG, "Scope loaded successfully, count: " + mScope.size());
            } else {
                AndroidLog.w(TAG, "getScope returned null");
                isScopeGetFailed = true;
            }
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to get scope from XposedService", e);
            isScopeGetFailed = true;
        }

        isInitScopeGet = true;
    }
}
