package com.sevtinge.hyperceiler.libhook.rules.systemui.navigation

import android.content.Context
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.view.Surface
import android.view.View
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import java.lang.reflect.Method
import java.util.Collections
import java.util.WeakHashMap

object RotationButtonB : BaseHook() {

    private const val STATE_NAVIGATION_BAR = "RotationButtonB.navigationBar"
    private const val STATE_CONTEXT = "RotationButtonB.context"

    var isListen: Boolean = false
    private val enable by lazy {
        PrefsBridge.getStringAsInt("system_framework_other_rotation_button_int", 0) != 1
    }
    private val navigationBar by lazy {
        loadClass("com.android.systemui.navigationbar.views.NavigationBar")
    }
    private val providerContexts = Collections.synchronizedMap(WeakHashMap<Any, Context>())
    private val hookedProviderMethods = Collections.synchronizedSet(mutableSetOf<Method>())

    override fun init() {
        navigationBar.findMethod { name("onRotationProposal") }.createBeforeHook {
            if (!enable) it.result = null
        }
        // Disabling suggestions does not need any of the forced-mode view hooks.
        if (!enable) return

        val restoredNavigationBar = getHotReloadRuntimeState(
            STATE_NAVIGATION_BAR,
            Any::class.java
        )
        val restoredContext = getHotReloadRuntimeState(STATE_CONTEXT, Context::class.java)
        if (restoredNavigationBar != null && restoredContext != null) {
            ensureRotationObserver(restoredNavigationBar, restoredContext)
            (restoredNavigationBar.getObjectField("mView") as? View)?.let(::bindRotationProvider)
        }

        Constructors.find(navigationBar)
            .toList().createAfterHooks {
                val mContext =
                    it.thisObject.getObjectFieldAs("mContext") as Context?
                ensureRotationObserver(it.thisObject, mContext)
            }
        // R8/AOT can inline NavigationBar construction as well. The view lifecycle
        // still provides a fully initialized controller and Context.
        navigationBar.findAllMethods { name("onViewAttached") }.createAfterHooks {
            val context = it.thisObject.getObjectField("mContext") as? Context
            ensureRotationObserver(it.thisObject, context)
            (it.thisObject.getObjectField("mView") as? View)?.let(::bindRotationProvider)
        }

        val navigationBarView = loadClass("com.android.systemui.navigationbar.views.NavigationBarView")
        Constructors.find(navigationBarView).toList().createAfterHooks {
            bindRotationProvider(it.thisObject as View)
        }
        // Newer SystemUI creates the controller here instead of in the view constructor.
        navigationBarView.findAllMethods { name("setRotationPolicyWrapper") }.createAfterHooks {
            bindRotationProvider(it.thisObject as View)
        }

        loadClass($$$"com.android.systemui.shared.rotation.RotationButtonController$$ExternalSyntheticLambda5").findMethod { name("onClick") }.createBeforeHook {
                val rotationButtonController = it.thisObject.getObjectField("f$0")
                if (enable && rotationButtonController != null) {
                    rotationButtonController.callMethod(
                        "setRotateSuggestionButtonState",
                        true,
                        false
                    )
                }
        }
    }

    private fun bindRotationProvider(view: View) {
        val controller = view.getObjectField("mRotationButtonController") ?: return
        val provider = controller.getObjectField("mWindowRotationProvider") ?: return
        providerContexts[provider] = view.context
        val getter = provider.javaClass.findMethod { name("get"); paramCount(0) }
        if (!hookedProviderMethods.add(getter)) return
        // R8 renumbers synthetic lambdas (Lambda1 became Lambda0 on Android 17).
        // Resolve the actual supplier and limit the override to registered instances.
        getter.createBeforeHook {
            val context = providerContexts[it.thisObject]
            if (context != null) {
                when (getScreenOrientation(context)) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> it.result = Surface.ROTATION_0
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> it.result = Surface.ROTATION_90
                }
            }
        }
        XposedLog.i(TAG, lpparam.packageName, "Rotation provider hook: ${provider.javaClass.name}")
    }

    private fun ensureRotationObserver(navigationBar: Any, context: Context?) {
        if (isListen) return
        if (context == null) {
            XposedLog.e(TAG, lpparam.packageName, "context can't is null!")
            return
        }
        val contentObserver: ContentObserver =
            object : ContentObserver(Handler(context.mainLooper)) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    val isShow = getBoolean(context)
                    val rotation = getInt(context)
                    navigationBar.callMethod("onRotationProposal", rotation, isShow)
                }
            }

        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor("rotation_button_data"), false, contentObserver
        )
        isListen = true
        XposedLog.i(TAG, lpparam.packageName, "Rotation suggestion observer registered")
        BaseHook.registerContentObserverHotReloadCleanup(context.contentResolver, contentObserver)
        BaseHook.putHotReloadRuntimeState(STATE_NAVIGATION_BAR, navigationBar)
        BaseHook.putHotReloadRuntimeState(STATE_CONTEXT, context)
    }

    fun getScreenOrientation(context: Context): Int {
        val display = context.display
        val rotation = display.rotation
        // 获取屏幕方向
        return when (rotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Surface.ROTATION_90, Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun getData(context: Context): String? {
        return Settings.System.getString(context.contentResolver, "rotation_button_data")
    }

    private fun getBoolean(context: Context): Boolean {
        val data = getData(context) ?: return false
        val sp = data.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val s = java.util.ArrayList<String>(listOf<String?>(*sp))[1]
        return s.contains("true")
    }

    private fun getInt(context: Context): Int {
        val data = getData(context) ?: return -1
        val sp = data.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return ArrayList(listOf<String?>(*sp))[0]!!.toInt()
    }
}
