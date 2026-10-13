package com.sevtinge.hyperceiler.libhook.rules.home.dock

import android.content.Context
import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.util.Log
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.provider.DockGlassRendererEndpoint
import com.sevtinge.hyperceiler.libhook.provider.DockGlassRendererProtocol
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import java.util.concurrent.atomic.AtomicBoolean

/** Uses SystemUI's existing UI/RenderThread, not a service or another HyperCeiler process. */
class DockGlassRendererSystemUI : BaseHook() {
    private val installed = AtomicBoolean()
    @Volatile private var closed = false
    private var endpoint: DockGlassRendererEndpoint? = null
    private var registrationThread: HandlerThread? = null

    override fun init() {
        val processName = Application.getProcessName()
        if (processName != "com.android.systemui") {
            // Unconditional logcat: raw Log bypasses the module log-level gate. A silent return
            // here made a previous failure mode invisible for an entire investigation round.
            Log.i("HyperCeiler.DockGlass",
                "SystemUI renderer skipped: process=$processName uid=${Process.myUid()}")
            return
        }
        // OEM SystemUI Application names are obfuscated on newer builds. Use the framework
        // lifecycle dispatcher, after the real Application finished initialization.
        loadClass("android.app.Instrumentation").getDeclaredMethod(
            "callApplicationOnCreate", Application::class.java)
            .createAfterHook { param -> install(param.args[0] as Context) }
        // Handle a module hot reload after Application.onCreate has already finished.
        val current = loadClass("android.app.ActivityThread").getDeclaredMethod("currentApplication")
            .invoke(null) as? Context
        if (current != null && current.packageName == "com.android.systemui") install(current)
        registerHotReloadCleanup {
            closed = true
            endpoint?.close()
            registrationThread?.quitSafely()
        }
    }

    private fun install(context: Context) {
        // Wrap the whole body, not just the endpoint constructor: anything thrown before the
        // register loop starts must still leave a trace, otherwise the renderer silently never
        // appears and only the client-side "dependency unavailable" retries are visible.
        try {
            installLocked(context)
        } catch (error: Exception) {
            installed.set(false)
            Log.w("HyperCeiler.DockGlass",
                "SystemUI renderer setup failed uid=${Process.myUid()} pkg=${context.packageName}", error)
        }
    }

    private fun installLocked(context: Context) {
        if (closed || !installed.compareAndSet(false, true)) return
        val renderer = DockGlassRendererEndpoint(context)
        endpoint = renderer
        val thread = HandlerThread("HC-DockRenderer-Register").apply { start() }
        registrationThread = thread
        val handler = Handler(thread.looper)
        fun register(attempt: Int) {
            if (closed) return
            try {
                val window = loadClass("android.os.ServiceManager").getDeclaredMethod(
                    "getService", String::class.java).invoke(null, "window") as? IBinder
                    ?: error("Window service not ready")
                DockGlassRendererProtocol.register(window, renderer)
                Log.i("HyperCeiler.DockGlass",
                    "SystemUI renderer registered; app process not required uid=${Process.myUid()}")
                thread.quitSafely()
            } catch (error: Exception) {
                if (attempt < 8) handler.postDelayed({ register(attempt + 1) },
                    minOf(500L shl attempt, 8_000L))
                else {
                    Log.w("HyperCeiler.DockGlass",
                        "SystemUI renderer registration failed uid=${Process.myUid()}", error)
                    thread.quitSafely()
                }
            }
        }
        handler.post { register(0) }
    }
}
