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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.thememanager.AllowDownloadMore;
import com.sevtinge.hyperceiler.module.hook.thememanager.AllowThirdTheme;
import com.sevtinge.hyperceiler.module.hook.thememanager.DisableThemeAdNew;
import com.sevtinge.hyperceiler.module.hook.thememanager.EnableFoldTheme;
import com.sevtinge.hyperceiler.module.hook.thememanager.EnablePadTheme;
import com.sevtinge.hyperceiler.module.hook.thememanager.VersionCodeModify;

@HookBase(targetPackage = "com.android.thememanager",  isPad = false)
public class ThemeManager extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new AllowThirdTheme(), mPrefsMap.getBoolean("system_framework_allow_third_theme"));
        initHook(new DisableThemeAdNew(), mPrefsMap.getBoolean("various_theme_disable_ads"));
        initHook(new AllowDownloadMore(), mPrefsMap.getBoolean("theme_manager_allow_download_more"));
        initHook(new EnablePadTheme(), mPrefsMap.getBoolean("various_theme_enable_pad_theme"));
        initHook(new EnableFoldTheme(), mPrefsMap.getBoolean("various_theme_enable_fold_theme"));

        // 修改版本号
        initHook(new VersionCodeModify(), mPrefsMap.getStringAsInt("theme_manager_new_version_code_modify", 0) != 0);
    }

}
