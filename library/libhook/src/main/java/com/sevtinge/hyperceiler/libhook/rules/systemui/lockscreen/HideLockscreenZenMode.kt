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
package com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicBoolean
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

object HideLockscreenZenMode : BaseHook() {
    private val zenModeClass by lazy {
        loadClass("com.android.systemui.statusbar.notification.zen.ZenModeViewController")
    }

    override fun init() {
        if (isMoreHyperOSVersion(4f)) {
            hookOS4()
            return
        }
        // hyperOS fix by hyper helper
        zenModeClass.findMethod { filter { name.startsWith("updateVisibility") } }
            .createBeforeHook {
                it.thisObject.setObjectField("manuallyDismissed", true)
            }
    }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun hookOS4() {
        val active = AtomicBoolean(false)
        val handles = ArrayList<XposedInterface.HookHandle>()
        try {
            val store = requiredClass("com.android.notification.tinypanel.FlipNotifDataStore")
            val media = requiredClass("com.android.systemui.media.controls.shared.model.MediaData")
            val merge = store.getDeclaredMethod("merge", Boolean::class.java, media, List::class.java).apply {
                check(!Modifier.isStatic(modifiers) && returnType == List::class.java)
                isAccessible = true
            }
            val view = requiredClass("com.miui.systemui.notification.view.NotificationNumStateView")
            val collector = requiredClass(
                "com.miui.systemui.notification.view.viewbinder.NotificationNumStateViewBinder\$bind\$1\$1\$1\$1"
            )
            val continuation = requiredClass("kotlin.coroutines.Continuation")
            check(requiredClass("kotlinx.coroutines.flow.FlowCollector").isAssignableFrom(collector))
            val classId = collector.getDeclaredField("\$r8\$classId").apply {
                check(type == Int::class.java && !Modifier.isStatic(modifiers))
                isAccessible = true
            }
            val targetView = collector.getDeclaredField("\$view").apply {
                check(type == view && !Modifier.isStatic(modifiers))
                isAccessible = true
            }
            val emit = collector.getDeclaredMethod("emit", Any::class.java, continuation).apply {
                check(!Modifier.isStatic(modifiers) && returnType == Any::class.java)
                isAccessible = true
            }
            handles.add(checkNotNull(hookMethod(merge, object : IMethodHook {
                override fun before(param: HookParam) {
                    if (!active.get() || param.isSkipped || param.thisObject?.javaClass != store) return
                    // This argument controls only the tiny lockscreen's merged UI list.
                    // Run the original no-Zen branch, including media/filtering/capacity.
                    // Do not dismiss notifications or change the system's Zen setting.
                    if (param.args[0] is Boolean) param.args[0] = false
                }
            })))
            handles.add(checkNotNull(hookMethod(emit, object : IMethodHook {
                // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
                @Suppress("TooGenericExceptionCaught")
                override fun before(param: HookParam) {
                    if (!active.get() || param.isSkipped || param.thisObject?.javaClass != collector) return
                    try {
                        // R8 id0 alone consumes the lockscreen Zen presentation Boolean.
                        // Other ids control count/visibility/color/scale and must keep their inputs.
                        if (classId.getInt(param.thisObject) != 0 || !view.isInstance(targetView.get(param.thisObject))) return
                        if (param.args[0] is Boolean) param.args[0] = false
                    } catch (error: ReflectiveOperationException) {
                        debugCallbackError("OS4 lockscreen Zen consumer", error)
                    } catch (error: RuntimeException) {
                        debugCallbackError("OS4 lockscreen Zen consumer", error)
                    }
                }
            })))
            active.set(true)
        } catch (error: Exception) {
            active.set(false)
            for (handle in handles) {
                try { handle.unhook() } catch (rollback: RuntimeException) { error.addSuppressed(rollback) }
            }
            throw IllegalStateException("OS4 lockscreen Zen UI shape or registration unavailable", error)
        }
    }

    private fun requiredClass(name: String): Class<*> =
        findClassIfExists(name) ?: throw IllegalStateException("Missing OS4 lockscreen Zen class: $name")
}
