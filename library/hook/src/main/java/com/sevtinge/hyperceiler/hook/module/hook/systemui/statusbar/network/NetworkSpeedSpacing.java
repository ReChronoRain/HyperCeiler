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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network;

import android.os.Message;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class NetworkSpeedSpacing extends BaseHook {
    public boolean handler;

    @Override
    public void init() {
        try {
            findClass("com.android.systemui.statusbar.policy.NetworkSpeedController").getDeclaredMethod("postUpdateNetworkSpeedDelay", long.class);
            findAndHookMethod("com.android.systemui.statusbar.policy.NetworkSpeedController",
                    "postUpdateNetworkSpeedDelay", long.class, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            long originInterval = (long) param.args[0];
                            if (originInterval == 4000L) {
                                originInterval = mPrefsMap.getInt(
                                    "system_ui_statusbar_network_speed_update_spacing",
                                    4
                                ) * 1000L;
                                param.args[0] = originInterval;
                            }
                        }
                    }
            );
        } catch (NoSuchMethodException e) {
            try {
                findAndHookMethod("com.android.systemui.statusbar.policy.NetworkSpeedController$4",
                        "handleMessage", Message.class, new MethodHook() {
                            @Override
                            protected void after(MethodHookParam param) {
                                Message message = (Message) param.args[0];
                                Object handleMessage = XposedHelpers.getObjectField(param.thisObject, "this$0");
                                Object mBgHandler = XposedHelpers.getObjectField(handleMessage, "mBgHandler");
                                if (message.what == 200001) {
                                    handleMessage(mBgHandler, true);
                                    // logE(TAG, "200001: " + mBgHandler);
                                }
                            }
                        }
                );
            } catch (Throwable t) {
                findAndHookMethod("com.android.systemui.statusbar.policy.NetworkSpeedController$2",
                        "handleMessage", Message.class, new MethodHook() {
                            @Override
                            protected void after(MethodHookParam param) {
                                Message message = (Message) param.args[0];
                                Object handleMessage = XposedHelpers.getObjectField(param.thisObject, "this$0");
                                Object mBgHandler = XposedHelpers.getObjectField(handleMessage, "mBgHandler");
                                if (message.what == 200001) {
                                    handleMessage(mBgHandler, true);
                                    // logE(TAG, "200001: " + mBgHandler);
                                }
                            }
                        }
                );
            }
        }
    }

    public void handleMessage(Object mBgHandler, boolean z) {
        XposedHelpers.callMethod(mBgHandler, "removeMessages", 200001);
        if (z) {
            XposedHelpers.callMethod(mBgHandler, "sendEmptyMessageDelayed", 200001,
                mPrefsMap.getInt("system_ui_statusbar_network_speed_update_spacing",
                    4
                ) * 1000L
            );
        }
    }
}
