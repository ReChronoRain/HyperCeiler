/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */
package com.sevtinge.hyperceiler.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

/** Loads the package-installer rules when the ROM exposes the AOSP name. */
@HookBase(targetPackage = "com.android.packageinstaller")
public class PackageInstallerAndroid extends PackageInstaller {
    @Override
    public void onPackageLoaded() {
        // The AOSP package is also used on MIUI ROMs, but it does not contain
        // the MIUI installer classes used by PackageInstaller's rules.
        if (BaseHook.findClassIfExists("com.miui.packageInstaller.InstallStart") == null) return;
        super.onPackageLoaded();
    }
}
