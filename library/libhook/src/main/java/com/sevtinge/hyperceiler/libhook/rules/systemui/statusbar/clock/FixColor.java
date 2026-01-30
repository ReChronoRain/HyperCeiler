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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.clock;

import android.graphics.Color;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.MathUtils;

import java.util.ArrayList;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class FixColor extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.systemui.controlcenter.phone.widget.NotificationShadeFakeStatusBarClock", "updateHeaderColor", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                Color bigTimeColor = (Color) getObjectField(param.getThisObject(), "bigTimeColor");
                Color tintColor = Color.valueOf((int) getObjectField(param.getThisObject(), "mTint"));
                Color lightColor = Color.valueOf((int) getObjectField(param.getThisObject(), "mLightColor"));
                Color darkColor = Color.valueOf((int) getObjectField(param.getThisObject(), "mDarkColor"));
                float whiteFraction = (float) getObjectField(param.getThisObject(), "mWhiteFraction");
                ArrayList<?> areas = (ArrayList<?>) getObjectField(param.getThisObject(), "mAreas");
                float darkIntensity = (float) getObjectField(param.getThisObject(), "mDarkIntensity");
                boolean useTint = (boolean) getObjectField(param.getThisObject(), "mUseTint");
                int inTintColor = Color.argb(
                    MathUtils.lerp(tintColor.alpha(), bigTimeColor.alpha(), whiteFraction),
                    MathUtils.lerp(tintColor.red(), bigTimeColor.red(), whiteFraction),
                    MathUtils.lerp(tintColor.green(), bigTimeColor.green(), whiteFraction),
                    MathUtils.lerp(tintColor.blue(), bigTimeColor.blue(), whiteFraction)
                );
                int inLightColor = Color.argb(
                    MathUtils.lerp(lightColor.alpha(), bigTimeColor.alpha(), whiteFraction),
                    MathUtils.lerp(lightColor.red(), bigTimeColor.red(), whiteFraction),
                    MathUtils.lerp(lightColor.green(), bigTimeColor.green(), whiteFraction),
                    MathUtils.lerp(lightColor.blue(), bigTimeColor.blue(), whiteFraction)
                );
                int inDarkColor = Color.argb(
                    MathUtils.lerp(darkColor.alpha(), bigTimeColor.alpha(), whiteFraction),
                    MathUtils.lerp(darkColor.red(), bigTimeColor.red(), whiteFraction),
                    MathUtils.lerp(darkColor.green(), bigTimeColor.green(), whiteFraction),
                    MathUtils.lerp(darkColor.blue(), bigTimeColor.blue(), whiteFraction)
                );
                Object getBigTime = callMethod(param.getThisObject(), "getBigTime");
                callMethod(getBigTime, "onDarkChanged", areas, darkIntensity, inTintColor, inLightColor, inDarkColor, useTint);
                param.setResult(null);
            }
        });
    }
}
