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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api

import android.content.Context
import android.os.Handler
import com.sevtinge.hyperceiler.hook.utils.callMethodAs
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.getStaticObjectFieldAs
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import java.util.concurrent.Executor

/**
 * Only for HyperOS2
 */
@Suppress("unused")
object MiuiStub {
    private val INSTANCE by lazy {
        loadClass("miui.stub.MiuiStub").getStaticObjectFieldAs<Any>("INSTANCE")
    }

    lateinit var javaAdapter: JavaAdapter

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
        loadClass("com.android.systemui.util.kotlin.JavaAdapter").constructorFinder()
            .first()
            .createAfterHook {
                javaAdapter = JavaAdapter(it.thisObject)
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
                instance.getObjectFieldAs<Any>("mFlashlightController").callMethodAs("get")
            )
        }

        val activityStarter by lazy {
            ActivityStarter(
                instance.getObjectFieldAs<Any>("mActivityStarter").callMethodAs("get")
            )
        }
    }
}
