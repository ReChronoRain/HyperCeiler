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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui

import android.content.Context
import android.os.Handler
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.base.BaseLoad
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getStaticObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.Executor

/**
 * 提供不同 HyperOS SystemUI 实现中的 MiuiStub 与 JavaAdapter 访问能力。
 */
@Suppress("unused")
object MiuiStub {
    private const val JAVA_ADAPTER = "com.android.systemui.util.kotlin.JavaAdapter"

    private val INSTANCE by lazy {
        loadClass("miui.stub.MiuiStub").getStaticObjectFieldAs<Any>("INSTANCE")
    }

    @Volatile
    private var systemJavaAdapter: JavaAdapter? = null

    private val javaAdapterClass by lazy {
        loadClassOrNull(JAVA_ADAPTER, BaseLoad.getClassLoader())
    }

    private val fallbackJavaAdapter by lazy {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        val clazz = requireNotNull(javaAdapterClass) { "$JAVA_ADAPTER is unavailable" }
        val constructor = clazz.declaredConstructors.first { it.parameterCount == 1 }
        constructor.isAccessible = true
        JavaAdapter(constructor.newInstance(scope))
    }

    val javaAdapter: JavaAdapter
        get() {
            systemJavaAdapter?.let { return it }

            val clazz = javaAdapterClass
            if (clazz != null) {
                val dependencyClass = loadClassOrNull(
                    "com.android.systemui.Dependency",
                    BaseLoad.getClassLoader()
                )
                val adapter = runCatching {
                    dependencyClass?.let { BaseHook.callStaticMethod(it, "get", clazz) }
                }.getOrNull()
                if (adapter != null) {
                    return JavaAdapter(adapter).also { systemJavaAdapter = it }
                }
            }

            return fallbackJavaAdapter
        }

    val baseProvider by lazy {
        BaseProvider(INSTANCE.getObjectFieldAs("mBaseProvider"))
    }

    val miuiModuleProvider by lazy {
        MiuiModuleProvider(INSTANCE.getObjectFieldAs("mMiuiModuleProvider"))
    }

    val sysUIProvider by lazy {
        SysUIProvider(INSTANCE.getObjectFieldAs("mSysUIProvider"))
    }

    @JvmStatic
    fun createHook() {
        val clazz = javaAdapterClass ?: return
        Constructors.find(clazz)
            .firstOrNull()
            ?.createAfterHook {
                systemJavaAdapter = JavaAdapter(it.thisObject)
            }
    }

    override fun toString(): String = INSTANCE.toString()

    class BaseProvider(instance: Any) : BaseReflectObject(instance) {
        val mainHandler by lazy {
            instance.getObjectFieldAs<Handler>("mMainHandler")
        }

        val bgHandler by lazy {
            instance.getObjectFieldAs<Handler>("mBgHandler")
        }

        val context by lazy {
            instance.getObjectFieldAs<Context>("mContext")
        }

        val uiBackgroundExecutor by lazy {
            instance.getObjectFieldAs<Executor>("mUiBackgroundExecutor")
        }
    }

    class MiuiModuleProvider(instance: Any) : BaseReflectObject(instance)

    class SysUIProvider(instance: Any) : BaseReflectObject(instance) {
        val flashlightController by lazy {
            FlashlightController(
                instance.getObjectFieldAs<Any>("mFlashlightController").callMethod("get") as Any
            )
        }

        val activityStarter by lazy {
            ActivityStarter(
                instance.getObjectFieldAs<Any>("mActivityStarter").callMethod("get") as Any
            )
        }
    }
}
