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
package com.sevtinge.hyperceiler.libhook.rules.mediaeditor

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.LazyClass.AndroidBuildCls
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setStaticObjectField
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.core.extension.MemberExtension.paramCount
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import org.json.JSONObject
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object UnlockCustomPhotoFrames : BaseHook() {
    private val isCloudData by lazy {
        mPrefsMap.getBoolean("mediaeditor_unlock_cloud_custom_photo")
    }
    private val isLeica by lazy {
        mPrefsMap.getBoolean("mediaeditor_unlock_custom_photo_frames_leica")
    }
    private val isRedmi by lazy {
        mPrefsMap.getBoolean("mediaeditor_unlock_custom_photo_frames_redmi")
    }
    private val isPOCO by lazy {
        mPrefsMap.getBoolean("mediaeditor_unlock_custom_photo_frames_poco")
    }

    private val methodA by lazy<List<Method>> {
        DexKit.findMemberList("MA") { bridge ->
            // 改动日志:
            // 现在这个查找方式直接兼容 1.5 - 2.3+
            // 1.6.5.10.2 之后迪斯尼定制画框解锁的地方和现在的不一样
            // 1.7.5.0.4 之后，类名迁移，内部文件改动较大
            // 合并缓存，现在只需查询一次即可获取全部 6 个需要 Hook 的方法，且不会出现多余的方法 (2025.1.8)
            // 1.10.0.0.6 之后，类名混淆
            // 所以，它要是再把特征混淆完了的话，那 886 了
            // 这构思玩意都快接近 hook 安全服务了 (2025.3.19)
            // 使用二次查询，虽然慢，但是能找全
            bridge.findMethod {
                matcher {
                    addCaller {
                        paramCount = 2
                        returnType = "java.util.LinkedHashMap"
                        modifiers = Modifier.STATIC // or Modifier.FINAL 2.0.0.1.8 取消了 FINAL 设定
                        addUsingField {
                            name = "DEVICE"
                        }
                    }
                    addUsingField {
                        type = "java.util.List"
                    }
                    returnType = "boolean"
                }
            }.last().declaredClass?.findMethod {
                matcher {
                    addUsingField {
                        type = "java.util.List"
                    }

                    returnType = "boolean"
                }
            }
        }
    }

    private val methodB by lazy<Method?> {
        DexKit.findMember("MB") { bridge ->
            bridge.findMethod {
                matcher {
                    paramCount = 2
                    returnType = "java.util.LinkedHashMap"
                    modifiers = Modifier.STATIC // or Modifier.FINAL 2.0.0.1.8 取消了 FINAL 设定
                    addInvoke {
                        paramCount = 0
                        returnType = "boolean"
                    }
                    addUsingField {
                        name = "DEVICE"
                    }
                }
            }.single()
        }
    }

    private val methodC by lazy<List<Method>> {
        DexKit.findMemberList("MC") { bridge ->
            bridge.findMethod {
                matcher {
                    declaredClass = methodA.first().declaringClass.name

                    paramCount = 0
                    returnType = "boolean"
                }
            }
        }
    }

    private val cloudA by lazy<Method?> {
        DexKit.findMember("CA") { bridge ->
            // 2.3.0.0.9 起 TAG 已混淆
            bridge.findMethod {
                matcher {
                    addCaller {
                        addInvoke("Ljava/util/concurrent/locks/ReentrantLock;->lock()V")
                        addInvoke("Ljava/lang/Object;->equals(Ljava/lang/Object;)Z")
                    }
                    paramCount = 2
                }
            }.single {
                it.paramTypes[0] == it.returnType
            }

            /*bridge.findClass {
                matcher {
                    addUsingString(
                        "PhotoWatermarkRepository",
                        StringMatchType.Contains
                    )
                }
            }.findMethod {
                matcher {
                    // 2.2.0.8 开始已混淆此字符串
                    // addUsingString("https://www.baidu.com", StringMatchType.Contains)
                    addInvoke("Ljava/net/URL;->openConnection()Ljava/net/URLConnection;")
                }
            }.single()*/
        }
    }

    override fun init() {
        var index = 0
        val actions = listOf<(Method) -> Unit>(::xiaomi, ::poco, ::redmi)

        methodA.forEach { method ->
            if (method.paramCount == 1) {
                // 公共解锁特定机型定制画框使用限制
                other(method)
            } else {
                val action = actions[index]
                action(method)
                index = (index + 1) % actions.size
            }
        }

        if (isLeica && methodB != null) {
            // 1.10.0.0.6 新增 Xiaomi 15 Ultra 独占定制画框
            methodB?.createBeforeHook {
                AndroidBuildCls.setStaticObjectField(
                    "DEVICE",
                    "xuanyuan"
                )
            }
        }

        if (isRedmi) {
            // Redmi Note 13 Pro+ 定制版画框
            val base = methodA.first().declaringClass.name
            // 优先尝试常见的内部类名，找到即挂钩，避免不必要地回退计算
            val target = runCatching { loadClass($$"$${base}$a") }.getOrNull()
                ?: runCatching { loadClass($$"$${base}$2") }.getOrNull()

            if (target != null) {
                target.methodFinder()
                    .filterByName("invoke")
                    .firstOrNull()
                    ?.createHook { returnConstant(true) }
                    ?: XposedLog.e(
                        TAG,
                        lpparam.packageName,
                        "Invoke method not found in ${target.name}"
                    )
            } else {
                // 回退：对 methodC 做差集匹配并钩子，延迟构建签名集合以提升性能
                val aSigns = methodA.asSequence().map { m ->
                    "${m.declaringClass.name}#${m.name}(${m.parameterTypes.joinToString(",") { it.name }})"
                }.toSet()

                methodC.asSequence()
                    .filter { m ->
                        val sig = "${m.declaringClass.name}#${m.name}(${m.parameterTypes.joinToString(",") { it.name }})"
                        sig !in aSigns
                    }
                    .forEach { m ->
                        try {
                            m.createHook { returnConstant(true) }
                        } catch (e: Throwable) {
                            XposedLog.e(
                                TAG,
                                lpparam.packageName,
                                "Hook failed for ${m.name}: ${e.message}"
                            )
                        }
                    }
            }
        }

        // 2.0.9.0.6 之后的新水印改成云控方案，和相机一样
        runCatching {
            if (isCloudData && cloudA != null) {
                // 强制拉取云端配置
                cloudA?.createBeforeHook {
                    val getConfig = it.args[0]
                    it.result = getConfig
                }

                JSONObject::class.java.methodFinder()
                    .filterByName("optJSONObject")
                    .first()
                    .createHook {
                        before {
                            // 忽略时间和机型限制
                            val limitation = it.args[0] as String
                            if (limitation.contains("limitation")) {
                                XposedLog.d(TAG, lpparam.packageName, "block limitation optJSONObject")
                                it.result = null
                            }
                        }
                    }
            }
        }.onFailure {
            XposedLog.e(TAG, lpparam.packageName, "cloudA not found, maybe not supported")
        }
    }

    private fun xiaomi(name: Method) {
        name.createHook {
            returnConstant(isLeica)
        }
    }

    private fun redmi(name: Method) {
        name.createHook {
            returnConstant(isRedmi)
        }
    }

    private fun poco(name: Method) {
        name.createHook {
            returnConstant(isPOCO)
        }
    }

    private fun other(name: Method) {
        name.createHook {
            returnConstant(true)
        }
    }
}
