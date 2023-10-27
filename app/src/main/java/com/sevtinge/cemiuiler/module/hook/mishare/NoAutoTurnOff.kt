package com.sevtinge.cemiuiler.module.hook.mishare

import com.github.kyuubiran.ezxhelper.EzXHelper.classLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.api.BlurDraw.getValueByFields
import com.sevtinge.cemiuiler.utils.log.XposedLogUtils
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
                    addUsingStringsEquals("mishare:advertise_lock", "power")
                }
                paramCount = 2
                modifiers = Modifier.STATIC
            }
        }.map { it.getMethodInstance(classLoader) }.toList()
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
        nullMethod.createHooks {
            before {
                it.result = null
            }
        }

        try {
            null2Method.createHooks {
                before {
                    it.result = null
                }
            }
        } catch (_: Throwable) {
        }

        try {
            null3Method.createHooks {
                after {
                    val fieldNames = ('a'..'z').map { name -> name.toString() }
                    val getField = getValueByFields(it.thisObject, fieldNames) ?: return@after
                    XposedHelpers.callMethod(getField, "removeCallbacks", it.thisObject)
                    XposedLogUtils.logI("null3Method hook success, $getField")
                }
            }
        } catch (t: Throwable) {
            XposedLogUtils.logE(TAG, "null3Method hook failed", t)
        }

        try {
            XposedLogUtils.logI("$nullField")
            XposedHelpers.setStaticIntField(nullClass, nullField.name, 999999999)
            XposedLogUtils.logI(TAG, "nullField hook success.")
        } catch (t: Throwable) {
            XposedLogUtils.logE(TAG, "nullField hook failed", t)
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
