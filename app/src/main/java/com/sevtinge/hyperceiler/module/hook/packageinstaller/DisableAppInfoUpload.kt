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

import android.content.Context
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.*
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.result.MethodDataList
import java.lang.reflect.Modifier
import java.util.stream.Collectors


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
        val avlUploadInvokerList = DexKit.dexKitBridge.findMethod {
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

        if (avlUploadInvokerList.isEmpty()) {
            throw IllegalStateException("cannot find MethodData invoking '/avl/upload/'")
        }
        logD("/avl/upload/", avlUploadInvokerList)

        avlUploadInvokerList.forEach {
            it.getMethodInstance(lpparam.classLoader).createHook {
                replace { }
            }
        }
    }

    private fun disableInvokeInterceptCheckApi() {
        /**
         * methods invoke api '/v4/game/interceptcheck/'
         */
        val interceptCheckInvokerList = DexKit.dexKitBridge.findMethod {
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

        if (interceptCheckInvokerList.isEmpty()) {
            throw IllegalStateException("cannot find MethodData invoking 'interceptcheck'")
        }
        logD("/interceptcheck", interceptCheckInvokerList)

        interceptCheckInvokerList.forEach {
            it.getMethodInstance(lpparam.classLoader).createHook {
                returnConstant(null)
            }
        }
    }

    private fun disableInvokeInfoLayoutApi() {
        /**
         * methods invoke api '/info/layout'
         */
        val infoLayoutInvokerList = DexKit.dexKitBridge.findMethod {
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

        if (infoLayoutInvokerList.isEmpty()) {
            throw IllegalStateException("cannot find MethodData invoking '/info/layout'")
        }
        logD("/info/layout'", infoLayoutInvokerList)

        infoLayoutInvokerList.forEach {
            val method = it.getMethodInstance(lpparam.classLoader)
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

    private fun logD(prefix: String, list: MethodDataList) {
        logD(
            TAG, lpparam.packageName,
            "'${prefix}' find methods: " + list.stream().map {
                "${it.getClassInstance(lpparam.classLoader).name}#${
                    it.getMethodInstance(
                        lpparam.classLoader
                    ).name
                }"
            }.collect(Collectors.joining(" | "))
        )
    }
}
