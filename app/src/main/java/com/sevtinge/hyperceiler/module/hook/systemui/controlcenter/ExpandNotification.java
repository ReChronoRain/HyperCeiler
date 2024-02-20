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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.Set;

import de.robv.android.xposed.XposedHelpers;

public class ExpandNotification extends BaseHook {
    @Override
    public void init() {
        Set<String> mPkg = mPrefsMap.getStringSet("system_ui_control_center_expand_notification");
        hookAllMethods("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", "setFeedbackIcon",
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    boolean mOnKeyguard = (boolean) XposedHelpers.callMethod(param.thisObject, "isOnKeyguard");
                    if (!mOnKeyguard) {
                        Object notification = XposedHelpers.getObjectField(XposedHelpers.callMethod(param.thisObject, "getEntry"), "mSbn");
                        String pkgName = (String) XposedHelpers.callMethod(notification, "getPackageName");
                        if (mPkg.contains(pkgName))
                            XposedHelpers.callMethod(param.thisObject, "setSystemExpanded", true);
                    }
                }
            }
        );

        hookAllMethods("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow", "setHeadsUp",
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    boolean mOnKeyguard = (boolean) XposedHelpers.callMethod(param.thisObject, "isOnKeyguard");
                    boolean showHeadsUp = (boolean) param.args[0];
                    if (!mOnKeyguard && showHeadsUp) {
                        View notifyRow = (View) param.thisObject;
                        Object notification = XposedHelpers.getObjectField(XposedHelpers.callMethod(param.thisObject, "getEntry"), "mSbn");
                        String pkgName = (String) XposedHelpers.callMethod(notification, "getPackageName");
                        if (mPkg.contains(pkgName)) {
                            Runnable expandNotify = new Runnable() {
                                @Override
                                public void run() {
                                    XposedHelpers.callMethod(param.thisObject, "expandNotification");
                                }
                            };
                            notifyRow.postDelayed(expandNotify, 60);
                        }
                    }
                }
            }
        );
    }
}
