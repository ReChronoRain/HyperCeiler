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
package com.sevtinge.hyperceiler.module.hook.camera;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public class MaxScreenBrightness extends BaseHook {

    @Override
    public void init() throws NoSuchMethodException {
        
        MethodData methodData = DexKit.getDexKitBridge().findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                        .usingNumbers(0, 8208, -1.0f, 256, 204)
                )
        ).singleOrThrow(() -> new IllegalStateException("MaxScreenBrightness: Cannot found getHaloBrightness()"));
        Method method = methodData.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "getHaloBrightness() method is " + method);
        hookMethod(method, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) XposedHelpers.callMethod(param.thisObject, "getActivity");
                setScreenBrightnessToMax(activity);
            }

        });

        findAndHookMethod(Window.class, "setAttributes", WindowManager.LayoutParams.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) param.args[0];
                layoutParams.screenBrightness = 1.0f;
                param.args[0] = layoutParams;
            }
        });


        findAndHookMethod("com.android.camera.ActivityBase", "onCreate", Bundle.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                setScreenBrightnessToMax(activity);
            }
        });

        findAndHookMethod("com.android.camera.ActivityBase", "onStart", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                setScreenBrightnessToMax(activity);
            }
        });

        findAndHookMethod("com.android.camera.ActivityBase", "onRestart", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                setScreenBrightnessToMax(activity);
            }
        });

        findAndHookMethod("com.android.camera.ActivityBase", "onResume", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                setScreenBrightnessToMax(activity);
            }
        });

    }

    private void setScreenBrightnessToMax(Activity activity) {
        Window window = activity.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        window.setAttributes(layoutParams);

    }

}
