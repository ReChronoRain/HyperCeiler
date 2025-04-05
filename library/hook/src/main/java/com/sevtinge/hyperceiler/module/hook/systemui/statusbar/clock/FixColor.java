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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.clock;

import android.graphics.Color;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.MathUtils;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class FixColor extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.systemui.controlcenter.phone.widget.NotificationShadeFakeStatusBarClock", "updateHeaderColor", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) {
                Color bigTimeColor = (Color) XposedHelpers.getObjectField(param.thisObject, "bigTimeColor");
                Color tintColor = Color.valueOf((int) XposedHelpers.getObjectField(param.thisObject, "mTint"));
                Color lightColor = Color.valueOf((int) XposedHelpers.getObjectField(param.thisObject, "mLightColor"));
                Color darkColor = Color.valueOf((int) XposedHelpers.getObjectField(param.thisObject, "mDarkColor"));
                float whiteFraction = (float) XposedHelpers.getObjectField(param.thisObject, "mWhiteFraction");
                ArrayList<?> areas = (ArrayList<?>) XposedHelpers.getObjectField(param.thisObject, "mAreas");
                float darkIntensity = (float) XposedHelpers.getObjectField(param.thisObject, "mDarkIntensity");
                boolean useTint = (boolean) XposedHelpers.getObjectField(param.thisObject, "mUseTint");
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
                Object getBigTime = XposedHelpers.callMethod(param.thisObject, "getBigTime");
                XposedHelpers.callMethod(getBigTime, "onDarkChanged", areas, darkIntensity, inTintColor, inLightColor, inDarkColor, useTint);
                param.setResult(null);
            }
        });
    }
}
