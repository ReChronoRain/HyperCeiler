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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.clock;

import android.graphics.Color;
import android.widget.Toast;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.ui.fragment.settings.development.DevelopmentDebugInfoFragment;
import com.sevtinge.hyperceiler.utils.MathUtils;

import java.util.ArrayList;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FixColor extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.systemui.controlcenter.phone.widget.NotificationShadeFakeStatusBarClock", "updateHeaderColor", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) {
                /*Object bigTime = XposedHelpers.getObjectField(findClassIfExists("com.android.systemui.controlcenter.phone.widget.NotificationShadeFakeStatusBarClock"), "bigTime");
                Color bigTimeColor = (Color) XposedHelpers.getObjectField(findClassIfExists("com.android.systemui.controlcenter.phone.widget.NotificationShadeFakeStatusBarClock"), "bigTimeColor");
                Color tintColor = (Color) XposedHelpers.getObjectField(findClassIfExists("com.android.systemui.controlcenter.phone.widget.NotificationShadeFakeStatusBarClock"), "mTint");
                Color lightColor = (Color) XposedHelpers.getObjectField(findClassIfExists("com.android.systemui.controlcenter.phone.widget.NotificationShadeFakeStatusBarClock"), "mLightColor");
                Color darkColor = (Color) XposedHelpers.getObjectField(findClassIfExists("com.android.systemui.controlcenter.phone.widget.NotificationShadeFakeStatusBarClock"), "mDarkColor");
                float whiteFraction = (float) XposedHelpers.getObjectField(findClassIfExists("com.android.systemui.controlcenter.phone.widget.NotificationShadeFakeStatusBarClock"), "mWhiteFraction");
                ArrayList<?> areas = (ArrayList<?>) XposedHelpers.getObjectField(findClassIfExists("com.android.systemui.controlcenter.phone.widget.NotificationShadeFakeStatusBarClock"), "mAreas");
                float darkIntensity = (float) XposedHelpers.getObjectField(findClassIfExists("com.android.systemui.controlcenter.phone.widget.NotificationShadeFakeStatusBarClock"), "mDarkIntensity");
                boolean useTint = (boolean) XposedHelpers.getObjectField(findClassIfExists("com.android.systemui.controlcenter.phone.widget.NotificationShadeFakeStatusBarClock"), "mUseTint");
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
                logI(String.valueOf(darkIntensity));
                if (whiteFraction <= 0.01f) XposedHelpers.callMethod(bigTime, "onDarkChanged", areas, darkIntensity, inTintColor, inLightColor, inDarkColor, useTint);*/
                param.setResult(null);
            }
        });
    }
}
