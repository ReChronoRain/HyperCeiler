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
package com.sevtinge.hyperceiler.libhook.rules.camera;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

;

public class MaxScreenBrightness extends BaseHook {
    private Method mGetHaloBrightnessMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mGetHaloBrightnessMethod = requiredMember("GetHaloBrightness", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingNumbers(0, -1.0f, 204)
                                .paramTypes(int.class)
                        )
                ).singleOrThrow(() -> new IllegalStateException("MaxScreenBrightness: Cannot found getHaloBrightness()"));
                return methodData;
            }
        });
        return true;
    }

    @Override
    public void init() {

        XposedLog.d(TAG, getPackageName(), "getHaloBrightness() method is " + mGetHaloBrightnessMethod);
        hookMethod(mGetHaloBrightnessMethod, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                Activity activity = (Activity) callMethod(param.getThisObject(), "getActivity");
                setScreenBrightnessToMax(activity);
            }

        });

        findAndHookMethod(Window.class, "setAttributes", WindowManager.LayoutParams.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) param.getArgs()[0];
                layoutParams.screenBrightness = 1.0f;
                param.getArgs()[0] = layoutParams;
            }
        });


        findAndHookMethod("com.android.camera.ActivityBase", "onCreate", Bundle.class, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                Activity activity = (Activity) param.getThisObject();
                setScreenBrightnessToMax(activity);
            }
        });

        findAndHookMethod("com.android.camera.ActivityBase", "onStart", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                Activity activity = (Activity) param.getThisObject();
                setScreenBrightnessToMax(activity);
            }
        });

        findAndHookMethod("com.android.camera.ActivityBase", "onRestart", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                Activity activity = (Activity) param.getThisObject();
                setScreenBrightnessToMax(activity);
            }
        });

        findAndHookMethod("com.android.camera.ActivityBase", "onResume", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                Activity activity = (Activity) param.getThisObject();
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
