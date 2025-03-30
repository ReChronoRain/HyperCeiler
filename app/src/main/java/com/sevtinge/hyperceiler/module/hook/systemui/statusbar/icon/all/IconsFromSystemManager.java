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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class IconsFromSystemManager extends BaseHook {

    @Override
    public void init() {
        Class<?> statusBarIconControllerImpl;
        statusBarIconControllerImpl = findClassIfExists("com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl");
        if (statusBarIconControllerImpl == null) {
            statusBarIconControllerImpl = findClassIfExists("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl");
        }

        boolean successHooked = findAndHookMethodSilently(statusBarIconControllerImpl,
            "setIcon",
            String.class, findClass("com.android.internal.statusbar.StatusBarIcon"),
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    String slotName = (String) param.args[0];
                    if (checkSlot(slotName)) {
                        XposedHelpers.setObjectField(param.args[1], "visible", false);
                    }
                }
            }
        );

        if (!successHooked) {
            findAndHookMethod(statusBarIconControllerImpl,
                "setIcon",
                String.class, findClass("com.android.systemui.statusbar.phone.StatusBarIconHolder"),
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        String slotName = (String) param.args[0];
                        if (checkSlot(slotName)) {
                            Object statusBarIconInstance = XposedHelpers.getObjectField(param.args[1], "mIcon");
                            XposedHelpers.setObjectField(statusBarIconInstance, "visible", false);
                        }
                    }
                });
        }

    }

    public boolean checkSlot(String slotName) {
        switch (slotName) {
            case "stealth" -> {
                return mPrefsMap.getBoolean("system_ui_status_bar_hide_icon_stealth");
            }
            case "mute" -> {
                return mPrefsMap.getBoolean("system_ui_status_bar_hide_icon_mute");
            }
            case "speakerphone" -> {
                return mPrefsMap.getBoolean("system_ui_status_bar_hide_icon_speakerphone");
            }
            case "call_record" -> {
                return mPrefsMap.getBoolean("system_ui_status_bar_hide_icon_call_record");
            }
            default -> {
                return false;
            }
        }
    }
}
