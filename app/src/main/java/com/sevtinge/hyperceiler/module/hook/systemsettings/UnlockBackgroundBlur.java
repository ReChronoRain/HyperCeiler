package com.sevtinge.hyperceiler.module.hook.systemsettings;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XposedHelpers;

public class UnlockBackgroundBlur extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("android.os.SystemProperties", "getBoolean", String.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                String strArg = (String) param.args[0];
                //Boolean boolArg = (Boolean) param.args[1];
                if (strArg.equals("persist.sys.background_blur_supported")) {
                    param.setResult(true);
                }
            }
        });
        // Class<?> mMiuiDisplaySettings = findClassIfExists("com.android.settings.MiuiDisplaySettings");
        // Class<?> mDisplaySettingsCloudBackupHelper = findClassIfExists("com.android.settings.cloudbackup.DisplaySettingsCloudBackupHelper");
        // Class<?> mMiuiBlurUtils = findClassIfExists("miuix.core.util.MiuiBlurUtils");
        //
        // XposedHelpers.setStaticBooleanField(mMiuiDisplaySettings, "BACKGROUND_BLUR_SUPPORTED", false);
        // XposedHelpers.setStaticBooleanField(mDisplaySettingsCloudBackupHelper, "BACKGROUND_BLUR_SUPPORTED", false);
        // XposedHelpers.setBooleanField(mMiuiBlurUtils, "SUPPORT_MIUI_BLUR", false);
    }
}
