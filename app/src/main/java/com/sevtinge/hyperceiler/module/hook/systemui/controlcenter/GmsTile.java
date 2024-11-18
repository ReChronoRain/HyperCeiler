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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.ArrayMap;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.TileUtils;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

public class GmsTile extends TileUtils {
    public final String CheckGms = "com.google.android.gms";
    public final String[] GmsAppsSystem = new String[]{
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.vending",
        "com.google.android.syncadapters.contacts",
        "com.google.android.backuptransport",
        "com.google.android.onetimeinitializer",
        "com.google.android.partnersetup",
        "com.google.android.configupdater",
        "com.google.android.ext.shared",
        "com.google.android.printservice.recommendation"};

    @Override
    public void init() {
        super.init();
    }

    @Override
    public Class<?> customClass() {
        return findClassIfExists("com.android.systemui.qs.tiles.ScreenLockTile");
    }

    @Override
    public String setTileProvider() {
        return "screenLockTileProvider";
    }

    @Override
    public String customName() {
        return "custom_GMS";
    }

    @Override
    public int customRes() {
        return R.string.tiles_gms;
    }

    @Override
    public void tileCheck(MethodHookParam param, String tileName) {
        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        PackageManager packageManager = mContext.getPackageManager();
        try {
            packageManager.getPackageInfo(CheckGms, PackageManager.GET_ACTIVITIES);
            param.setResult(true);
        } catch (PackageManager.NameNotFoundException e) {
            logE(TAG, "com.android.systemui", "Not Find GMS App: " + e);
            param.setResult(false);
        }
    }

    @Override
    public Intent tileHandleLongClick(MethodHookParam param, String tileName) {
        // 长按跳转谷歌基础服务页面
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.googlebase.ui.GmsCoreSettings"));
        return intent;
    }

    @Override
    public void tileClick(MethodHookParam param, String tileName) {
        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        PackageManager packageManager = mContext.getPackageManager();
        int End = packageManager.getApplicationEnabledSetting(CheckGms);
        if (End == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            for (String GmsAppsSystem : GmsAppsSystem) {
                try {
                    packageManager.getPackageInfo(GmsAppsSystem, PackageManager.GET_ACTIVITIES);
                    packageManager.setApplicationEnabledSetting(GmsAppsSystem, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
                    logD(TAG, "com.android.systemui", "To Enabled Gms App:" + GmsAppsSystem);
                } catch (PackageManager.NameNotFoundException e) {
                    logE(TAG, "com.android.systemui", "Don't have Gms app :" + GmsAppsSystem);
                }
            }
            XposedHelpers.callMethod(param.thisObject, "refreshState");
        } else if (End == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || End == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            for (String GmsAppsSystem : GmsAppsSystem) {
                try {
                    packageManager.getPackageInfo(GmsAppsSystem, PackageManager.GET_ACTIVITIES);
                    packageManager.setApplicationEnabledSetting(GmsAppsSystem, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
                    logD(TAG, "com.android.systemui", "To Disabled Gms App:" + GmsAppsSystem);
                } catch (PackageManager.NameNotFoundException e) {
                    logE(TAG, "com.android.systemui", "Don't have Gms app :" + GmsAppsSystem);
                }
            }
            XposedHelpers.callMethod(param.thisObject, "refreshState");
        }

    }

    @Override
    public ArrayMap<String, Integer> tileUpdateState(MethodHookParam param, Class<?> mResourceIcon, String tileName) {
        boolean isEnable;
        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        PackageManager packageManager = mContext.getPackageManager();
        int End = packageManager.getApplicationEnabledSetting(CheckGms);
        isEnable = End == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        ArrayMap<String, Integer> tileResMap = new ArrayMap<>();
        tileResMap.put("custom_GMS_Enable", isEnable ? 1 : 0);
        tileResMap.put("custom_GMS_ON", R.drawable.ic_control_center_gms_toggle_on);
        tileResMap.put("custom_GMS_OFF", R.drawable.ic_control_center_gms_toggle_off);
        return tileResMap;
    }
}
