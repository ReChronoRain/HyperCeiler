package com.sevtinge.hyperceiler.utils;

import android.content.Context;

import com.sevtinge.hyperceiler.ui.common.utils.DialogHelper;

public class XposedActivateHelper {

    public static boolean isModuleActive = false;
    public static int XposedVersion = 0;

    public static void checkActivateState(Context context) {
        if (!isModuleActive) DialogHelper.showXposedActivateDialog(context);
    }
}
