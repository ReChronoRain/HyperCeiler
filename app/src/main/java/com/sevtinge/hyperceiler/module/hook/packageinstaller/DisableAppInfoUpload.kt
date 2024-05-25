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
package com.sevtinge.hyperceiler.module.hook.packageinstaller

import android.content.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.*
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toElementList
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toMethod
import org.luckypray.dexkit.query.matchers.base.*
import java.lang.reflect.*
import java.util.stream.*


object DisableAppInfoUpload : BaseHook() {

    override fun init() {
        disableInvokeInfoLayoutApi()
        disableInvokeInterceptCheckApi()
        disableInvokeAvlUploadApi()
    }

    /**
     * after installing
     */
    private fun disableInvokeAvlUploadApi() {
        /**
         * methods invoke api '/avl/upload/'
         */
        val avlUploadInvokerList = DexKit.getDexKitBridgeList("avlUploadInvokerList") {
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
            }.toElementList(EzXHelper.safeClassLoader)
        }.toMethodList()

        if (avlUploadInvokerList.isEmpty()) {
            throw IllegalStateException("cannot find MethodData invoking '/avl/upload/'")
        }
        logD("/avl/upload/", avlUploadInvokerList)

        avlUploadInvokerList.forEach {
            it.createHook {
                replace { }
            }
        }
    }

    private fun disableInvokeInterceptCheckApi() {
        /**
         * methods invoke api '/v4/game/interceptcheck/'
         */
        val interceptCheckInvokerList = DexKit.getDexKitBridgeList("interceptCheckInvokerList") {
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
            }.toElementList(EzXHelper.safeClassLoader)
        }.toMethodList()

        if (interceptCheckInvokerList.isEmpty()) {
            throw IllegalStateException("cannot find MethodData invoking 'interceptcheck'")
        }
        logD("/interceptcheck", interceptCheckInvokerList)

        interceptCheckInvokerList.forEach {
            it.createHook {
                returnConstant(null)
            }
        }
    }

    private fun disableInvokeInfoLayoutApi() {
        /**
         * methods invoke api '/info/layout'
         */
        val infoLayoutInvokerList = DexKit.getDexKitBridgeList("interceptCheckInvokerList") {
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
            }.toElementList(EzXHelper.safeClassLoader)
        }.toMethodList()

        if (infoLayoutInvokerList.isEmpty()) {
            throw IllegalStateException("cannot find MethodData invoking '/info/layout'")
        }
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

        mResHook.setResReplacement("com.miui.packageinstaller", "layout", "layout_network_error", R.layout.replacement_empty)
        mResHook.setResReplacement("com.miui.packageinstaller", "layout", "safe_mode_layout_network_error", R.layout.replacement_empty)
    }

    private fun logD(prefix: String, list:  List<Method>) {
        logD(
            TAG, lpparam.packageName,
            "'${prefix}' find methods: " + list.stream().map {
                "${it.javaClass.name}#${
                    it.toMethod().name
                }"
            }.collect(Collectors.joining(" | "))
        )
    }
}
