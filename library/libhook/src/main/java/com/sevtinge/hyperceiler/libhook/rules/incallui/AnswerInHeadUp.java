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
package com.sevtinge.hyperceiler.libhook.rules.incallui;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

;

public class AnswerInHeadUp extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.incallui.InCallPresenter", "answerIncomingCall", Context.class, String.class, int.class, boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                boolean showUi = (boolean) param.getArgs()[3];
                if (showUi) {
                    Object foregroundInfo = callStaticMethod(findClassIfExists("miui.process.ProcessManager"),
                        "getForegroundInfo");
                    if (foregroundInfo != null) {
                        String topPackage = (String) getObjectField(foregroundInfo, "mForegroundPackageName");
                        if (!"com.miui.home".equals(topPackage)) {
                            param.getArgs()[3] = false;
                        }
                    }
                }
            }
        });
    }
}
