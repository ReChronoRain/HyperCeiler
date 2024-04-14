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
package com.sevtinge.hyperceiler.module.hook.various;

import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.getProp;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class UnlockIme extends BaseHook {

    private final String[] miuiImeList = new String[]{
        "com.iflytek.inputmethod.miui",
        "com.sohu.inputmethod.sogou.xiaomi",
        "com.baidu.input_mi",
        "com.miui.catcherpatch"
    };

    private int navBarColor = 0;

    @Override
    public void init() throws NoSuchMethodException {
        if (getProp("ro.miui.support_miui_ime_bottom", "0").equals("1")) {
            startHook(lpparam);
        }
    }

    private void startHook(XC_LoadPackage.LoadPackageParam param) {
        // 检查是否为小米定制输入法
        boolean isNonCustomize = true;
        for (String isMiui : miuiImeList) {
            if (isMiui.equals(param.packageName)) {
                isNonCustomize = false;
                break;
            }
        }
        if (isNonCustomize) {
            Class<?> sInputMethodServiceInjector = findClassIfExists("android.inputmethodservice.InputMethodServiceInjector");
            if (sInputMethodServiceInjector == null)
                sInputMethodServiceInjector = findClassIfExists("android.inputmethodservice.InputMethodServiceStubImpl");
            if (sInputMethodServiceInjector != null) {
                hookSIsImeSupport(sInputMethodServiceInjector);
                hookIsXiaoAiEnable(sInputMethodServiceInjector);
                setPhraseBgColor(sInputMethodServiceInjector);
            } else {
                logE(TAG, "Class not found: InputMethodServiceInjector");
            }
        }

        hookDeleteNotSupportIme("android.inputmethodservice.InputMethodServiceInjector$MiuiSwitchInputMethodListener",
            param.classLoader);

        // 获取常用语的ClassLoader
        boolean finalIsNonCustomize = isNonCustomize;
        findAndHookMethod("android.inputmethodservice.InputMethodModuleManager",
            "loadDex", ClassLoader.class, String.class,
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    getSupportIme((ClassLoader) param.args[0]);
                    hookDeleteNotSupportIme("com.miui.inputmethod.InputMethodBottomManager$MiuiSwitchInputMethodListener", (ClassLoader) param.args[0]);
                    if (finalIsNonCustomize) {
                        Class<?> InputMethodBottomManager = findClassIfExists("com.miui.inputmethod.InputMethodBottomManager", (ClassLoader) param.args[0]);
                        if (InputMethodBottomManager != null) {
                            hookSIsImeSupport(InputMethodBottomManager);
                            hookIsXiaoAiEnable(InputMethodBottomManager);
                        } else {
                            logE(TAG, "Class not found: com.miui.inputmethod.InputMethodBottomManager");
                        }
                    }
                }
            }
        );
    }

    /**
     * 跳过包名检查，直接开启输入法优化
     *
     * @param clazz 声明或继承字段的类
     */
    private void hookSIsImeSupport(Class<?> clazz) {
        try {
            XposedHelpers.setStaticObjectField(clazz, "sIsImeSupport", 1);
        } catch (Throwable throwable) {
            logE(TAG, "Hook field sIsImeSupport: " + throwable);
        }
    }

    /**
     * 小爱语音输入按钮失效修复
     *
     * @param clazz 声明或继承方法的类
     */
    private void hookIsXiaoAiEnable(Class<?> clazz) {
        try {
            hookAllMethods(clazz, "isXiaoAiEnable", MethodHook.returnConstant(false));
        } catch (Throwable throwable) {
            logE(TAG, "Hook method isXiaoAiEnable: " + throwable);
        }
    }

    /**
     * 在适当的时机修改抬高区域背景颜色
     *
     * @param clazz 声明或继承字段的类
     */
    private void setPhraseBgColor(Class<?> clazz) {
        try {
            findAndHookMethod("com.android.internal.policy.PhoneWindow",
                "setNavigationBarColor", int.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        if ((int) param.args[0] == 0) return;
                        navBarColor = (int) param.args[0];
                        customizeBottomViewColor(clazz);
                    }
                }
            );
            hookAllMethods(clazz, "addMiuiBottomView",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        customizeBottomViewColor(clazz);
                    }
                }
            );
        } catch (Throwable throwable) {
            logE(TAG, "Set the color of the MiuiBottomView: " + throwable);
        }
    }

    /**
     * 将导航栏颜色赋值给输入法优化的底图
     *
     * @param clazz 声明或继承字段的类
     */
    private void customizeBottomViewColor(Class<?> clazz) {
        try {
            if (navBarColor != 0) {
                int color = -0x1 - navBarColor;
                XposedHelpers.callStaticMethod(clazz, "customizeBottomViewColor", true, navBarColor, color | -0x1000000, color | 0x66000000);
            }
        } catch (Throwable e) {
            logE(TAG, "call customizeBottomViewColor: " + e);
        }
    }

    /**
     * 针对A10的修复切换输入法列表
     *
     * @param className 声明或继承方法的类的名称
     */
    private void hookDeleteNotSupportIme(String className, ClassLoader classLoader) {
        try {
            hookAllMethods(findClassIfExists(className, classLoader), "deleteNotSupportIme", MethodHook.returnConstant(null));
        } catch (Throwable throwable) {
            logE(TAG, "Hook method deleteNotSupportIme: " + throwable);
        }
    }

    /**
     * 使切换输入法界面显示第三方输入法
     *
     * @param classLoader
     */
    private void getSupportIme(ClassLoader classLoader) {
        try {
            findAndHookMethod("com.miui.inputmethod.InputMethodBottomManager",
                classLoader, "getSupportIme",
                new MethodHook() {

                    @Override
                    protected void before(MethodHookParam param) {
                        List<?> getEnabledInputMethodList = (List<?>) XposedHelpers.callMethod(
                            XposedHelpers.getObjectField(
                                XposedHelpers.getStaticObjectField(
                                    findClassIfExists("com.miui.inputmethod.InputMethodBottomManager", classLoader),
                                    "sBottomViewHelper"), "mImm"), "getEnabledInputMethodList");
                        param.setResult(getEnabledInputMethodList);
                    }
                }
            );
        } catch (Throwable e) {
            logE(TAG, "Hook method getSupportIme: " + e);
        }
    }
}
