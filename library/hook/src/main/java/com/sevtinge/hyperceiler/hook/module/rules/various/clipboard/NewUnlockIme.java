/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.rules.various.clipboard;

import static com.sevtinge.hyperceiler.hook.utils.InvokeUtils.callMethod;
import static com.sevtinge.hyperceiler.hook.utils.InvokeUtils.getField;
import static com.sevtinge.hyperceiler.hook.utils.InvokeUtils.getStaticField;
import static com.sevtinge.hyperceiler.hook.utils.InvokeUtils.setStaticField;
import static com.sevtinge.hyperceiler.hook.utils.KotlinXposedHelperKt.callStaticMethod;
import static com.sevtinge.hyperceiler.hook.utils.PropUtils.getProp;

import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;

import android.content.Context;

import androidx.annotation.NonNull;

import com.hchen.hooktool.HCBase;
import com.hchen.hooktool.hook.IHook;
import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 解除全面屏键盘优化限制
 */
public class NewUnlockIme extends BaseHook {
    private static final String TAG = "NewUnlockIme";
    private static boolean shouldHook = false;
    private static int navBarColor = 0;
    private static final String[] miuiImeList = new String[]{
        "com.iflytek.inputmethod.miui",
        "com.sohu.inputmethod.sogou.xiaomi",
        "com.baidu.input_mi",
        "com.miui.catcherpatch"
    };

    public void unlock(@NonNull ClassLoader classLoader) {
        fakeSupportImeList(classLoader);
        // notDeleteNotSupportIme(findClass("com.miui.inputmethod.InputMethodBottomManager$MiuiSwitchInputMethodListener", classLoader));

        if (!shouldHook) return;

        Class<?> InputMethodBottomManager = findClassIfExists("com.miui.inputmethod.InputMethodBottomManager", classLoader);
        if (InputMethodBottomManager != null) {
            fakeSupportIme(InputMethodBottomManager);
            fakeXiaoAiEnable(InputMethodBottomManager);
        } else {
            logE(TAG, "Not found class: com.miui.inputmethod.InputMethodBottomManager");
        }
    }

    @Override
    public void init() {
        if (getProp("ro.miui.support_miui_ime_bottom", "0").equals("1")) {
            startHook(lpparam);
        }
    }

    private void startHook(XC_LoadPackage.LoadPackageParam lpparam) {
        // 检查是否为小米定制输入法
        if (Arrays.stream(miuiImeList).anyMatch(s -> s.equals(lpparam)))
            return;

        shouldHook = true;

        Class<?> sInputMethodServiceInjector = findClassIfExists("android.inputmethodservice.InputMethodServiceInjector");
        if (sInputMethodServiceInjector == null)
            sInputMethodServiceInjector = findClassIfExists("android.inputmethodservice.InputMethodServiceStubImpl");

        if (sInputMethodServiceInjector != null) {
            fakeSupportIme(sInputMethodServiceInjector);
            fakeXiaoAiEnable(sInputMethodServiceInjector);
            setPhraseBgColor(sInputMethodServiceInjector);
        } else {
            logE(TAG, "Not found class: android.inputmethodservice.InputMethodServiceStubImpl");
        }

        // notDeleteNotSupportIme(findClass("android.inputmethodservice.InputMethodServiceInjector$MiuiSwitchInputMethodListener"));
    }

    /**
     * 跳过包名检查，直接开启输入法优化
     */
    private static void fakeSupportIme(@NonNull Class<?> clazz) {
        setStaticField(clazz, "sIsImeSupport", 1);
        findAndHookMethod(clazz, "isImeSupport", Context.class, MethodHook.returnConstant(true));
    }

    /**
     * 小爱语音输入按钮失效修复
     */
    private static void fakeXiaoAiEnable(@NonNull Class<?> clazz) {
        findAndHookMethod(clazz, "isXiaoAiEnable", MethodHook.returnConstant(false));
    }

    /**
     * 在适当的时机修改抬高区域背景颜色
     */
    private void setPhraseBgColor(@NonNull Class<?> clazz) {
        findAndHookMethod("com.android.internal.policy.PhoneWindow",
            "setNavigationBarColor",
            int.class,
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    if ((int) param.args[0] == 0) return;

                    navBarColor = (int) param.args[0];
                    customizeBottomViewColor(clazz);
                }
            }
        );

        hookAllMethods(clazz,
            "addMiuiBottomView",
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    customizeBottomViewColor(clazz);
                }
            }
        );
    }

    /**
     * 将导航栏颜色赋值给输入法优化的底图
     */
    private static void customizeBottomViewColor(@NonNull Class<?> clazz) {
        if (navBarColor != 0) {
            int color = -0x1 - navBarColor;
            callStaticMethod(clazz, "customizeBottomViewColor", true, navBarColor, color | -0x1000000, color | 0x66000000);
        }
    }

    /**
     * 针对 A10 的修复切换输入法列表
     */
    /*private static void notDeleteNotSupportIme(@NonNull Class<?> clazz) {
        if (existsMethod(clazz, "deleteNotSupportIme")) {
            hookMethod(clazz, "deleteNotSupportIme", doNothing());
        }
    }*/

    /**
     * 使切换输入法界面显示第三方输入法
     */
    private void fakeSupportImeList(@NonNull ClassLoader classLoader) {
        /*findAndHookMethod("com.miui.inputmethod.InputMethodBottomManager", classLoader,
            "getSupportIme",
            new MethodHook() {
                @Override
                public void before(MethodHookParam param) throws Throwable {
                    Object sBottomViewHelper = getStaticObjectField(findClassIfExists("com.miui.inputmethod.InputMethodBottomManager"), "sBottomViewHelper");
                    Object mImm = getObjectField(param.thisObject, "mImm");
                    List<?> list = (List<?>) callMethod(getStaticObjectField(findClassIfExists("com.miui.inputmethod.InputMethodBottomManager"), "sBottomViewHelper"),
                            "mImm"
                        ),
                        "getEnabledInputMethodList"
                    );
                    param.setResult(list);
                }
            }
        );*/
    }
}
