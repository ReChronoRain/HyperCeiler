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
package com.sevtinge.hyperceiler.module.hook.incallui;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import miui.process.ForegroundInfo;
import miui.process.ProcessManager;

public class AnswerInHeadUp extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.incallui.InCallPresenter", "answerIncomingCall", Context.class, String.class, int.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                boolean showUi = (boolean) param.args[3];
                if (showUi) {
                    ForegroundInfo foregroundInfo = ProcessManager.getForegroundInfo();
                    if (foregroundInfo != null) {
                        String topPackage = foregroundInfo.mForegroundPackageName;
                        /*if (!"com.miui.home".equals(topPackage)) {
                            param.args[3] = false;
                        }*/
                    }
                }
            }
        });
    }
}
