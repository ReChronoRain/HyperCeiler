package com.sevtinge.hyperceiler.hook.module.hook.systemui.navigation

import android.content.Context
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.view.Surface
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHooks
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook

object RotationButtonB : BaseHook() {

    var isListen: Boolean = false
    private val enable by lazy {
        mPrefsMap.getStringAsInt("system_framework_other_rotation_button_int", 0) != 1
    }
    private val navigationBar by lazy {
        loadClass("com.android.systemui.navigationbar.views.NavigationBar")
    }

    override fun init() {
        navigationBar.constructorFinder()
            .toList().createAfterHooks {
                if (!enable) return@createAfterHooks

                val mContext =
                    it.thisObject.getObjectFieldAs("mContext") as Context?
                if (!isListen) {
                    if (mContext == null) {
                        logE(TAG, "context can't is null!")
                        return@createAfterHooks
                    }

                    val contentObserver: ContentObserver =
                        object : ContentObserver(Handler(mContext.mainLooper)) {
                            override fun onChange(selfChange: Boolean, uri: Uri?) {
                                val isShow: Boolean = getBoolean(mContext)
                                val rotation: Int = getInt(mContext)
                                it.thisObject.callMethod(
                                    "onRotationProposal",
                                    rotation,
                                    isShow
                                )
                            }
                        }

                    mContext.contentResolver.registerContentObserver(
                        Settings.System.getUriFor("rotation_button_data"),
                        false, contentObserver
                    )
                    isListen = true
                }
            }

        loadClass($$$"com.android.systemui.navigationbar.views.NavigationBarView$$ExternalSyntheticLambda1")
            .methodFinder().filterByName("get")
            .first().createBeforeHook {
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
                    logE(TAG, "Unknown parameters, unable to continue execution, execute the original method!")
                    return@createBeforeHook
                }
                it.result = intValue
            }

        navigationBar.methodFinder()
            .filterByName("onRotationProposal")
            .first().createBeforeHook {
                if (!enable) {
                    it.result = null
                }
            }

        loadClass($$$"com.android.systemui.shared.rotation.RotationButtonController$$ExternalSyntheticLambda5")
            .methodFinder().filterByName("onClick")
            .first().createBeforeHook {
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
