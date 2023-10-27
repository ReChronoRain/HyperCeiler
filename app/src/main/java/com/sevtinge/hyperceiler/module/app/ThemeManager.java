package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.thememanager.DisableThemeAdNew;
import com.sevtinge.hyperceiler.module.hook.thememanager.EnableFoldTheme;
import com.sevtinge.hyperceiler.module.hook.thememanager.EnablePadTheme;
import com.sevtinge.hyperceiler.module.hook.thememanager.VersionCodeModify;

public class ThemeManager extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableThemeAdNew(), mPrefsMap.getBoolean("various_theme_diable_ads"));
        initHook(new EnablePadTheme(), mPrefsMap.getBoolean("various_theme_enable_pad_theme"));
        initHook(new EnableFoldTheme(), mPrefsMap.getBoolean("various_theme_enable_fold_theme"));

        // 修改版本号
        initHook(new VersionCodeModify(), mPrefsMap.getBoolean("theme_manager_version_code_modify"));
    }

}
