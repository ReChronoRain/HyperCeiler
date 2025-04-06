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
package com.sevtinge.hyperceiler.hook.module.hook.systemui;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class AutoCollapse extends BaseHook {
    @Override
    public void init() {
        MethodHook method = new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                Object mState = XposedHelpers.callMethod(param.thisObject, "getState");
                int state = XposedHelpers.getIntField(mState, "state");
                if (state != 0) {
                    String tileSpec = (String) XposedHelpers.callMethod(param.thisObject, "getTileSpec");
                    if (!"edit".equals(tileSpec)) {
                        Object mHost = XposedHelpers.getObjectField(param.thisObject, "mHost");
                        XposedHelpers.callMethod(mHost, "collapsePanels");
                    }
                }
            }
        };

        if (isMoreHyperOSVersion(2f)) {
            findAndHookMethod("com.android.systemui.qs.tileimpl.QSTileImpl", lpparam.classLoader, "click","com.android.systemui.animation.Expandable", method);
        } else {
            findAndHookMethod("com.android.systemui.qs.tileimpl.QSTileImpl", lpparam.classLoader, "click", View.class, method);
        }
    }
}
