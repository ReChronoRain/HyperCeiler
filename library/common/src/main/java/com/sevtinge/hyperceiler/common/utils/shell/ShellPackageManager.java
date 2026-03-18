package com.sevtinge.hyperceiler.common.utils.shell;

import android.content.ComponentName;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.ShellUtils;
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi;

@SuppressWarnings("unused")
public class ShellPackageManager {
    private static final String TAG = "ShellPackageManager";
    private static final String PM = "pm";

    public static boolean enable(String packageName) {
        return enableOrDisable(packageName, true);
    }

    public static boolean enable(ComponentName componentName) {
        return enableOrDisable(componentName.flattenToString(), true);
    }

    public static boolean disable(String packageName) {
        return enableOrDisable(packageName, false);
    }

    public static boolean disable(ComponentName componentName) {
        return enableOrDisable(componentName.flattenToString(), false);
    }

    public static boolean enableOrDisable(String packageName, boolean isEnable) {
        String status = isEnable ? "enable" : "disable";
        ShellUtils.CommandResult commandResult = ShellUtils.execCommand(PM + " " + status + " " + packageName, true);
        if (ProjectApi.isDebug()) {
            AndroidLog.d(TAG, commandResult.toString());
        }
        return commandResult.result == 0;
    }

    public static boolean enableOrDisable(ComponentName componentName, boolean isEnable) {
        return enableOrDisable(componentName.flattenToString(), isEnable);
    }
}
