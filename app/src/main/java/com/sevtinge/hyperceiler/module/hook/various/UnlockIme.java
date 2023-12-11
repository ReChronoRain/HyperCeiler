package com.sevtinge.hyperceiler.module.hook.various;

import android.view.inputmethod.InputMethodManager;

import com.sevtinge.hyperceiler.module.base.BaseHook;

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
                logE(TAG, "Failed:Class not found: InputMethodServiceInjector");
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
                    hookDeleteNotSupportIme("com.miui.inputmethod.InputMethodBottomManager$MiuiSwitchInputMethodListener", (ClassLoader) param.args[0]);
                    Class<?> InputMethodBottomManager = findClassIfExists("com.miui.inputmethod.InputMethodBottomManager", (ClassLoader) param.args[0]);
                    if (InputMethodBottomManager != null) {
                        if (finalIsNonCustomize) {
                            hookSIsImeSupport(InputMethodBottomManager);
                            hookIsXiaoAiEnable(InputMethodBottomManager);
                        }
                        try {
                            // 针对A11的修复切换输入法列表
                            hookAllMethods(InputMethodBottomManager, "getSupportIme",
                                new MethodHook() {
                                    @Override
                                    protected void before(MethodHookParam param) {
                                        param.setResult(((InputMethodManager) XposedHelpers.getObjectField(
                                            XposedHelpers.getStaticObjectField(
                                                InputMethodBottomManager,
                                                "sBottomViewHelper"),
                                            "mImm")).getEnabledInputMethodList());
                                    }
                                }
                            );
                        } catch (Throwable throwable) {
                            logE(TAG, "Failed: getSupportIme: " + throwable);
                        }
                    } else {
                        logE(TAG, "Failed:Class not found: com.miui.inputmethod.InputMethodBottomManager");
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
            logE(TAG, "Failed:Hook field sIsImeSupport: " + throwable);
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
            logE(TAG, "Failed:Hook method isXiaoAiEnable: " + throwable);
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
            logE(TAG, "Failed to set the color of the MiuiBottomView: " + throwable);
        }
    }

    /**
     * 将导航栏颜色赋值给输入法优化的底图
     *
     * @param clazz 声明或继承字段的类
     */
    private void customizeBottomViewColor(Class<?> clazz) {
        if (navBarColor != 0) {
            int color = -0x1 - navBarColor;
            XposedHelpers.callStaticMethod(clazz, "customizeBottomViewColor", true, navBarColor, color | -0x1000000, color | 0x66000000);
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
            logE(TAG, "Failed:Hook method deleteNotSupportIme: " + throwable);
        }
    }
}
