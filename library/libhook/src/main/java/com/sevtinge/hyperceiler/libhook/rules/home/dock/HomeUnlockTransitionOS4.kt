// SPDX-License-Identifier: AGPL-3.0-or-later
package com.sevtinge.hyperceiler.libhook.rules.home.dock

import android.os.Parcel
import android.os.SystemClock
import android.view.WindowManager
import com.sevtinge.hyperceiler.common.log.ModuleLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import java.util.concurrent.atomic.AtomicBoolean

/** Necessary early signal only, independent of the legacy renderer's lifetime. */
class HomeUnlockTransitionOS4 : BaseHook() {
    private val signalTag = "HyperCeiler.HomeWindow"
    private val endpoint = HomeUnlockTransitionEndpointOS4 { ModuleLog.i(signalTag, it) }
    private val windowFailure = AtomicBoolean(false)
    @Volatile private var stopped = false

    override fun init() {
        registerHotReloadCleanup { stopped = true; endpoint.close() }
        val window = loadClass("com.android.server.wm.WindowState")
        val attrs = window.getDeclaredField("mAttrs").apply { isAccessible = true }
        val session = window.getDeclaredField("mSession").apply { isAccessible = true }
        val uid = session.type.getDeclaredField("mUid").apply { isAccessible = true }
        val pid = session.type.getDeclaredField("mPid").apply { isAccessible = true }
        // Window construction/removal only, no reflective scan on every frame.
        window.declaredConstructors.forEach { constructor ->
            constructor.isAccessible = true
            constructor.createAfterHook { param ->
                if (stopped) return@createAfterHook
                runCatching {
                    val value = attrs.get(param.thisObject) as? WindowManager.LayoutParams ?: return@runCatching
                    if (!DockWindowPolicy.isLauncherWindow(value.packageName, value.title.toString(),
                            value.type, param.thisObject.callMethod("getDisplayId") as Int)) return@runCatching
                    val owner = session.get(param.thisObject) ?: return@runCatching
                    endpoint.bind(param.thisObject, uid.getInt(owner), pid.getInt(owner))
                }.onFailure { if (windowFailure.compareAndSet(false, true))
                    ModuleLog.w(signalTag, "early unlock window identity unavailable", it) }
            }
        }
        // Read the same platform predicate as the existing renderer, after
        // actual visibility edges. No reflective search or polling on frames.
        runCatching { HomeWindowVisibilityContract.resolve(window) }.onSuccess { contract ->
            contract.edges().forEach { edge ->
                edge.createAfterHook { param ->
                    if (!stopped && endpoint.owns(param.thisObject)) {
                        runCatching { contract.visible().invoke(param.thisObject) as Boolean }
                            .onSuccess { endpoint.visibility(param.thisObject, it) }
                            .onFailure { if (windowFailure.compareAndSet(false, true))
                                ModuleLog.w(signalTag, "platform window visibility read unavailable", it) }
                    }
                }
            }
            ModuleLog.i(signalTag, "window visibility edges ready: " + contract.edges().joinToString { it.toGenericString() })
        }.onFailure { ModuleLog.w(signalTag, "window visibility source unavailable, no polling fallback", it) }
        window.getDeclaredMethod("removeImmediately").apply { isAccessible = true }.createBeforeHook { param ->
            if (!stopped) endpoint.removed(param.thisObject)
        }
        loadClass("android.view.IWindowManager\$Stub").getDeclaredMethod("onTransact", Integer.TYPE,
            Parcel::class.java, Parcel::class.java, Integer.TYPE).apply { isAccessible = true }.createBeforeHook { param ->
            if (stopped || param.args[0] != HomeUnlockTransitionEndpointOS4.TRANSACTION) return@createBeforeHook
            val reply = param.args[2] as? Parcel ?: return@createBeforeHook
            try { endpoint.receive(param.args[1] as Parcel, reply, param.args[3] as Int) }
            catch (error: Exception) { reply.writeException(error) }
            param.result = true
        }
        // The same platform start boundary already used by the original Dock.
        // setKeyguardShown(false)/locked-state notification was measured 374 ms
        // later on this ROM and cannot replace this event without losing phase.
        for (name in listOf("com.android.server.wm.KeyguardController", "com.android.server.wm.ActivityTaskManagerService")) {
            val type = runCatching { loadClass(name) }.getOrNull() ?: continue
            val methods = generateSequence(type) { it.superclass }.flatMap { it.declaredMethods.asSequence() }
                .filter { it.name == "keyguardGoingAway" && it.returnType == Void.TYPE
                    && it.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType)) }.toList()
            if (methods.size != 1) continue
            methods.single().apply { isAccessible = true }.createBeforeHook { param ->
                if (!stopped) endpoint.transition(SystemClock.uptimeMillis(), param.args[0] as Int)
            }
            endpoint.ready()
            ModuleLog.i(signalTag, "early unlock time endpoint ready, source=${methods.single().toGenericString()}")
            return
        }
        endpoint.unavailable()
        ModuleLog.w(signalTag, "early unlock source unavailable, no repeated installation attempts")
    }
}
