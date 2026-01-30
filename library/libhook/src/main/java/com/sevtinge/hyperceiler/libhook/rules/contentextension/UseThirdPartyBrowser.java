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
package com.sevtinge.hyperceiler.libhook.rules.contentextension;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IReplaceHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

;

public class UseThirdPartyBrowser extends BaseHook {

    @Override
    public void init() {
        // XposedBridge.log("Hook到传送门进程！");
        final Class<?> clazz = findClass("com.miui.contentextension.utils.AppsUtils");
        // getClassInfo(clazz);

        findAndReplaceMethod(clazz, "getIntentWithBrowser", String.class, (IReplaceHook) param -> {
            XposedLog.i(TAG, getPackageName(), "hooked url " + param.getArgs()[0].toString());
            Uri uri = Uri.parse(param.getArgs()[0].toString());
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(uri);
            return intent;
        });

        findAndReplaceMethod(clazz, "openGlobalSearch", Context.class, String.class, String.class, (IReplaceHook) param -> {
            XposedLog.i(TAG, getPackageName(), "hooked all-search on, word is " + param.getArgs()[1].toString() + ", from " + param.getArgs()[2].toString());
            try {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_WEB_SEARCH);
                intent.putExtra(SearchManager.QUERY, param.getArgs()[1].toString());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ((Context) param.getArgs()[0]).startActivity(intent);
            } catch (Exception e) {
                XposedLog.e(TAG, getPackageName(), e);
            }
            return null;
        });
    }
}
