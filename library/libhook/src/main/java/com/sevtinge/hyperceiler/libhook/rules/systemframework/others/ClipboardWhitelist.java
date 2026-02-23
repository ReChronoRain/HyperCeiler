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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import java.util.Set;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class ClipboardWhitelist extends BaseHook {
    @Override
    public void init() {
        Class<?> clipboardClass = findClass("com.android.server.clipboard.ClipboardService");
        String key = "system_framework_clipboard_whitelist_apps";
        Set<String> selectedApps = PrefsBridge.getStringSet(key);
        hookAllMethods(clipboardClass, "clipboardAccessAllowed", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                for (String pkgName : selectedApps) {
                    if (pkgName.equals(param.getArgs()[1])) {
                        param.setResult(true);
                        return;
                    }
                }
            }
        });
    }
}
