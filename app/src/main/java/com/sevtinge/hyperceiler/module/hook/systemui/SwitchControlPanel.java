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

import static com.sevtinge.hyperceiler.utils.log.AndroidLogUtils.LogD;
import static com.sevtinge.hyperceiler.utils.log.AndroidLogUtils.LogI;

import android.view.MotionEvent;

import com.sevtinge.hyperceiler.module.base.BaseHook;

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
                LogI(TAG, "mDownX：" + mDownX + "in before");
                LogI(TAG, "f：" + f + "in before");
                LogI(TAG, "：" + i + "in before");
                i *= -1;
                int i2 = i;
                LogI(TAG, "：" + i2 + "in before");
            }

            @Override
            protected void after(MethodHookParam param) {
                float mDownX = XposedHelpers.getFloatField(param.thisObject, "mDownX");
                float f = (float) param.args[1];
                int i = (Float.compare(mDownX, f / 2.0f));
                LogI(TAG, "mDownX：" + mDownX + "in after");
                LogI(TAG, "f：" + f + "in after");
                LogI(TAG, "：" + i + "in after");
                i *= -1;
                int i2 = i;
                LogI(TAG, "：" + i2 + "in after");
            }
        });
    }
}
