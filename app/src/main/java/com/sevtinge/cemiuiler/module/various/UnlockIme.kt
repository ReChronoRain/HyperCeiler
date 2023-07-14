package com.sevtinge.cemiuiler.module.various

import android.view.inputmethod.InputMethodManager
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.PropertyUtils
import com.sevtinge.cemiuiler.utils.api.sameAs
import com.sevtinge.cemiuiler.utils.callStaticMethod
import com.sevtinge.cemiuiler.utils.getObjectFieldAs
import com.sevtinge.cemiuiler.utils.getStaticObjectField
import com.sevtinge.cemiuiler.utils.setStaticObjectField
import de.robv.android.xposed.callbacks.XC_LoadPackage

object UnlockIme : BaseHook() {
    private val miuiImeList: List<String> = listOf(
        "com.iflytek.inputmethod.miui",
        "com.sohu.inputmethod.sogou.xiaomi",
        "com.baidu.input_mi",
        "com.miui.catcherpatch"
    )

    private var navBarColor: Int? = null

    override fun init() {
        // 检查是否支持全面屏优化
        if (PropertyUtils["ro.miui.support_miui_ime_bottom", "0"] != "1") return
        EzXHelper.initHandleLoadPackage(lpparam)
        EzXHelper.setLogTag(TAG)
        Log.i("MiuiIme is supported")
        startHook(lpparam)
    }

    private fun startHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 检查是否为小米定制输入法
        val isNonCustomize = !miuiImeList.contains(lpparam.packageName)
        if (isNonCustomize) {
            val sInputMethodServiceInjector =
                loadClassOrNull("android.inputmethodservice.InputMethodServiceInjector")
                    ?: loadClassOrNull("android.inputmethodservice.InputMethodServiceStubImpl")

            sInputMethodServiceInjector?.also {
                hookSIsImeSupport(it)
                hookIsXiaoAiEnable(it)
                setPhraseBgColor(it)
            } ?: Log.e("Failed:Class not found: InputMethodServiceInjector")
        }

        hookDeleteNotSupportIme(
            "android.inputmethodservice.InputMethodServiceInjector\$MiuiSwitchInputMethodListener",
            lpparam.classLoader
        )

        // 获取常用语的ClassLoader
        loadClass("android.inputmethodservice.InputMethodModuleManager").methodFinder().filter {
            name == "loadDex" && parameterTypes.sameAs(ClassLoader::class.java, String::class.java)
        }.toList().createHooks {
            after { param ->
                hookDeleteNotSupportIme(
                    "com.miui.inputmethod.InputMethodBottomManager\$MiuiSwitchInputMethodListener",
                    param.args[0] as ClassLoader
                )
                loadClassOrNull(
                    "com.miui.inputmethod.InputMethodBottomManager",
                    param.args[0] as ClassLoader
                )?.also {
                    if (isNonCustomize) {
                        hookSIsImeSupport(it)
                        hookIsXiaoAiEnable(it)
                    }

                    // 针对A11的修复切换输入法列表
                    it.getMethod("getSupportIme").createHook {
                        replace { _ ->
                            it.getStaticObjectField("sBottomViewHelper")
                                ?.getObjectFieldAs<InputMethodManager>("mImm")?.enabledInputMethodList
                        }
                    }
                } ?: Log.e("Failed:Class not found: com.miui.inputmethod.InputMethodBottomManager")
            }
        }

        Log.i("Hook MIUI IME Done!")
    }

    /**
     * 跳过包名检查，直接开启输入法优化
     *
     * @param clazz 声明或继承字段的类
     */
    private fun hookSIsImeSupport(clazz: Class<*>) {
        runCatching {
            clazz.setStaticObjectField("sIsImeSupport", 1)
            Log.i("Success:Hook field sIsImeSupport")
        }.onFailure {
            Log.i("Failed:Hook field sIsImeSupport ")
            Log.i(it)
        }
    }

    /**
     * 小爱语音输入按钮失效修复
     *
     * @param clazz 声明或继承方法的类
     */
    private fun hookIsXiaoAiEnable(clazz: Class<*>) {
        runCatching {
            clazz.getMethod("isXiaoAiEnable").createHook {
                returnConstant(false)
            }
        }.onFailure {
            Log.i("Failed:Hook method isXiaoAiEnable")
            Log.i(it)
        }
    }

    /**
     * 在适当的时机修改抬高区域背景颜色
     *
     * @param clazz 声明或继承字段的类
     */
    private fun setPhraseBgColor(clazz: Class<*>) {
        runCatching {
            // 导航栏颜色被设置后, 将颜色存储起来并传递给常用语
            loadClass("com.android.internal.policy.PhoneWindow").methodFinder().first {
                name == "setNavigationBarColor" && parameterTypes.sameAs(Int::class.java)
            }.createHook {
                after { param ->
                    if​ (param.args[​0​] ​==​ ​0​) ​return​@after
                    navBarColor = param.args[0] as Int
                    customizeBottomViewColor(clazz)
                }
            }

            // 当常用语被创建后, 将背景颜色设置为存储的导航栏颜色
            clazz.methodFinder().first { name == "addMiuiBottomView" }.createHook {
                after {
                    customizeBottomViewColor(clazz)
                }
            }
        }.onFailure {
            Log.i("Failed to set the color of the MiuiBottomView")
            Log.i(it)
        }
    }

    /**
     * 将导航栏颜色赋值给输入法优化的底图
     *
     * @param clazz 声明或继承字段的类
     */
    private fun customizeBottomViewColor(clazz: Class<*>) {
        navBarColor?.let {
            val color = -0x1 - it
            clazz.callStaticMethod(
                "customizeBottomViewColor",
                true, navBarColor, color or -0x1000000, color or 0x66000000
            )
        }
    }

    /**
     * 针对A10的修复切换输入法列表
     *
     * @param className 声明或继承方法的类的名称
     */
    private fun hookDeleteNotSupportIme(className: String, classLoader: ClassLoader) {
        runCatching {
            loadClass(className, classLoader).methodFinder().first { name == "deleteNotSupportIme" }
                .createHook { returnConstant(null) }
        }.onFailure {
            Log.i("Failed:Hook method deleteNotSupportIme")
            Log.i(it)
        }
    }
}
