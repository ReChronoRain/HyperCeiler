package com.sevtinge.hyperceiler.utils;

import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.IS_LOGGER_ALIVE;
import static com.sevtinge.hyperceiler.utils.XposedActivateHelper.isModuleActive;

import android.content.Context;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

public class LogServiceUtils {

    public static void init(Context context) {
        shouldShowLogServiceWarnDialog(context);
    }

    private static void shouldShowLogServiceWarnDialog(Context context) {
        if (showLogServiceWarn()) {
            DialogHelper.showLogServiceWarnDialog(context);
        }
    }

    private static boolean showLogServiceWarn() {
        return !IS_LOGGER_ALIVE && isModuleActive && BuildConfig.BUILD_TYPE != "release" &&
            !PrefsUtils.mSharedPreferences.getBoolean("prefs_key_development_close_log_alert_dialog", false);
    }
}
