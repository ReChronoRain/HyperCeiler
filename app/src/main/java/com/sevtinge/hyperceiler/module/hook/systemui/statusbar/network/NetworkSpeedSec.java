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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class NetworkSpeedSec extends BaseHook {
    @Override
    public void init() {
        try {
            findClass("com.android.systemui.statusbar.views.NetworkSpeedView").getDeclaredMethod("setNetworkSpeed", String.class);
            findAndHookMethod("com.android.systemui.statusbar.views.NetworkSpeedView",
                "setNetworkSpeed", String.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (param.args[0] != null) {
                            String mText = (String) param.args[0];
                            param.args[0] = mText.replace("/", "")
                                .replace("s", "")
                                .replace("'", "")
                                .replace("วิ", "");
                        }
                    }
                }
            );
        } catch (NoSuchMethodException e) {
            try {
                findClass("com.android.systemui.statusbar.views.NetworkSpeedView").getDeclaredMethod("setNetworkSpeed", String.class, String.class);
                findAndHookMethod("com.android.systemui.statusbar.views.NetworkSpeedView",
                    "setNetworkSpeed", String.class, String.class,
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            // logE(TAG, "1: " + param.args[0] + " 2: " + param.args[1]);
                            String mText = (String) param.args[1];
                            param.args[1] = mText.replace("/", "")
                                .replace("B", "")
                                .replace("s", "")
                                .replace("'", "")
                                .replace("วิ", "");
                        }
                    }
                );
            } catch (NoSuchMethodException f) {
                logE(TAG, "No such: " + e + " And: " + f);
            }
        }

    }
}
