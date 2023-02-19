package com.sevtinge.cemiuiler.module.home.layout;

import android.content.Context;
import android.os.Bundle;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class UnlockGrids extends BaseHook {

    Class<?> mDeviceConfig;

    @Override
    public void init() {

        mDeviceConfig = findClassIfExists("com.miui.home.launcher.DeviceConfig");

        hookAllMethodsSilently("com.miui.home.launcher.compat.LauncherCellCountCompatDevice", "shouldUseDeviceValue", XC_MethodReplacement.returnConstant(false));

        findAndHookMethod("com.miui.home.settings.MiuiHomeSettings", "onCreatePreferences", Bundle.class, String.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mScreenCellsConfig"), "setVisible", true);
            }
        });

        findAndHookMethod(mDeviceConfig, "loadCellsCountConfig", Context.class, boolean.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                int sCellCountY = (int) XposedHelpers.getStaticObjectField(mDeviceConfig, "sCellCountY");
                if (sCellCountY > 6) {
                    int cellHeight = (int) XposedHelpers.callStaticMethod(mDeviceConfig, "getCellHeight");
                    XposedHelpers.setStaticObjectField(mDeviceConfig, "sFolderCellHeight", cellHeight);
                }
            }
        });

        UnlockGridsRes();
    }

    public void UnlockGridsRes() {
        mResHook.setObjectReplacement("com.miui.home", "integer", "config_cell_count_x", 3);
        mResHook.setObjectReplacement("com.miui.home", "integer", "config_cell_count_y", 4);
        mResHook.setObjectReplacement("com.miui.home", "integer", "config_cell_count_x_min", 3);
        mResHook.setObjectReplacement("com.miui.home", "integer", "config_cell_count_y_min", 4);
        mResHook.setObjectReplacement("com.miui.home", "integer", "config_cell_count_x_max", 10);
        mResHook.setObjectReplacement("com.miui.home", "integer", "config_cell_count_y_max", 10);
    }
}
