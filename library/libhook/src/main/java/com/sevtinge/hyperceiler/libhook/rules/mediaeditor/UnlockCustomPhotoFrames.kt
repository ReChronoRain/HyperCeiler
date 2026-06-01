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

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.LazyClass.AndroidBuildCls
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getPackageVersionCode
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethodOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setStaticObjectField
import org.json.JSONObject
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object UnlockCustomPhotoFrames : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        methodA
        if (!is24012Version) methodB
        methodC
        cloudA
        return true
    }
    private val isCloudData by lazy {
        PrefsBridge.getBoolean("mediaeditor_unlock_cloud_custom_photo")
    }
    private val isLeica by lazy {
        PrefsBridge.getBoolean("mediaeditor_unlock_custom_photo_frames_leica")
    }
    private val isRedmi by lazy {
        PrefsBridge.getBoolean("mediaeditor_unlock_custom_photo_frames_redmi")
    }
    private val isPOCO by lazy {
        PrefsBridge.getBoolean("mediaeditor_unlock_custom_photo_frames_poco")
    }

    val is24012Version by lazy {
        getPackageVersionCode(lpparam) >= 204990012
    }

    private val methodA by lazy<List<Method>> {
        requiredMemberList("MA") { bridge ->
            // 改动日志:
            // 现在这个查找方式直接兼容 1.5 - 2.4+
            // 1.6.5.10.2 之后迪斯尼定制画框解锁的地方和现在的不一样
            // 1.7.5.0.4 之后，类名迁移，内部文件改动较大
            // 合并缓存，现在只需查询一次即可获取全部 6 个需要 Hook 的方法，且不会出现多余的方法 (2025.1.8)
            // 1.10.0.0.6 之后，类名混淆
            // 所以，它要是再把特征混淆完了的话，那 886 了
            // 这构思玩意都快接近 hook 安全服务了 (2025.3.19)
            // 使用二次查询，虽然慢，但是能找全
            // 2.4.0.1.2 开始, 整体更改较大
            // 使用了 exif 信息校验设备，移除了显式返回的类型，好在有马脚可以直接匹配，要不然真头疼 (2026.6.1)
            bridge.findMethod {
                matcher {
                    addCaller {
                        if (is24012Version) {
                            paramCount = 4
                            addInvoke("Lmiuix/core/util/SystemProperties;->getBoolean(Ljava/lang/String;Z)Z")
                            modifiers = Modifier.STATIC
                            // 2.4.0.1.2 移除了使用 DEVICE 信息校验设备，改用 exif 信息校验设备
                            /*addUsingField {
                                name = "DEVICE"
                            }*/
                        } else {
                            paramCount = 2
                            returnType = "java.util.LinkedHashMap"
                            modifiers = Modifier.STATIC // or Modifier.FINAL 2.0.0.1.8 取消了 FINAL 设定
                            addUsingField {
                                name = "DEVICE"
                            }
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

    @Deprecated("2.4.0.1.2 版本已弃用本地逻辑判断")
    private val methodB by lazy<Method?> {
        optionalMember("MB") { bridge ->
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
        optionalMemberList("MC") { bridge ->
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
        optionalMember("CA") { bridge ->
            // 2.3.0.0.9 起 TAG 已混淆
            bridge.findMethod {
                matcher {
                    addCaller {
                        addInvoke("Ljava/util/concurrent/locks/ReentrantLock;->lock()V")
                        addInvoke("Ljava/lang/Object;->equals(Ljava/lang/Object;)Z")
                    }
                    addUsingField {
                        name = "DEVICE"
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
            if (method.parameterCount == 1) {
                // 公共解锁特定机型定制画框使用限制
                other(method)
            } else {
                val action = actions[index]
                action(method)
                index = (index + 1) % actions.size
            }
        }

        if (isLeica && !is24012Version && methodB != null) {
            // 1.10.0.0.6 新增 Xiaomi 15 Ultra 独占定制画框
            // 2.4.0.1.2 取消独占状态
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
            val target = runCatching { findClass($$"$${base}$a") }.getOrNull()
                ?: runCatching { findClass($$"$${base}$2") }.getOrNull()

            if (target != null) {
                target.findMethodOrNull { name("invoke") }
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

                JSONObject::class.java.findMethod { name("optJSONObject") }
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

