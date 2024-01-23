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
package com.sevtinge.hyperceiler.module.hook.systemsettings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ViewWifiPasswordHook extends BaseHook {

    @Override
    public void init() {
        int titleId = mResHook.addResource("system_wifipassword_btn_title", R.string.system_settings_wifipassword_btn_title);
        int dlgTitleId = mResHook.addResource("system_wifi_password_dlgtitle", R.string.system_settings_wifi_password_dlgtitle);
        hookAllMethods("com.android.settings.wifi.SavedAccessPointPreference", lpparam.classLoader, "onBindViewHolder", new MethodHook() {
            @Override
            @SuppressLint("DiscouragedApi")
            protected void after(MethodHookParam param) throws Throwable {
                View view = (View) XposedHelpers.getObjectField(param.thisObject, "mView");
                int btnId = view.getResources().getIdentifier("btn_delete", "id", "com.android.settings");
                Button button = view.findViewById(btnId);
                button.setText(titleId);
            }
        });
        final String[] wifiSharedKey = new String[1];
        final String[] passwordTitle = new String[1];
        findAndHookMethod("miuix.appcompat.app.AlertDialog$Builder", lpparam.classLoader, "setTitle", int.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                if (wifiSharedKey[0] != null) {
                    param.args[0] = dlgTitleId;
                }
            }
        });

        findAndHookMethod("miuix.appcompat.app.AlertDialog$Builder", lpparam.classLoader, "setMessage", CharSequence.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                if (wifiSharedKey[0] != null) {
                    CharSequence str = (CharSequence) param.args[0];
                    str = str + "\n" + passwordTitle[0] + ": " + wifiSharedKey[0];
                    param.args[0] = str;
                }
            }
        });
        hookAllMethods("miuix.appcompat.app.AlertDialog", lpparam.classLoader, "onCreate", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                if (wifiSharedKey[0] != null) {
                    TextView messageView = (TextView) XposedHelpers.callMethod(param.thisObject, "getMessageView");
                    messageView.setTextIsSelectable(true);
                }
            }
        });
        hookAllMethods("com.android.settings.wifi.MiuiSavedAccessPointsWifiSettings", lpparam.classLoader, "showDeleteDialog", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object wifiEntry = param.args[0];
                boolean canShare = (boolean) XposedHelpers.callMethod(wifiEntry, "canShare");
                if (canShare) {
                    if (passwordTitle[0] == null) {
                        Resources modRes = getModuleRes((Context) XposedHelpers.callMethod(param.thisObject, "getContext"));
                        passwordTitle[0] = modRes.getString(R.string.system_settings_wifi_password_label);
                    }
                    String sharedKey = getSharedKey(param, wifiEntry);
                    wifiSharedKey[0] = sharedKey;
                }
            }

            @Override
            protected void after(MethodHookParam param) {
                Object wifiEntry = param.args[0];
                boolean canShare = (boolean) XposedHelpers.callMethod(wifiEntry, "canShare");
                if (canShare) {
                    wifiSharedKey[0] = null;
                }
            }
        });
    }

    private String getSharedKey(XC_MethodHook.MethodHookParam param, Object wifiEntry) {
        Object mWifiManager = XposedHelpers.getObjectField(param.thisObject, "mWifiManager");
        Object wifiConfiguration = XposedHelpers.callMethod(wifiEntry, "getWifiConfiguration");
        Class<?> WifiDppUtilsClass = XposedHelpers.findClass("com.android.settings.wifi.dpp.WifiDppUtils", lpparam.classLoader);
        String sharedKey = (String) XposedHelpers.callStaticMethod(WifiDppUtilsClass, "getPresharedKey", mWifiManager, wifiConfiguration);
        sharedKey = (String) XposedHelpers.callStaticMethod(WifiDppUtilsClass, "removeFirstAndLastDoubleQuotes", sharedKey);
        return sharedKey;
    }
}
