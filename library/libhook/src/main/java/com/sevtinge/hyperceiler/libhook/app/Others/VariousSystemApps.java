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

package com.sevtinge.hyperceiler.libhook.app.Others;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.various.dialog.DialogCustom;
import com.sevtinge.hyperceiler.libhook.rules.various.system.CollapseMiuiTitle;
import com.sevtinge.hyperceiler.libhook.rules.various.system.MiuiAppNoOverScroll;

import java.util.Arrays;
import java.util.HashSet;

@HookBase(targetPackage = "VariousSystemApps")
public class VariousSystemApps extends BaseLoad {
    Class<?> mHelpers;
    String mPackageName;
    boolean isMiuiApps;

    @Override
    public void onPackageLoaded() {
        mPackageName = getPackageName();
        isMiuiApps = mPackageName.startsWith("com.miui") || mPackageName.startsWith("com.xiaomi") || miuiDialogCustomApps.contains(mPackageName);
        initHook(new DialogCustom(), isMiuiDialogCustom());
        initHook(new MiuiAppNoOverScroll(), isMiuiOverScrollApps());
        initHook(new CollapseMiuiTitle(), isCollapseMiuiTitleApps());
    }

    private boolean isMiuiOverScrollApps() {
        return mPrefsMap.getBoolean("various_no_overscroll") && miuiOverScrollApps.contains(mPackageName);
    }

    private boolean isMiuiDialogCustom() {
        return mPrefsMap.getStringAsInt("various_dialog_gravity", 0) != 0 && isMiuiApps;
    }

    private boolean isCollapseMiuiTitleApps() {
        return mPrefsMap.getStringAsInt("various_collapse_miui_title", 0) != 0 && collapseMiuiTitleApps.contains(mPackageName);
    }

    HashSet<String> miuiOverScrollApps = new HashSet<>(Arrays.asList(
        "com.android.fileexplorer",
        "com.android.providers.downloads.ui",
        "com.android.settings"
    ));

    HashSet<String> miuiDialogCustomApps = new HashSet<>(Arrays.asList(
        "com.android.fileexplorer",
        "com.android.providers.downloads.ui",
        "com.android.settings"
    ));

    HashSet<String> collapseMiuiTitleApps = new HashSet<>(Arrays.asList(
        "com.android.fileexplorer",
        "com.android.providers.downloads.ui",
        "com.android.settings"
    ));
}
