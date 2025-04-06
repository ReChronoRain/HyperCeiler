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
package com.sevtinge.hyperceiler.hook.module.hook.incallui;

import android.content.Context;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class AnswerInHeadUp extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.incallui.InCallPresenter", "answerIncomingCall", Context.class, String.class, int.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                boolean showUi = (boolean) param.args[3];
                if (showUi) {
                    Object foregroundInfo = XposedHelpers.callStaticMethod(findClassIfExists("miui.process.ProcessManager"),
                            "getForegroundInfo");
                    if (foregroundInfo != null) {
                        String topPackage = (String) XposedHelpers.getObjectField(foregroundInfo, "mForegroundPackageName");
                        /*if (!"com.miui.home".equals(topPackage)) {
                            param.args[3] = false;
                        }*/
                    }
                }
            }
        });
    }
}
