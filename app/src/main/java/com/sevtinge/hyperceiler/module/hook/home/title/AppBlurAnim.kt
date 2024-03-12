package com.sevtinge.hyperceiler.module.hook.home.title

import android.app.*
import android.view.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.blur.*
import de.robv.android.xposed.*
import java.util.concurrent.*

object AppBlurAnim : BaseHook() {
    private val appsBlurRadius by lazy {
        mPrefsMap.getInt("home_title_app_blur_radius", 100)
    }
    private val appsDimAlpha by lazy {
        mPrefsMap.getInt("home_title_app_dim_alpha", 0)
    }
    private val fixSmallWindowAnim by lazy {
        mPrefsMap.getBoolean("home_title_fix_small_window")
    }
    private var isStartingApp: Boolean = false

    private val blurUtil by lazy {
        loadClass("com.miui.home.launcher.common.BlurUtilities")
    }
    private val blur by lazy {
        loadClass("com.miui.home.launcher.common.BlurUtils")
    }
    private val mainThreadExecutor by lazy {
        loadClass("com.miui.home.recents.TouchInteractionService")
            .getStaticObjectFieldOrNull("MAIN_THREAD_EXECUTOR") as Executor
    }
    private val overviewState by lazy {
        loadClass("com.miui.home.launcher.LauncherState")
            .getStaticObjectFieldOrNull("OVERVIEW")
    }

    // by HyperHelper
    override fun init() {
        var transitionBlurView : MiBlurView? = null

        blurUtil.methodFinder().filterByName("setBackgroundBlurEnabled")
            .filterStatic().first()
            .hookAfterMethod { param ->
                val launcher = param.args[0]
                transitionBlurView = MiBlurView(launcher as Activity)
                transitionBlurView?.let {
                    it.setBlur(appsBlurRadius)
                    it.setDim(appsDimAlpha)
                }

                val viewGroup = XposedHelpers.getObjectField(launcher, "mLauncherView") as ViewGroup
                viewGroup.addView(transitionBlurView, viewGroup.indexOfChild(
                    XposedHelpers.getObjectField(launcher, "mOverviewPanel") as View
                ).coerceAtLeast(0))
            }

        // Remove blur view from Launcher
        loadClass("com.miui.home.recents.views.FloatingIconView").methodFinder()
            .filterByName("onLauncherDestroy").first()
            .hookAfterMethod {
                val viewGroup =
                    XposedHelpers.getObjectField(it.args[0], "mLauncherView") as ViewGroup
                if (transitionBlurView?.isAttachedToWindow == true) {
                    viewGroup.removeView(transitionBlurView)
                }
                transitionBlurView = null
            }

        // Blur when launching app
        blur.methodFinder().filterByName("fastBlurWhenStartOpenOrCloseApp")
            .first().hookBeforeMethod {
                val isOpen = it.args[0] as Boolean
                if (isOpen) {
                    transitionBlurView?.show(true)
                    isStartingApp = true
                }
                else {
                    // "isOpen" seems to always be true
                    transitionBlurView?.show(false)
                    transitionBlurView?.hide(true)
                }
                it.result = null
            }

        // Reset blur after launching the app
        blur.methodFinder().filterByName("fastBlurWhenStartOpenOrCloseApp")
            .first().replaceMethod {
                transitionBlurView?.hide(false)
                 if (isStartingApp) {
                     isStartingApp = false
                 }
            }

        // Widely used
        blur.methodFinder().filterByName("fastBlurWhenUseCompleteRecentsBlur")
            .first().replaceMethod {
                val useAnim = it.args[2] as Boolean
                mainThreadExecutor.execute {
                    if (useAnim) {
                        transitionBlurView?.show(true, it.args[1] as Float)

                        isStartingApp = false
                    } else if (isStartingApp) {
                        transitionBlurView?.restore()
                    } else {
                        transitionBlurView?.show(false, it.args[1] as Float)
                    }
                }
            }

        // Use with "fastBlurWhenUseCompleteRecentsBlur"
        blur.methodFinder().filterByName("resetBlurWhenUseCompleteRecentsBlur")
            .first().replaceMethod {
                mainThreadExecutor.execute {
                    val useAnim = it.args[1] as Boolean
                    transitionBlurView?.hide(useAnim)
                }
            }

        // Blur when entering recent tasks
        // Skip when triggered by a gesture in the app
        blur.methodFinder().filterByName("fastBlurWhenEnterRecents")
            .first().replaceMethod {
                if (XposedHelpers.getBooleanField(it.args[1], "mIsFromFsGesture")) {
                    return@replaceMethod null
                }
                transitionBlurView?.show(it.args[2] as Boolean)
            }

        // Reset blur when exiting recent tasks
        // Skip when triggered by a gesture in the app
        blur.methodFinder().filterByName("fastBlurWhenExitRecents")
            .first().replaceMethod {
                if (XposedHelpers.getBooleanField(it.args[1], "mIsFromFsGesture")) {
                    return@replaceMethod null
                }
                val useAnim = it.args[2] as Boolean
                // Forced animation to avoid flickering when opening a small window
                // not sure if it has a negative effect for now
                transitionBlurView?.hide(useAnim || fixSmallWindowAnim)
            }

        // Reset blur, widely used
        blur.methodFinder().filterByName("fastBlurWhenExitRecents")
            .first().replaceMethod {
                mainThreadExecutor.execute {
                    val useAnim = it.args[1] as Boolean
                    if (isStartingApp && !useAnim) {
                        transitionBlurView?.hide(true)
                    } else {
                        transitionBlurView?.hide(useAnim)
                    }
                    isStartingApp = false
                }
            }

        // Beginning of the uncertainty section
        blur.methodFinder().filterByName("fastBlurWhenEnterMultiWindowMode")
            .first().replaceMethod {
                if (
                    XposedHelpers.getObjectField(
                        XposedHelpers.getObjectField(it.args[0] ?: return@replaceMethod null,"mStateManager"),
                        "mState"
                    ) == overviewState
                ) {
                    transitionBlurView?.show(it.args[1] as Boolean)
                } else {
                    // 原逻辑没有 else 分支
                }
            }

        blur.methodFinder().filterByName("fastBlurWhenGestureResetTaskView")
            .first().replaceMethod {
                if (
                    XposedHelpers.getObjectField(
                        XposedHelpers.getObjectField(it.args[0] ?: return@replaceMethod null,"mStateManager"),
                        "mState"
                    ) == overviewState
                ) {
                    transitionBlurView?.show(it.args[1] as Boolean)
                } else {
                    // 原逻辑没有 else 分支
                }
            }

        blur.methodFinder().filterByName("restoreBlurRatioAfterAndroidS")
            .first().replaceMethod {
                transitionBlurView?.restore(true)
            }

        // 模糊缺失暂时不写.png
    }
}