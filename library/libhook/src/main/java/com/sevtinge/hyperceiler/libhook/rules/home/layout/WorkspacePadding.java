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
package com.sevtinge.hyperceiler.libhook.rules.home.layout;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getPackageVersionCode;

import android.content.Context;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class WorkspacePadding extends HomeBaseHookNew {

    Context mContext;
    Class<?> mDeviceConfig;

    @Override
    public void initBase() {
        // The workspace padding getters live on DeviceConfig (old) on some launcher
        // versions and on DeviceConfigs (new) on others. The original code picked the
        // class with `versionCode < 600000000 ? NEW : OLD`, which is inverted with respect
        // to how every other home rule maps versions (@Version(min = 600000000) -> NEW).
        // On a HyperOS 3.3 launcher (e.g. 750062372) that resolved the old class, where
        // getWorkspaceCellPadding* no longer exists, so initBase() aborted with
        // MemberNotFoundException.
        // Rather than relying on a version number at all, pick whichever class actually
        // declares the getters. That keeps older launchers on exactly the class they
        // already worked with, whichever one that is.
        mDeviceConfig = resolveDeviceConfigClass();

        // Capture a Context for dp2px. The signature differs between versions:
        //   old class: Init(Context, boolean) / Init(Context, int, boolean)
        //   new class: init(Context, boolean)  (lower-case first letter)
        // So hook every overload of either name and take the first Context argument.
        IMethodHook captureContext = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                for (Object arg : param.getArgs()) {
                    if (arg instanceof Context) {
                        mContext = (Context) arg;
                        break;
                    }
                }
            }
        };
        hookAllMethods(mDeviceConfig, "Init", captureContext);
        hookAllMethods(mDeviceConfig, "init", captureContext);

        if (PrefsBridge.getBoolean("home_layout_workspace_padding_bottom_enable")) {
            findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingBottom", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    int dp = PrefsBridge.getInt("home_layout_workspace_padding_bottom", 0);
                    // Init/init does not necessarily run before the getter, so mContext
                    // may still be null here
                    param.setResult(mContext != null
                        ? DisplayUtils.dp2px(mContext, dp)
                        : DisplayUtils.dp2px(dp));
                }
            });
        }

        if (PrefsBridge.getBoolean("home_layout_workspace_padding_top_enable")) {
            try {
                // 新版本桌面，先标记，后续再做进一步修改
                findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingTop", Context.class, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.setResult(DisplayUtils.dp2px(PrefsBridge.getInt("home_layout_workspace_padding_top", 0)));
                    }
                });
            } catch (Throwable t) {
                findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingTop", new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.setResult(DisplayUtils.dp2px(PrefsBridge.getInt("home_layout_workspace_padding_top", 0)));
                    }
                });
            }
        }

        if (PrefsBridge.getBoolean("home_layout_workspace_padding_horizontal_enable")) {
            XposedLog.d(TAG, getPackageName(), "===============home_layout_workspace_padding_horizontal: " + PrefsBridge.getInt("home_layout_workspace_padding_horizontal", 0));
            findAndHookMethod(mDeviceConfig, "getWorkspaceCellPaddingSide", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    param.setResult(DisplayUtils.dp2px(PrefsBridge.getInt("home_layout_workspace_padding_horizontal", 0)));
                }
            });
        }
    }

    /**
     * Picks the DeviceConfig class that actually declares the workspace padding getters.
     *
     * Falls back to the version-based choice used elsewhere in the home rules
     * (>= 600000000 -> DeviceConfigs) if neither class exposes them, so behaviour stays
     * defined even on a launcher this was never tested against.
     */
    private Class<?> resolveDeviceConfigClass() {
        for (String name : new String[]{DEVICE_CONFIG_NEW, DEVICE_CONFIG_OLD}) {
            Class<?> clazz = findClassIfExists(name);
            if (clazz != null && declaresPaddingGetter(clazz)) {
                return clazz;
            }
        }
        return findClassIfExists(getPackageVersionCode(getLpparam()) >= 600000000
            ? DEVICE_CONFIG_NEW : DEVICE_CONFIG_OLD);
    }

    private boolean declaresPaddingGetter(Class<?> clazz) {
        for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
            String name = method.getName();
            if (name.equals("getWorkspaceCellPaddingBottom")
                || name.equals("getWorkspaceCellPaddingTop")
                || name.equals("getWorkspaceCellPaddingSide")) {
                return true;
            }
        }
        return false;
    }
}
