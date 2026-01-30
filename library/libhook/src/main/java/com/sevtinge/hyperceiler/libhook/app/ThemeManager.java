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
package com.sevtinge.hyperceiler.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.thememanager.AllowDownloadMore;
import com.sevtinge.hyperceiler.libhook.rules.thememanager.AllowThirdTheme;
import com.sevtinge.hyperceiler.libhook.rules.thememanager.DisableThemeAdNew;
import com.sevtinge.hyperceiler.libhook.rules.thememanager.UnlockAIWallPaper;

@HookBase(targetPackage = "com.android.thememanager")
public class ThemeManager extends BaseLoad {

    public ThemeManager() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {
        initHook(new AllowThirdTheme(), mPrefsMap.getBoolean("system_framework_allow_third_theme"));
        initHook(new DisableThemeAdNew(), mPrefsMap.getBoolean("various_theme_disable_ads"));
        initHook(new AllowDownloadMore(), mPrefsMap.getBoolean("theme_manager_allow_download_more"));
        initHook(UnlockAIWallPaper.INSTANCE, mPrefsMap.getBoolean("theme_manager_unlock_ai_wallpaper"));
    }

}
