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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.home.title

import android.app.*
import android.view.*
import android.view.animation.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.blur.*
import com.sevtinge.hyperceiler.utils.log.*
import de.robv.android.xposed.*
import java.util.concurrent.*

object AppBlurAnim : BaseHook() {
    private val appsBlurRadius by lazy {
        mPrefsMap.getInt("home_title_app_blur_radius", 100)
    }
    private val appsDimAlpha by lazy {
        mPrefsMap.getInt("home_title_app_dim_alpha", 50)
    }
    private val wallBlurRadius by lazy {
        mPrefsMap.getInt("home_title_app_blur_radius", 100)
    }
    private val wallDimAlpha by lazy {
        mPrefsMap.getInt("home_title_app_dim_alpha", 50)
    }
    private val minusBlurRadius by lazy {
        mPrefsMap.getInt("home_title_app_blur_radius", 100)
    }
    private val minusDimAlpha by lazy {
        mPrefsMap.getInt("home_title_app_dim_alpha", 50)
    }
    private val minusOverlapMode by lazy {
        true
    }
    private val minusShowLaunch by lazy {
        true
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
        var wallpaperBlurView : MiBlurView? = null
        var minusBlurView : MiBlurView? = null

        blurUtil.methodFinder().filterByName("setBackgroundBlurEnabled")
            .filterStatic().first()
            .hookAfterMethod { param ->
                val launcher = param.args[0]
                transitionBlurView = MiBlurView(launcher as Activity)
                transitionBlurView?.let {
                    it.setBlur(appsBlurRadius)
                    it.setDim(appsDimAlpha)
                    //it.setBackgroundColor(1204495145)
                    it.setPassWindowBlur(true)
                }

                wallpaperBlurView = MiBlurView(launcher)
                wallpaperBlurView?.let {
                    it.setBlur(wallBlurRadius)
                    it.setDim(wallDimAlpha)
                    //it.setBackgroundColor(1204495145)
                    it.setPassWindowBlur(true)
                }

                minusBlurView = MiBlurView(launcher)
                minusBlurView?.let {
                    it.setBlur(minusBlurRadius)
                    it.setDim(minusDimAlpha)
                    it.setNonlinear(false, LinearInterpolator())
                    it.setPassWindowBlur(true)
                }

                val viewGroup = XposedHelpers.getObjectField(launcher, "mLauncherView") as ViewGroup
                viewGroup.addView(transitionBlurView, viewGroup.indexOfChild(
                    XposedHelpers.getObjectField(launcher, "mOverviewPanel") as View
                ).coerceAtLeast(0))
                viewGroup.addView(wallpaperBlurView, 0)
                if (minusOverlapMode) {
                    viewGroup.addView(minusBlurView)
                }
                else {
                    viewGroup.addView(minusBlurView, 0)
                }
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
                if (wallpaperBlurView?.isAttachedToWindow == true) {
                    viewGroup.removeView(wallpaperBlurView)
                }
                if (minusBlurView?.isAttachedToWindow == true) {
                    viewGroup.removeView(minusBlurView)
                }
                transitionBlurView = null
                wallpaperBlurView = null
                minusBlurView = null
            }

        blur.methodFinder().filterByName("fastBlur").filterByParamCount(3)
            .first().hookBeforeMethod {
                wallpaperBlurView?.show(it.args[2] as Boolean, it.args[0] as Float)
                it.result = null
            }

        blur.methodFinder().filterByName("fastBlur").filterByParamCount(4)
            .first().hookBeforeMethod {
                wallpaperBlurView?.showWithDuration(it.args[2] as Boolean, it.args[0] as Float, 350)
                it.result = null
            }

        // Blur when launching app
        blur.methodFinder().filterByName("fastBlurWhenStartOpenOrCloseApp")
            .first().hookBeforeMethod {
                val isOpen = it.args[0] as Boolean
                if (isOpen) {
                    XposedLogUtils.logD("111")
                    transitionBlurView?.show(true)
                    isStartingApp = true
                }
                else {
                    XposedLogUtils.logD("222")
                    // "isOpen" seems to always be true
                    if (shouldBlurWallpaper(it.args[1] ?: return@hookBeforeMethod)) {
                        wallpaperBlurView?.show(false)
                    }
                    transitionBlurView?.show(false)
                    transitionBlurView?.hide(true)
                }
                it.result = null
            }

        blur.methodFinder().filterByName("fastBlurWhenFinishOpenOrCloseApp")
            .first().replaceMethod {
                transitionBlurView?.hide(false)
                if (shouldBlurWallpaper(it.args[0] ?: return@replaceMethod null)) {
                    wallpaperBlurView?.show(false)
                }
                else {
                    wallpaperBlurView?.hide(false)
                }
                if (isStartingApp) {
                    isStartingApp = false
                } else {

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
                    if (shouldBlurWallpaper(it.args[0] ?: return@execute)) {
                        wallpaperBlurView?.show(false)
                    }
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
                if (shouldBlurWallpaper(it.args[0] ?: return@replaceMethod null)) {
                    wallpaperBlurView?.show(false)
                }
                // Forced animation to avoid flickering when opening a small window
                // not sure if it has a negative effect for now
                transitionBlurView?.hide(useAnim || fixSmallWindowAnim)
            }


        blur.methodFinder().filterByName("resetBlur")
            .first().replaceMethod {
                mainThreadExecutor.execute {
                    val useAnim = it.args[1] as Boolean
                    if (shouldBlurWallpaper(it.args[0] ?: return@execute)) {
                        wallpaperBlurView?.show(false)
                    }
//                        if (useAnim) {
//                            transitionBlurView.show(false)
//                        }
                    if (isStartingApp && !useAnim) {
                        transitionBlurView?.hide(true)
                    }
                    else {
                        transitionBlurView?.hide(useAnim)
                    }
                    isStartingApp = false
                }
            }

        blur.methodFinder().filterByName("fastBlurWhenEnterFolderPicker")
            .first().replaceMethod {
                wallpaperBlurView?.showWithDuration(
                    it.args[2] as Boolean, it.args[1] as Float, it.args[3] as Int
                )
            }

        blur.methodFinder().filterByName("fastBlurWhenExitFolderPicker")
            .first().replaceMethod {
                val useAnim = it.args[2] as Boolean
                if (shouldBlurWallpaper(it.args[0] ?: return@replaceMethod null)) {
                    return@replaceMethod null
                }
                if (
                    XposedHelpers.getObjectField(
                        XposedHelpers.getObjectField(
                            it.args[0],"mStateManager"
                        ),
                        "mState"
                    ) == overviewState
                ) {
                    return@replaceMethod null
                }
                wallpaperBlurView?.showWithDuration(
                    useAnim, it.args[1] as Float, it.args[3] as Int
                )
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

        blur.methodFinder().filterByName("fastBlurWhenOpenOrCloseFolder")
            .first().replaceMethod {
                val useAnim = it.args[1] as Boolean
                if (shouldBlurWallpaper(it.args[0] ?: return@replaceMethod null)) {
                    wallpaperBlurView?.show(useAnim)
                }
                else {
                    wallpaperBlurView?.hide(useAnim)
                }
            }

        // 模糊缺失暂时不写.png
    }

    private fun shouldBlurWallpaper(launcher: Any): Boolean {
//        val isInNormalEditing = XposedHelpers.callMethod(launcher, "isInNormalEditing") as Boolean
//        val isFoldShowing = XposedHelpers.callMethod(launcher, "isFolderShowing") as Boolean
        return (XposedHelpers.callMethod(launcher, "isShouldBlur") as Boolean)
    }
}