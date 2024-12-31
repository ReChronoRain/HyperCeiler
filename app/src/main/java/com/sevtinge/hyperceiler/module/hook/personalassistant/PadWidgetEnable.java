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
package com.sevtinge.hyperceiler.module.hook.personalassistant;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class PadWidgetEnable extends BaseHook {

    Class<?> m = findClassIfExists("c.h.e.k.k.c.d");

    Class<?> m2 = findClassIfExists("c.h.e.p.s");

    public enum DeviceType {
        PAD, FOLDABLE_DEVICE, PHONE
    }

    @Override
    public void init() {
        hookAllMethods(m2, "c", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.callMethod(XposedHelpers.findClassIfExists("miui.os.Build", lpparam.classLoader), "isTablet", true);
                XposedHelpers.setStaticBooleanField(XposedHelpers.findClassIfExists("miui.os.Build", lpparam.classLoader), "IS_TABLET", true);
            }
        });

        /*findAndHookMethod(m,"a", Build.class, Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod(m2,"c", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        XposedHelpers.setStaticBooleanField(XposedHelpers.findClassIfExists("miui.os.Build",lpparam.classLoader),"IS_TABLET", true);
                    }
                });
            }
        });*/
    }
}
