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
package com.sevtinge.hyperceiler.module.hook.home.title;

import static com.github.kyuubiran.ezxhelper.ClassUtils.loadClass;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import com.github.kyuubiran.ezxhelper.ClassUtils;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

public class AnimParamCustom extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.home.recents.util.RectFSpringAnim", "setAnimParamByType", "com.miui.home.recents.util.RectFSpringAnim$AnimType", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                Class<?> clazzRectFSpringAnimRectAnimType =
                        findClassIfExists("com.miui.home.recents.util.RectFSpringAnim$RectAnimType");

                if (clazzRectFSpringAnimRectAnimType == null) {
                    AndroidLogUtils.logI(TAG, "clazzRectFSpringAnimRectAnimType is not found!");
                    return newSetAnim(param);
                } else {
                    return oldSetAnim(param, clazzRectFSpringAnimRectAnimType);
                }
            }
        });
    }

    private Object newSetAnim(XC_MethodHook.MethodHookParam param) throws ClassNotFoundException {
        // 先写个框架，太多了，懒得写了
        // 以下是官方写法
        Enum animType = (Enum) param.args[0];
        float gestureAnimMagicSpeed =
                (float) callStaticMethod(loadClass("com.miui.home.recents.util.Utilities", lpparam.classLoader), "getGestureAnimMagicSpeed");
        AndroidLogUtils.logI(TAG, "setAnimParamByType = " + animType);
        setObjectField(param.thisObject, "mLastAminType", param.args[0]);

        float[] fArr;
        float[] fArr2;
        float[] fArr3;
        float[] fArr4 = {1.0f, 0.35f, 0.0f};
        float[] fArr5 = {1.0f, 0.25f, 0.0f};

        switch (animType.name()) {
            case "OPEN_FROM_HOME":
                fArr = new float[]{0.86f, 0.15f, 3.0f};
                fArr2 = new float[]{0.86f, 0.15f, 3.0f};
                fArr3 = new float[]{0.86f, 0.15f, 0.0f};
                fArr4 = new float[]{0.86f, 0.15f, 0.0f};
                fArr5 = new float[]{0.86f, 0.15f, 0.0f};
                setAnimParam(param, fArr, fArr2, fArr3, fArr4, fArr5);

            case "OPEN_FROM_RECENTS":
                float f1 = gestureAnimMagicSpeed * 0.32f;
                float f2 = gestureAnimMagicSpeed * 0.3f;
                fArr = new float[]{1.1f, f1, 0.0f};
                fArr2 = new float[]{1.1f, f1, 0.0f};
                fArr3 = new float[]{1.1f, f1, 0.0f};
                fArr4 = new float[]{1.0f, f2, 0.0f};
                setAnimParam(param, fArr, fArr2, fArr3, fArr4, fArr5);

            case "CLOSE_TO_RECENTS":
                fArr = new float[]{1.1f, 0.3f, 0.0f};
                fArr2 = new float[]{1.1f, 0.35f, 0.0f};
                fArr3 = new float[]{1.1f, 0.35f, 0.0f};
                setAnimParam(param, fArr, fArr2, fArr3, fArr4, fArr5);

            case "CLOSE_TO_HOME":
            case "CLOSE_TO_HOME_CENTER":
            case "CLOSE_TO_ELEMENT":
                callMethod(param.thisObject, "setEndEnable", true);
                fArr = new float[]{0.8f, 0.4f, 0.0f};
                fArr2 = new float[]{0.8f, 0.4f, 0.0f};
                fArr3 = new float[]{1.0f, 0.4f, 0.0f};
                setAnimParam(param, fArr, fArr2, fArr3, fArr4, fArr5);

            case "CLOSE_FROM_FEED":
            case "APP_TO_APP":
                callMethod(param.thisObject, "setEndEnable", true);
                fArr = new float[]{1.0f, 0.405f, 0.0f};
                fArr2 = new float[]{1.0f, 0.405f, 0.0f};
                fArr3 = new float[]{1.0f, 0.405f, 0.0f};
                setAnimParam(param, fArr, fArr2, fArr3, fArr4, fArr5);

            case "START_FIRST_TASK":
                callMethod(param.thisObject, "setEndEnable", true);
                fArr = new float[]{0.9f, 0.3f, 0.0f};
                fArr2 = new float[]{0.9f, 0.3f, 0.0f};
                fArr3 = new float[]{0.9f, 0.3f, 0.0f};
                setAnimParam(param, fArr, fArr2, fArr3, fArr4, fArr5);

            case "CLOSE_TO_DRAG":
                callMethod(param.thisObject, "setEndEnable", true);
                fArr = new float[]{0.95f, 0.38f, 0.0f};
                fArr2 = new float[]{0.9f, 0.4f, 0.0f};
                fArr3 = new float[]{1.0f, 0.35f, 0.0f};
                setAnimParam(param, fArr, fArr2, fArr3, fArr4, fArr5);

            case "CLOSE_TO_WORLD_CIRCLE":
            case "CIRCLE_SMALL_VIEW":
                fArr = new float[]{0.8f, 0.4f, 0.0f};
                fArr2 = new float[]{0.8f, 0.4f, 0.0f};
                fArr3 = new float[]{1.0f, 0.35f, 0.0f};
                setAnimParam(param, fArr, fArr2, fArr3, fArr4, fArr5);

            case "CLOSE_TO_SMALL_WINDOW":
            case "CLOSE_TO_SMALL_WINDOW_ROTATE":
                fArr = new float[]{1.0f, 0.45f, 0.0f};
                fArr2 = new float[]{0.93f, 0.55f, 0.0f};
                fArr3 = new float[]{1.0f, 0.45f, 0.0f};
                setAnimParam(param, fArr, fArr2, fArr3, fArr4, fArr5);
        }

        return null;
    }

    private static void setAnimParam(XC_MethodHook.MethodHookParam param, float[] fArr, float[] fArr2, float[] fArr3, float[] fArr4, float[] fArr5) {
        callMethod(param.thisObject, "setAnimParam", "centerX", fArr[0], fArr[1], fArr[2]);
        callMethod(param.thisObject, "setAnimParam", "centerY", fArr[0], fArr[1], fArr[2]);
        callMethod(param.thisObject, "setAnimParam", "Width", fArr2[0], fArr2[1], fArr2[2]);
        callMethod(param.thisObject, "setAnimParam", "Height", fArr2[0], fArr2[1], fArr2[2]);
        callMethod(param.thisObject, "setAnimParam", "RadiusRatio", fArr3[0], fArr3[1], fArr3[2]);
        callMethod(param.thisObject, "setAnimParam", "Ratio", fArr3[0], fArr3[1], fArr3[2]);
        callMethod(param.thisObject, "setAnimParam", "Alpha", fArr4[0], fArr4[1], fArr4[2]);
        callMethod(param.thisObject, "setAnimParam", "SurfaceExtAlpha", fArr5[0], fArr5[1], fArr5[2]);
    }

    private Object oldSetAnim(XC_MethodHook.MethodHookParam param, Class<?> clazzRectFSpringAnimRectAnimType) throws NoSuchFieldException {
        Object RECT_CENTERX = ClassUtils.getStaticObjectOrNull(clazzRectFSpringAnimRectAnimType, "RECT_CENTER_X");
        Object RECT_CENTERY = ClassUtils.getStaticObjectOrNull(clazzRectFSpringAnimRectAnimType, "RECT_CENTER_Y");
        Object RECT_WIDTH = ClassUtils.getStaticObjectOrNull(clazzRectFSpringAnimRectAnimType, "RECT_WIDTH");
        Object RECT_RATIO = ClassUtils.getStaticObjectOrNull(clazzRectFSpringAnimRectAnimType, "RECT_RATIO");
        Object RADIUS = ClassUtils.getStaticObjectOrNull(clazzRectFSpringAnimRectAnimType, "RADIUS");
        Object ALPHA = ClassUtils.getStaticObjectOrNull(clazzRectFSpringAnimRectAnimType, "ALPHA");
        Enum animType = (Enum) param.args[0];
        AndroidLogUtils.logI(TAG, "setAnimParamByType = " + animType);
        setObjectField(param.thisObject, "mLastAminType", param.args[0]);
        switch (animType.name()) {
            case "BREAK_OPEN": // 1/1

            case "CLOSE_TO_DRAG":// 2/9
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERX, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERX_9", 900) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERX_9", 200) / 1000));// 0.9 0.2
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERY, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERY_9", 900) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERY_9", 200) / 1000));// 0.9 0.2
                callMethod(param.thisObject, "setAnimParam", RECT_WIDTH, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_WIDTH_9", 900) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_WIDTH_9", 200) / 1000));// 0.9 0.2
                callMethod(param.thisObject, "setAnimParam", RECT_RATIO, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_RATIO_9", 900) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_RATIO_9", 200) / 1000));// 0.9 0.2
                callMethod(param.thisObject, "setAnimParam", RADIUS, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RADIUS_9", 900) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RADIUS_9", 200) / 1000));// 0.9 0.2
                callMethod(param.thisObject, "setAnimParam", ALPHA, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_ALPHA_9", 900) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_ALPHA_9", 200) / 1000));// 0.9 0.2
                return null;

            case "OPEN_FROM_HOME":// 3/2
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERX, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERX_2", 1000) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERX_2", 330) / 1000));// 1 0.33
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERY, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERY_2", 1000) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERY_2", 330) / 1000));// 1 0.33
                callMethod(param.thisObject, "setAnimParam", RECT_WIDTH, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_WIDTH_2", 1000) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_WIDTH_2", 330) / 1000));// 1 0.33
                callMethod(param.thisObject, "setAnimParam", RECT_RATIO, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_RATIO_2", 1000) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_RATIO_2", 330) / 1000));// 1 0.33
                callMethod(param.thisObject, "setAnimParam", RADIUS, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RADIUS_2", 1000) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RADIUS_2", 330) / 1000));// 1 0.33
                callMethod(param.thisObject, "setAnimParam", ALPHA, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_ALPHA_2", 1000) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_ALPHA_2", 200) / 1000));// 1 0.2
                return null;

            case "OPEN_FROM_RECENTS":// 4/3
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERX, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERX_3", 900) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERX_3", 270) / 1000));// 0.9 0.27
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERY, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERY_3", 900) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERY_3", 270) / 1000));// 0.9 0.27
                callMethod(param.thisObject, "setAnimParam", RECT_WIDTH, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_WIDTH_3", 990) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_WIDTH_3", 360) / 1000));// 0.99 0.36
                callMethod(param.thisObject, "setAnimParam", RECT_RATIO, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_RATIO_3", 990) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_RATIO_3", 360) / 1000));// 0.99 0.36
                return null;

            case "CLOSE_TO_RECENTS":// 5/4
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERX, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERX_4", 900) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERX_4", 400) / 1000));// 0.9 0.4
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERY, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERY_4", 900) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERY_4", 400) / 1000));// 0.9 0.4
                callMethod(param.thisObject, "setAnimParam", RECT_WIDTH, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_WIDTH_4", 950) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_WIDTH_4", 400) / 1000));// 0.9 0.4
                callMethod(param.thisObject, "setAnimParam", RECT_RATIO, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_RATIO_4", 900) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_RATIO_4", 350) / 1000));// 0.97 0.35
                callMethod(param.thisObject, "setAnimParam", RADIUS, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RADIUS_4", 990) / 900), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RADIUS_4", 400) / 1000));// 0.9 0.4
                callMethod(param.thisObject, "setAnimParam", ALPHA, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_ALPHA_4", 990) / 900), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_ALPHA_4", 400) / 1000));// 0.9 0.4
                return null;

            case "CLOSE_TO_HOME":// 6/5
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERX, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERX_5", 880) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERX_5", 460) / 1000));// 0.88 0.46
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERY, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERY_5", 880) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERY_5", 460) / 1000));// 0.88 0.46
                callMethod(param.thisObject, "setAnimParam", RECT_WIDTH, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_WIDTH_5", 850) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_WIDTH_5", 460) / 1000));// 0.85 0.46
                callMethod(param.thisObject, "setAnimParam", RECT_RATIO, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_RATIO_5", 1000) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_RATIO_5", 350) / 1000));// 1 0.35
                callMethod(param.thisObject, "setAnimParam", RADIUS, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RADIUS_5", 1000) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RADIUS_5", 350) / 1000));// 1 0.35
                callMethod(param.thisObject, "setAnimParam", ALPHA, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_ALPHA_5", 1000) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_ALPHA_5", 400) / 1000));// 1 0.4
                return null;

            case "CLOSE_FROM_FEED":// 7/6
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERX, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERX_6", 950) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERX_6", 378) / 1000));// 0.95 0.378
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERY, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERY_6", 950) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERY_6", 378) / 1000));// 0.95 0.378
                callMethod(param.thisObject, "setAnimParam", RECT_WIDTH, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_WIDTH_6", 900) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_WIDTH_6", 405) / 1000));// 0.9 0.405
                callMethod(param.thisObject, "setAnimParam", RECT_RATIO, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_RATIO_6", 950) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_RATIO_6", 333) / 1000));// 0.95 0.333
                callMethod(param.thisObject, "setAnimParam", RADIUS, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RADIUS_6", 990) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RADIUS_6", 180) / 1000));// 0.99 0.18
                callMethod(param.thisObject, "setAnimParam", ALPHA, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_ALPHA_6", 990) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_ALPHA_6", 378) / 1000));// 0.99 0.378
                return null;

            case "APP_TO_APP":// 8/7
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERX, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERX_7", 990) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERX_7", 315) / 1000));// 0.99 0.315
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERY, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERY_7", 990) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERY_7", 315) / 1000));// 0.99 0.315
                callMethod(param.thisObject, "setAnimParam", RECT_WIDTH, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_WIDTH_7", 990) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_WIDTH_7", 315) / 1000));// 0.99 0.315
                callMethod(param.thisObject, "setAnimParam", RECT_RATIO, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_RATIO_7", 990) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_RATIO_7", 315) / 1000));// 0.99 0.315
                return null;

            case "START_FIRST_TASK":// 9/8
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERX, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERX_8", 990) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERX_8", 180) / 1000));// 0.99 0.18
                callMethod(param.thisObject, "setAnimParam", RECT_CENTERY, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_CENTERY_8", 990) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_CENTERY_8", 180) / 1000));// 0.99 0.18
                callMethod(param.thisObject, "setAnimParam", RECT_WIDTH, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_WIDTH_8", 990) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_WIDTH_8", 180) / 1000));// 0.99 0.18
                callMethod(param.thisObject, "setAnimParam", RECT_RATIO, ((float) mPrefsMap.getInt("home_title_custom_anim_param_damping_RECT_RATIO_8", 990) / 1000), ((float) mPrefsMap.getInt("home_title_custom_anim_param_stiffness_RECT_RATIO_8", 180) / 1000));// 0.99 0.18
                return null;

            default:
                return null;
        }
    }
}
