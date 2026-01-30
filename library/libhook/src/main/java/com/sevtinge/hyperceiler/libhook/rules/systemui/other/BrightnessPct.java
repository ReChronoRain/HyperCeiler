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
package com.sevtinge.hyperceiler.libhook.rules.systemui.other;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.mPct;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.removePct;

import android.annotation.SuppressLint;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class BrightnessPct extends BaseHook {
    @Override
    @SuppressLint("SetTextI18n")
    public void init() {
        hookAllMethods("com.android.systemui.controlcenter.policy.MiuiBrightnessController", "onStop", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                removePct(mPct);
            }
        });

        final Class<?> brightnessUtils = findClassIfExists("com.android.systemui.controlcenter.policy.BrightnessUtils");
        hookAllMethods("com.android.systemui.controlcenter.policy.MiuiBrightnessController", "onChanged", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                int pctTag = 0;
                if (mPct != null && mPct.getTag() != null) {
                    pctTag = (int) mPct.getTag();
                }
                if (pctTag == 0 || mPct == null) return;
                int currentLevel = (int) param.getArgs()[3];
                if (brightnessUtils != null) {
                    int maxLevel = (int) getStaticObjectField(brightnessUtils, "GAMMA_SPACE_MAX");
                    mPct.setText(((currentLevel * 100) / maxLevel) + "%");
                }
            }
        });
    }
}
