package com.sevtinge.hyperceiler.home.utils;

import static com.sevtinge.hyperceiler.Application.isModuleActivated;

import android.content.Context;

import com.sevtinge.hyperceiler.common.utils.DialogHelper;

public class XposedActivateHelper {

    public static int XposedVersion = 0;

    public static void init(Context context) {
        checkActivateState(context, isModuleActivated);
    }

    private static void checkActivateState(Context context, boolean isActivate) {
        if (isActivate) return;
        //DialogHelper.showXposedActivateDialog(context);
    }
}
