package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.thememanager.DisableThemeAdNew;
import com.sevtinge.cemiuiler.module.hook.thememanager.EnableFoldTheme;
import com.sevtinge.cemiuiler.module.hook.thememanager.EnablePadTheme;
import com.sevtinge.cemiuiler.module.hook.thememanager.ThemeCrackNew;
import com.sevtinge.cemiuiler.module.hook.thememanager.VersionCodeModify;

public class ThemeManager extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableThemeAdNew(), mPrefsMap.getBoolean("various_theme_diable_ads"));
        initHook(new ThemeCrackNew(), mPrefsMap.getBoolean("enable_function") && mPrefsMap.getBoolean("various_theme_crack"));
        initHook(new EnablePadTheme(), mPrefsMap.getBoolean("various_theme_enable_pad_theme"));
        initHook(new EnableFoldTheme(), mPrefsMap.getBoolean("various_theme_enable_fold_theme"));

        // 修改版本号
        initHook(new VersionCodeModify(), mPrefsMap.getBoolean("theme_manager_version_code_modify"));
    }

}
