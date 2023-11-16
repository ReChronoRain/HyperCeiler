package com.sevtinge.hyperceiler.module.hook.mishare

import com.github.kyuubiran.ezxhelper.EzXHelper.classLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.api.BlurDraw.getValueByFields
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Modifier

object NoAutoTurnOff : BaseHook() {
    private val nullMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("EnabledState", "mishare_enabled")
            }
        }.map { it.getMethodInstance(classLoader) }.toList()
    }

    private val null2Method by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingStringsEquals("mishare:advertise_lock")
                }
                paramCount = 2
                modifiers = Modifier.STATIC
            }
        }.map { it.getMethodInstance(classLoader) }.first()
    }

    private val null3Method by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("com.miui.mishare.action.GRANT_NFC_TOUCH_PERMISSION")
                usingNumbers(600000L)
                modifiers = Modifier.PRIVATE
            }
        }.map { it.getMethodInstance(classLoader) }.toList()
    }

    private val toastMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingStringsEquals("null context", "cta_agree")
                }
                returnType = "boolean"
                paramTypes = listOf("android.content.Context", "java.lang.String")
                paramCount = 2
            }
        }.map { it.getMethodInstance(classLoader) }.toList()
    }

    // 比较激进的移除 Toast 方式
    /*private val toast2Method by lazy {
        dexKitBridge.findMethod {
            matcher {
                returnType = "void"
                paramTypes = listOf("android.content.Context", "java.lang.CharSequence","int")
                modifiers = Modifier.STATIC
            }
        }.map { it.getMethodInstance(classLoader) }.toList()
    }*/

    override fun init() {
        val nullClass = dexKitBridge.findClass {
            matcher {
                addUsingStringsEquals("NfcShareTaskManager", "task out of limit type ")
            }
        }.map { it.getInstance(classLoader) }.first()

        val nullField = dexKitBridge.findField {
            matcher {
                declaredClass(nullClass)
                modifiers = Modifier.STATIC
            }
        }.map { it.getFieldInstance(classLoader) }.first()

        // 禁用小米互传功能自动关闭部分
        try {
        nullMethod.createHooks {
            returnConstant(null)
        }
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, "nullMethod hook failed", t)
        }

        try {
            null2Method.createHook {
                returnConstant(null)
            }
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, "null2Method hook failed", t)
        }

        try {
            null3Method.createHooks {
                after {
                    val fieldNames = ('a'..'z').map { name -> name.toString() }
                    val getField = getValueByFields(it.thisObject, fieldNames) ?: return@after
                    XposedHelpers.callMethod(getField, "removeCallbacks", it.thisObject)
                    logI(
                        TAG, this@NoAutoTurnOff.lpparam.packageName,
                        "null3Method hook success, $getField"
                    )
                }
            }
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, "null3Method hook failed", t)
        }

        try {
            findAndHookConstructor(nullClass, object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    XposedHelpers.setObjectField(param.thisObject, nullField.name, 999999999)
                    logI(nullField.name)
                } })
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, "nullField hook failed", t)
        }


        // 干掉小米互传十分钟倒计时 Toast
        toastMethod.createHooks {
            before { param ->
                if (param.args[1].equals("security_agree")) {
                    param.result = false
                }
            }
        }
    }
}
