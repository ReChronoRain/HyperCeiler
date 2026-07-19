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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class AutoCollapse extends BaseHook {
    @Override
    public void init() {

        findAndHookMethod("com.android.systemui.qs.tileimpl.QSTileImpl", "click","com.android.systemui.animation.Expandable", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                Object mState = callMethod(param.getThisObject(), "getState");
                int state = com.sevtinge.hyperceiler.libhook.base.BaseHook.getIntField(mState, "state");
                if (state != 0) {
                    String tileSpec = (String) callMethod(param.getThisObject(), "getTileSpec");
                    if (!"edit".equals(tileSpec)) {
                        Object mHost = getObjectField(param.getThisObject(), "mHost");
                       callMethod(mHost, "collapsePanels");
                    }
                }
            }
        });
    }
}
