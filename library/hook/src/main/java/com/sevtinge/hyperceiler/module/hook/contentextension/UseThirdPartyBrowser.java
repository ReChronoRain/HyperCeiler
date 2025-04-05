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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.contentextension;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;


public class UseThirdPartyBrowser extends BaseHook {

    @Override
    public void init() {
        // XposedBridge.log("Hook到传送门进程！");
        final Class<?> clazz = XposedHelpers.findClass("com.miui.contentextension.utils.AppsUtils", lpparam.classLoader);
        // getClassInfo(clazz);

        XposedHelpers.findAndHookMethod(clazz, "getIntentWithBrowser", String.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                logI(TAG, UseThirdPartyBrowser.this.lpparam.packageName, "hooked url " + param.args[0].toString());
                Uri uri = Uri.parse(param.args[0].toString());
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                intent.setData(uri);
                return intent;
            }
        });

        XposedHelpers.findAndHookMethod(clazz, "openGlobalSearch", Context.class, String.class, String.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                logI(TAG, UseThirdPartyBrowser.this.lpparam.packageName, "hooked all-search on, word is " + param.args[1].toString() + ", from " + param.args[2].toString());
                try {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_WEB_SEARCH);
                    intent.putExtra(SearchManager.QUERY, param.args[1].toString());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ((Context) param.args[0]).startActivity(intent);
                } catch (Exception e) {
                    logE(TAG, UseThirdPartyBrowser.this.lpparam.packageName, e);
                }
                return null;
            }
        });
    }
}
