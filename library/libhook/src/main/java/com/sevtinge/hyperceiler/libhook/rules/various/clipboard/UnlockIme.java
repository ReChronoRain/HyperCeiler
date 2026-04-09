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
package com.sevtinge.hyperceiler.libhook.rules.various.clipboard;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getProp;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.appbase.input.InputMethodBottomManagerHelper;
import com.sevtinge.hyperceiler.libhook.appbase.input.InputMethodConfig;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Source:
 * <a href="https://github.com/RC1844/MIUI_IME_Unlock/blob/main/app/src/main/java/com/xposed/miuiime/MainHook.kt">RC1844/MIUI_IME_Unlock</a>
 */
public class UnlockIme extends BaseHook {

    private final String[] miuiImeList = new String[]{
        "com.iflytek.inputmethod.miui",
        "com.sohu.inputmethod.sogou.xiaomi",
        "com.baidu.input_mi",
        "com.miui.catcherpatch"
    };

    private int navBarColor = 0;

    @Override
    public void init() {
        // 检查是否支持全面屏优化
        if (isMiuiImeBottomSupported()) {
            startHook(getLpparam());
        }
    }

    private void startHook(XposedModuleInterface.PackageReadyParam param) {
        boolean showAllImeList = InputMethodConfig.shouldShowAllImeList();

        // 检查是否为小米定制输入法
        boolean isNonCustomize = isNonCustomizeIme(param.getPackageName());
        if (isNonCustomize) {
            Class<?> sInputMethodServiceInjector = findClassIfExists("android.inputmethodservice.InputMethodServiceInjector");
            if (sInputMethodServiceInjector == null)
                sInputMethodServiceInjector = findClassIfExists("android.inputmethodservice.InputMethodServiceStubImpl");
            if (sInputMethodServiceInjector != null) {
                hookSIsImeSupport(sInputMethodServiceInjector);
                hookIsXiaoAiEnable(sInputMethodServiceInjector);
                setPhraseBgColor(sInputMethodServiceInjector);
            } else {
                XposedLog.e(TAG, "Class not found: InputMethodServiceInjector");
            }
        }

        if (showAllImeList) {
            hookDeleteNotSupportIme("android.inputmethodservice.InputMethodServiceInjector$MiuiSwitchInputMethodListener",
                param.getClassLoader());
        }
    }

    public void initLoader(ClassLoader classLoader) {
        if (!isMiuiImeBottomSupported()) {
            return;
        }

        boolean showAllImeList = InputMethodConfig.shouldShowAllImeList();
        if (showAllImeList) {
            // 针对 A11 的修复切换输入法列表
            getSupportIme(classLoader);
            hookDeleteNotSupportIme("com.miui.inputmethod.InputMethodBottomManager$MiuiSwitchInputMethodListener", classLoader);
        }

        if (!isNonCustomizeIme(getPackageName())) {
            return;
        }

        Class<?> inputMethodBottomManager =
            InputMethodBottomManagerHelper.findBottomManagerClass(classLoader);
        if (inputMethodBottomManager != null) {
            hookSIsImeSupport(inputMethodBottomManager);
            hookIsXiaoAiEnable(inputMethodBottomManager);
        } else {
            XposedLog.e(TAG, "Class not found: com.miui.inputmethod.InputMethodBottomManager");
        }
    }

    /**
     * 跳过包名检查，直接开启输入法优化
     *
     * @param clazz 声明或继承字段的类
     */
    private void hookSIsImeSupport(Class<?> clazz) {
        try {
            EzxHelpUtils.setStaticObjectField(clazz, "sIsImeSupport", 1);
        } catch (Throwable throwable) {
            XposedLog.e(TAG, "Hook field sIsImeSupport: " + throwable);
        }
    }

    /**
     * 小爱语音输入按钮失效修复
     *
     * @param clazz 声明或继承方法的类
     */
    private void hookIsXiaoAiEnable(Class<?> clazz) {
        try {
            hookAllMethods(clazz, "isXiaoAiEnable", returnConstant(false));
        } catch (Throwable throwable) {
            XposedLog.e(TAG, "Hook method isXiaoAiEnable: " + throwable);
        }
    }

    /**
     * 在适当的时机修改抬高区域背景颜色
     *
     * @param clazz 声明或继承字段的类
     */
    private void setPhraseBgColor(Class<?> clazz) {
        try {
            // 导航栏颜色被设置后, 将颜色存储起来并传递给常用语
            findAndHookMethod("com.android.internal.policy.PhoneWindow",
                "setNavigationBarColor", int.class,
                new IMethodHook() {
                    @Override
                    public void after(HookParam param) {
                        if ((int) param.getArgs()[0] == 0) return;
                        navBarColor = (int) param.getArgs()[0];
                        customizeBottomViewColor(clazz);
                    }
                }
            );
            // 当常用语被创建后, 将背景颜色设置为存储的导航栏颜色
            hookAllMethods(clazz, "addMiuiBottomView",
                new IMethodHook() {
                    @Override
                    public void after(HookParam param) {
                        customizeBottomViewColor(clazz);
                    }
                }
            );
        } catch (Throwable throwable) {
            XposedLog.e(TAG, "Set the color of the MiuiBottomView: " + throwable);
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
                callStaticMethod(clazz, "customizeBottomViewColor", true, navBarColor, color | -0x1000000, color | 0x66000000);
            }
        } catch (Throwable e) {
            XposedLog.w(TAG, "call customizeBottomViewColor: " + e);
        }
    }

    /**
     * 针对 A10 的修复切换输入法列表
     * Android 16 已无此方法
     *
     * @param className 声明或继承方法的类的名称
     */
    private void hookDeleteNotSupportIme(String className, ClassLoader classLoader) {
        if (isMoreAndroidVersion(36)) return;
        try {
            Class<?> clazz = EzxHelpUtils.findClassIfExists(className, classLoader);
            if (clazz == null) {
                XposedLog.w(TAG, "Class not found: " + className);
                return;
            }
            hookAllMethods(clazz, "deleteNotSupportIme", returnConstant(null));
        } catch (Throwable throwable) {
            XposedLog.e(TAG, "Hook method deleteNotSupportIme: " + throwable);
        }
    }

    /**
     * 针对 A11 的修复切换输入法列表
     *
     * @param classLoader
     */
    private void getSupportIme(ClassLoader classLoader) {
        try {
            EzxHelpUtils.findAndHookMethod("com.miui.inputmethod.InputMethodBottomManager",
                classLoader, "getSupportIme",
                new IMethodHook() {
                    @Override
                    public void before(HookParam param) throws Throwable {
                        Object getEnabledInputMethodList =
                            InputMethodBottomManagerHelper.getEnabledInputMethodList(classLoader);
                        if (getEnabledInputMethodList instanceof List<?>) {
                            param.setResult(getEnabledInputMethodList);
                        }
                    }
                }
            );
        } catch (Throwable e) {
            XposedLog.e(TAG, "Hook method getSupportIme: " + e);
        }
    }

    private boolean isMiuiImeBottomSupported() {
        return "1".equals(getProp("ro.miui.support_miui_ime_bottom", "0"));
    }

    private boolean isNonCustomizeIme(String packageName) {
        for (String isMiui : miuiImeList) {
            if (isMiui.equals(packageName)) {
                return false;
            }
        }
        return true;
    }
}
