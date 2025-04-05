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

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class UnlockAlwaysOnDisplay implements IXposedHookZygoteInit {
    private static final String TAG = "UnlockAlwaysOnDisplayF";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        ClassLoader classLoader = startupParam.getClass().getClassLoader();
        // 理论这一句就够了，但是尚在测试。
        // BaseXposedInit.mXmlTool.setValueReplacement(XmlTool.TAG_BOOL, "is_only_support_keycode_goto", false);
        XposedHelpers.findAndHookMethod("miui.util.FeatureParser", classLoader, "getBoolean",
                String.class, boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        // XposedBridge.log(TAG + " " + " key: " + param.args[0] + " def: " + param.args[1]);
                        String key = (String) param.args[0];
                        if ("is_only_support_keycode_goto".equals(key)) {
                            param.setResult(false);
                        }
                    }
                }
        );
    }
}
