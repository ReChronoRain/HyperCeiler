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
package com.sevtinge.hyperceiler.module.hook.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class UnimportantNotification extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.systemui.statusbar.notification.collection.coordinator.FoldCoordinator$shadeExpansionListener$1",
                isMoreAndroidVersion(35) ? "onPanelExpansionChanged$1" : "onPanelExpansionChanged", "com.android.systemui.shade.ShadeExpansionChangeEvent",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        Object FoldCoordinator = XposedHelpers.getObjectField(param.thisObject, "this$0");
                        XposedHelpers.setObjectField(FoldCoordinator, "mPendingNotifications", new ArrayList<>());
                    }
                }
        );

        try {
            findAndHookMethod("com.android.systemui.statusbar.notification.collection.coordinator.FoldCoordinator",
                    "access$shouldIgnoreEntry",
                    "com.android.systemui.statusbar.notification.collection.coordinator.FoldCoordinator",
                    "com.android.systemui.statusbar.notification.collection.NotificationEntry",
                    new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) {
                            // Object mSbn = XposedHelpers.getObjectField(param.args[1], "mSbn");
                            // String getPackageName = (String) XposedHelpers.callMethod(mSbn, "getPackageName");
                            // logE(TAG, "after: " + param.getResult() + " pkg: " + getPackageName);
                            param.setResult(true);
                        }
                    }
            );
        }catch (Throwable ignore){
            findAndHookMethod("com.android.systemui.statusbar.notification.utils.NotificationUtil",
                    "shouldIgnoreEntry",
                    "com.android.systemui.statusbar.notification.collection.NotificationEntry",
                    new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) {
                            // Object mSbn = XposedHelpers.getObjectField(param.args[1], "mSbn");
                            // String getPackageName = (String) XposedHelpers.callMethod(mSbn, "getPackageName");
                            // logE(TAG, "after: " + param.getResult() + " pkg: " + getPackageName);
                            param.setResult(true);
                        }
                    }
            );
        }
    }
}
