/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */
package com.sevtinge.hyperceiler.libhook.rules.packageinstaller;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.common.log.XposedLog;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

/** Disables the OS3 ICP/unregistered-app warning branch in MIUI installer. */
public class DisableRegistrationCheck extends BaseHook {
    @Override
    public void init() {
        XposedLog.i(TAG, getPackageName(), "DisableRegistrationCheck init");
        Class<?> cloudParams = findClassIfExists("com.miui.packageInstaller.model.CloudParams");
        if (cloudParams == null) return;

        IMethodHook disabled = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(false);
            }
        };
        // These methods gate both the warning dialog and the hard registration restriction path.
        hookAllMethods(cloudParams, "isUnrecorded", disabled);
        hookAllMethods(cloudParams, "isNewUnregistered", disabled);

        // NewInstallerPrepareActivity.r1() is the final, direct gate used by
        // OS3 before it logs needShowNewUnregisteredDialog and shows the ICP
        // dialog. Hooking this gate keeps the change limited to the installer.
        Class<?> prepare = findClassIfExists(
                "com.miui.packageInstaller.NewInstallerPrepareActivity");
        if (prepare != null) {
            hookAllMethods(prepare, "r1", disabled);
            // Y0 is the caller that logs needShowNewUnregisteredDialog and
            // creates the dialog. On some OS3 class-loader variants r1 is
            // resolved through an invoke-special path, so bypass the caller
            // as well and continue through the normal post-check path B1().
            hookAllMethods(prepare, "Y0", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    try {
                        callMethod(param.getThisObject(), "B1");
                        param.setResult(null);
                    } catch (Throwable ignored) {
                        // Keep the r1 hook as the fallback if B1 is unavailable.
                    }
                }
            });
            XposedLog.i(TAG, getPackageName(), "Hooked NewInstallerPrepareActivity.r1");
        }
    }
}
