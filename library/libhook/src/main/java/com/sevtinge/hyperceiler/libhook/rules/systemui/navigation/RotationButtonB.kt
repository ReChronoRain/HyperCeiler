package com.sevtinge.hyperceiler.libhook.rules.systemui.navigation

import android.content.Context
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.view.Surface
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

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

    override fun init() {
        if (enable) {
            val restoredNavigationBar = getHotReloadRuntimeState(
                STATE_NAVIGATION_BAR,
                Any::class.java
            )
            val restoredContext = getHotReloadRuntimeState(STATE_CONTEXT, Context::class.java)
            if (restoredNavigationBar != null && restoredContext != null) {
                ensureRotationObserver(restoredNavigationBar, restoredContext)
            }
        }

        Constructors.find(navigationBar)
            .toList().createAfterHooks {
                if (!enable) return@createAfterHooks

                val mContext =
                    it.thisObject.getObjectFieldAs("mContext") as Context?
                ensureRotationObserver(it.thisObject, mContext)
            }

        loadClass($$$"com.android.systemui.navigationbar.views.NavigationBarView$$ExternalSyntheticLambda1").findMethod { name("get") }.createBeforeHook {
                if (!enable) return@createBeforeHook

                val navigationBarView = it.thisObject.getObjectField("f$0") ?: return@createBeforeHook
                val mLightContext =
                    navigationBarView.getObjectField("mLightContext") as Context
                val intValue = when (getScreenOrientation(mLightContext)) {
                    0 -> 1
                    1 -> 0
                    else -> -1
                }
                if (intValue == -1) {
                    XposedLog.e(TAG, lpparam.packageName, "Unknown parameters, unable to continue execution, execute the original method!")
                    return@createBeforeHook
                }
                it.result = intValue
            }

        navigationBar.findMethod { name("onRotationProposal") }.createBeforeHook {
                if (!enable) {
                    it.result = null
                }
            }

        loadClass($$$"com.android.systemui.shared.rotation.RotationButtonController$$ExternalSyntheticLambda5").findMethod { name("onClick") }.createBeforeHook {
                if (enable) {
                    val rotationButtonController =
                        it.thisObject.getObjectField("f$0") ?: return@createBeforeHook

                    rotationButtonController.callMethod(
                        "setRotateSuggestionButtonState",
                        true,
                        false
                    )
                }
        }
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
