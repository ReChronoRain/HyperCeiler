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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.other;

import static com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.mPct;
import static com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.removePct;

import android.annotation.SuppressLint;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class BrightnessPct extends BaseHook {
    @Override
    @SuppressLint("SetTextI18n")
    public void init() throws NoSuchMethodException {
        hookAllMethods("com.android.systemui.controlcenter.policy.MiuiBrightnessController", "onStop", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                removePct(mPct);
            }
        });

        final Class<?> brightnessUtils = findClassIfExists("com.android.systemui.controlcenter.policy.BrightnessUtils");
        hookAllMethods("com.android.systemui.controlcenter.policy.MiuiBrightnessController", "onChanged", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                int pctTag = 0;
                if (mPct != null && mPct.getTag() != null) {
                    pctTag = (int) mPct.getTag();
                }
                if (pctTag == 0 || mPct == null) return;
                int currentLevel = (int) param.args[3];
                if (brightnessUtils != null) {
                    int maxLevel = (int) XposedHelpers.getStaticObjectField(brightnessUtils, "GAMMA_SPACE_MAX");
                    mPct.setText(((currentLevel * 100) / maxLevel) + "%");
                }
            }
        });
    }
}
