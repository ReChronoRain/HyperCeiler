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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.hook.various.clipboard;

import android.content.Context;

import com.hchen.hooktool.HCBase;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.utils.SystemPropTool;

import java.util.Arrays;
import java.util.List;

public class NewUnlockIme extends HCBase implements LoadInputMethodDex.OnInputMethodDexLoad {
    private boolean shouldHook = false;

    private static final String[] miuiImeList = new String[]{
            "com.iflytek.inputmethod.miui",
            "com.sohu.inputmethod.sogou.xiaomi",
            "com.baidu.input_mi",
            "com.miui.catcherpatch"
    };

    private int navBarColor = 0;

    @Override
    public void load(ClassLoader classLoader) {
        fakeSupportImeList(classLoader);
        notDeleteNotSupportIme("com.miui.inputmethod.InputMethodBottomManager$MiuiSwitchInputMethodListener", classLoader);
        if (!shouldHook) return;
        Class<?> InputMethodBottomManager = findClass("com.miui.inputmethod.InputMethodBottomManager", classLoader);
        if (InputMethodBottomManager != null) {
            fakeIsSupportIme(InputMethodBottomManager);
            fakeIsXiaoAiEnable(InputMethodBottomManager);
        } else {
            logE(TAG, "Class not found: com.miui.inputmethod.InputMethodBottomManager");
        }
    }

    @Override
    public void init() {
        if (SystemPropTool.getProp("ro.miui.support_miui_ime_bottom", "0").equals("1")) {
            startHook();
        }
    }

    private void startHook() {
        // 检查是否为小米定制输入法
        if (Arrays.stream(miuiImeList).anyMatch(s -> s.equals(loadPackageParam.packageName))) return;
        shouldHook = true;
        Class<?> sInputMethodServiceInjector = findClass("android.inputmethodservice.InputMethodServiceInjector");
        if (sInputMethodServiceInjector == null)
            sInputMethodServiceInjector = findClass("android.inputmethodservice.InputMethodServiceStubImpl");
        if (sInputMethodServiceInjector != null) {
            fakeIsSupportIme(sInputMethodServiceInjector);
            fakeIsXiaoAiEnable(sInputMethodServiceInjector);
            setPhraseBgColor(sInputMethodServiceInjector);
        } else {
            logE(TAG, "Class not found: InputMethodServiceInjector");
        }

        notDeleteNotSupportIme("android.inputmethodservice.InputMethodServiceInjector$MiuiSwitchInputMethodListener", classLoader);
    }

    /**
     * 跳过包名检查，直接开启输入法优化
     */
    private void fakeIsSupportIme(Class<?> clazz) {
        setStaticField(clazz, "sIsImeSupport", 1);
        hookMethod(clazz, "isImeSupport", Context.class, returnResult(true));
    }

    /**
     * 小爱语音输入按钮失效修复
     */
    private void fakeIsXiaoAiEnable(Class<?> clazz) {
        hookMethod(clazz, "isXiaoAiEnable", returnResult(false));
    }

    /**
     * 在适当的时机修改抬高区域背景颜色
     */
    private void setPhraseBgColor(Class<?> clazz) {
        hookMethod("com.android.internal.policy.PhoneWindow",
                "setNavigationBarColor", int.class,
                new IHook() {
                    @Override
                    public void after() {
                        if ((int) getArg(0) == 0) return;
                        navBarColor = (int) getArg(0);
                        customizeBottomViewColor(clazz);
                    }
                }
        );

        hookAllMethod(clazz, "addMiuiBottomView",
                new IHook() {
                    @Override
                    public void after() {
                        customizeBottomViewColor(clazz);
                    }
                }
        );
    }

    /**
     * 将导航栏颜色赋值给输入法优化的底图
     */
    private void customizeBottomViewColor(Class<?> clazz) {
        if (navBarColor != 0) {
            int color = -0x1 - navBarColor;
            callStaticMethod(clazz, "customizeBottomViewColor", true, navBarColor, color | -0x1000000, color | 0x66000000);
        }
    }

    /**
     * 针对A10的修复切换输入法列表
     */
    private void notDeleteNotSupportIme(String className, ClassLoader classLoader) {
        hookMethod(className, classLoader, "deleteNotSupportIme", doNothing());
    }

    /**
     * 使切换输入法界面显示第三方输入法
     */
    private void fakeSupportImeList(ClassLoader classLoader) {
        hookMethod("com.miui.inputmethod.InputMethodBottomManager", classLoader, "getSupportIme",
                new IHook() {
                    @Override
                    public void before() {
                        List<?> mEnabledInputMethodList = (List<?>) callMethod(getField(getStaticField(
                                "com.miui.inputmethod.InputMethodBottomManager", classLoader,
                                "sBottomViewHelper"), "mImm"), "getEnabledInputMethodList");
                        setResult(mEnabledInputMethodList);
                    }
                }
        );
    }
}
