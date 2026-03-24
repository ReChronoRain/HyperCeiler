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
package com.sevtinge.hyperceiler.libhook.rules.packageinstaller

import android.content.Context
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.stream.Collectors


object DisableAppInfoUpload : BaseHook() {
    override fun useDexKit() = true
    private lateinit var avlUploadInvokerList: List<Method>
    private lateinit var interceptCheckInvokerList: List<Method>
    private lateinit var infoLayoutInvokerList: List<Method>

    override fun initDexKit(): Boolean {
        /**
         * methods invoke api '/avl/upload/'
         */
        avlUploadInvokerList = requiredMemberList("avlUploadInvokerList") {
            it.findMethod {
                matcher {
                    paramCount(4)
                    paramTypes(
                        null,
                        findClass("com.miui.packageInstaller.model.ApkInfo"),
                        Boolean::class.javaPrimitiveType,
                        null
                    )
                    usingStrings("appSourcepackageName", "packageName")
                    returnType(Void::class.javaPrimitiveType as Class<*>)
                    modifiers(AccessFlagsMatcher.create(Modifier.STATIC))
                }
            }
        }
        /**
         * methods invoke api '/v4/game/interceptcheck/'
         */
        interceptCheckInvokerList = requiredMemberList("interceptCheckInvokerList") {
            it.findMethod {
                matcher {
                    paramCount(6)
                    paramTypes(
                        Context::class.java,
                        null,
                        Int::class.javaPrimitiveType,
                        findClass("com.miui.packageInstaller.model.ApkInfo"),
                        HashMap::class.java,
                        null
                    )
                    returnType(Object::class.java)
                    usingStrings("device_type", "packageName", "installationMode", "apk_bit")
                }
            }
        }
        /**
         * methods invoke api '/info/layout'
         */
        infoLayoutInvokerList = requiredMemberList("infoLayoutInvokerList") {
            it.findMethod {
                matcher {
                    paramCount(7)
                    paramTypes(
                        String::class.java,
                        String::class.java,
                        String::class.java,
                        Integer::class.java,
                        String::class.java,
                        String::class.java,
                        null
                    )
                }
            }
        }
        return true
    }

    override fun init() {
        disableInvokeInfoLayoutApi()
        disableInvokeInterceptCheckApi()
        disableInvokeAvlUploadApi()
    }

    /**
     * after installing
     */
    private fun disableInvokeAvlUploadApi() {
        logD("/avl/upload/", avlUploadInvokerList)

        avlUploadInvokerList.forEach {
            it.createHook {
                replace { }
            }
        }
    }

    private fun disableInvokeInterceptCheckApi() {
        logD("/interceptcheck", interceptCheckInvokerList)

        interceptCheckInvokerList.forEach {
            it.createHook {
                returnConstant(null)
            }
        }
    }

    private fun disableInvokeInfoLayoutApi() {
        logD("/info/layout'", infoLayoutInvokerList)

        infoLayoutInvokerList.forEach { method ->
            method.createHook {
                if (method.returnType == Object::class.java) {
                    returnConstant(null)
                } else {
                    replace { }
                }
            }
        }

        setResReplacement("com.miui.packageinstaller", "layout", "layout_network_error", R.layout.replacement_empty)
        setResReplacement("com.miui.packageinstaller", "layout", "safe_mode_layout_network_error", R.layout.replacement_empty)
    }

    private fun logD(prefix: String, list:  List<Method>) {
        XposedLog.d(
            TAG, lpparam.packageName,
            "'${prefix}' find methods: " + list.stream().map {
                "${it.javaClass.name}#${
                    it.name
                }"
            }.collect(Collectors.joining(" | "))
        )
    }
}
