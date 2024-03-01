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
package com.sevtinge.hyperceiler.module.hook.systemui;

import android.view.MotionEvent;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import de.robv.android.xposed.XposedHelpers;

public class SwitchControlPanel extends BaseHook {

    Class<?> mControlPanelWindowManager;

    @Override
    public void init() {

        mControlPanelWindowManager = findClassIfExists("com.android.systemui.controlcenter.phone.ControlPanelWindowManager");

        findAndHookMethod(mControlPanelWindowManager, "dispatchToControlPanel", MotionEvent.class, float.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                float f = (float) param.args[1];
                XposedHelpers.setFloatField(param.thisObject, "mDownX", f);
                float mDownX = XposedHelpers.getFloatField(param.thisObject, "mDownX");
                int i = (Float.compare(mDownX, f / 2.0f));
                AndroidLogUtils.logI(TAG, "mDownX：" + mDownX + "in before");
                AndroidLogUtils.logI(TAG, "f：" + f + "in before");
                AndroidLogUtils.logI(TAG, "：" + i + "in before");
                i *= -1;
                int i2 = i;
                AndroidLogUtils.logI(TAG, "：" + i2 + "in before");
            }

            @Override
            protected void after(MethodHookParam param) {
                float mDownX = XposedHelpers.getFloatField(param.thisObject, "mDownX");
                float f = (float) param.args[1];
                int i = (Float.compare(mDownX, f / 2.0f));
                AndroidLogUtils.logI(TAG, "mDownX：" + mDownX + "in after");
                AndroidLogUtils.logI(TAG, "f：" + f + "in after");
                AndroidLogUtils.logI(TAG, "：" + i + "in after");
                i *= -1;
                int i2 = i;
                AndroidLogUtils.logI(TAG, "：" + i2 + "in after");
            }
        });
    }
}
