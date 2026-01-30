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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemsettings;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getModuleRes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class ViewWifiPasswordHook extends BaseHook {

    @Override
    @SuppressLint("DiscouragedApi")
    public void init() {
        int titleId = R.string.system_settings_wifipassword_btn_title;
        int dlgTitleId = R.string.system_settings_wifi_password_dlgtitle;

        hookAllMethods("com.android.settings.wifi.SavedAccessPointPreference", "onBindViewHolder", new IMethodHook() {
            @Override

            public void after(AfterHookParam param) throws PackageManager.NameNotFoundException {
                View view = (View) getObjectField(param.getThisObject(), "mView");
                int btnId = view.getResources().getIdentifier("btn_delete", "id", "com.android.settings");
                Button button = view.findViewById(btnId);
                Resources modRes = getModuleRes((Context) callMethod(param.getThisObject(), "getContext"));
                button.setText(modRes.getString(titleId));
            }
        });
        final String[] wifiSharedKey = new String[1];
        final String[] passwordTitle = new String[1];
        findAndHookMethod("miuix.appcompat.app.AlertDialog$Builder", "setTitle", int.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                if (wifiSharedKey[0] != null) {
                    param.getArgs()[0] = dlgTitleId;
                }
            }
        });

        findAndHookMethod("miuix.appcompat.app.AlertDialog$Builder", "setMessage", CharSequence.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                if (wifiSharedKey[0] != null) {
                    CharSequence str = (CharSequence) param.getArgs()[0];
                    str = str + "\n" + passwordTitle[0] + ": " + wifiSharedKey[0];
                    param.getArgs()[0] = str;
                }
            }
        });
        hookAllMethods("miuix.appcompat.app.AlertDialog", "onCreate", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                if (wifiSharedKey[0] != null) {
                    TextView messageView = (TextView) callMethod(param.getThisObject(), "getMessageView");
                    messageView.setTextIsSelectable(true);
                }
            }
        });
        hookAllMethods("com.android.settings.wifi.MiuiSavedAccessPointsWifiSettings", "showDeleteDialog", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) throws PackageManager.NameNotFoundException {
                Object wifiEntry = param.getArgs()[0];
                boolean canShare = (boolean) callMethod(wifiEntry, "canShare");
                if (canShare) {
                    if (passwordTitle[0] == null) {
                        Resources modRes = getModuleRes((Context) callMethod(param.getThisObject(), "getContext"));
                        passwordTitle[0] = modRes.getString(R.string.system_settings_wifi_password_label);
                    }
                    String sharedKey = getSharedKey(param, wifiEntry);
                    wifiSharedKey[0] = sharedKey;
                }
            }

            @Override
            public void after(AfterHookParam param) {
                Object wifiEntry = param.getArgs()[0];
                boolean canShare = (boolean) callMethod(wifiEntry, "canShare");
                if (canShare) {
                    wifiSharedKey[0] = null;
                }
            }
        });
    }

    private String getSharedKey(BeforeHookParam param, Object wifiEntry) {
        Object mWifiManager = getObjectField(param.getThisObject(), "mWifiManager");
        Object wifiConfiguration = callMethod(wifiEntry, "getWifiConfiguration");
        Class<?> WifiDppUtilsClass = findClass("com.android.settings.wifi.dpp.WifiDppUtils");
        String sharedKey = (String) callStaticMethod(WifiDppUtilsClass, "getPresharedKey", mWifiManager, wifiConfiguration);
        sharedKey = (String) callStaticMethod(WifiDppUtilsClass, "removeFirstAndLastDoubleQuotes", sharedKey);
        return sharedKey;
    }
}
