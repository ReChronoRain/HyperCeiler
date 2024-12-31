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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.Set;

import de.robv.android.xposed.XposedHelpers;

public class ClipboardWhitelist extends BaseHook {
    @Override
    public void init() {
        Class<?> clipboardClass = XposedHelpers.findClass("com.android.server.clipboard.ClipboardService", lpparam.classLoader);
        String key = "system_framework_clipboard_whitelist_apps";
        Set<String> selectedApps = mPrefsMap.getStringSet(key);
        hookAllMethods(clipboardClass, "clipboardAccessAllowed", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                for (String pkgName : selectedApps) {
                    if (pkgName.equals(param.args[1])) {
                        param.setResult(true);
                        return;
                    }
                }
            }
        });
    }
}
