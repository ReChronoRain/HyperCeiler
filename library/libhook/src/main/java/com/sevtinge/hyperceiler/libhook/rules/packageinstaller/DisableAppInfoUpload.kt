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
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.result.MethodDataList
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
        infoLayoutInvokerList = optionalMemberList("infoLayoutInvokerList") {
            // 先查接口常量本身。dexdump 实测（audit/appinfo_upload_check.py）：
            // OS3 里 '/info/layout' 存在于 s0.I#<clinit>，OS4.0.5 里该常量已从 dex 消失（0 命中），
            // 即这个接口调用点已被移除。仅靠 7 参形状在 405 上会命中 5 个形状同构但并非
            // 调用方的方法（c2.b#<init>、c2.c#a/j/k/l），其中返回 Object 的那个会被
            // returnConstant(null)，有误伤真实功能的风险；OS3 下形状匹配则恰好只有 2 个真调用方。
            // 因此把“接口常量存在”作为门禁：不在则这一条整体不 hook（optional 只降级），
            // 在则维持原形状匹配。
            if (it.findClass { matcher { usingStrings("/info/layout") } }.isEmpty()) {
                MethodDataList()
            } else it.findMethod {
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
            }.apply {
                // 该 matcher 没有限定 returnType / modifiers，DexKit 的 findMethod 会把构造函数一并返回。
                // OS4.0.4.0 设备日志实测：Lc2/b;-><init>(String,String,String,Integer,String,String,Lc2/a;)
                // 正好满足上面 7 参，而 getMethodInstance() 对 <init> 抛
                // "not a method"（InstanceUtil），整条规则因此被跳过。
                // 这里原地剔除构造类成员（保持 MethodDataList 类型，findMemberList 依赖它做分支）。
                // 405 离线复算同样命中这一条：<init> 在 7 参结果集里（唯一）。
                removeIf { data -> data.name == "<init>" || data.name == "<clinit>" }
            }
        }
        // 说明：三个查询对应三个独立的上报/拉取接口，以前都是 required，
        // 只要其中一个错配就会连带另外两个也不上钩（OS4.0.4.0 就是 info/layout 连带
        // 了 /avl/upload 与 /v4/game/interceptcheck）。因此 info/layout 降为 optional，
        // 查不到时只降级不连带。
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
