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
package com.sevtinge.hyperceiler.module.hook.home.dock

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.module.base.tool.AppsTool
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.addMiBackgroundBlendColor
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.clearAllBlur
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.clearMiBackgroundBlendColor
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setBlurRoundRect
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setMiViewBlurMode
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.utils.hookAfterMethod
import java.lang.reflect.Method
import java.util.function.Consumer

object DockCustomNew : BaseHook() {
    private val launcherClass by lazy {
        loadClass("com.miui.home.launcher.Launcher")
    }

    private val animationCompatComplexClass by lazy {
        loadClass("com.miui.home.launcher.compat.UserPresentAnimationCompatComplex")
    }

    private val showAnimationLambda by lazy {
        DexKit.findMember("ShowAnimationLambda") { bridge ->
            bridge.findMethod {
                matcher {
                    declaredClass("com.miui.home.launcher.compat.UserPresentAnimationCompatV12Phone")
                    addInvoke {
                        name = "conversionValueFrom3DTo2D"
                    }
                    addInvoke {
                        name = "setTranslationZ"
                    }
                }
            }.singleOrNull()
        } as Method?
    }

    override fun init() {
        val dockBgStyle = mPrefsMap.getStringAsInt("home_dock_add_blur", 0)
        var dockBlurView: View? = null

        launcherClass.hookAfterMethod("setupViews") {
            val isAllApp = mPrefsMap.getBoolean("home_dock_bg_all_app")
            val dockBgColor = mPrefsMap.getInt("home_dock_bg_color", 0)
            val dockRadius = dp2px(mPrefsMap.getInt("home_dock_bg_radius", 30))
            val dockHeight = dp2px(mPrefsMap.getInt("home_dock_bg_height", 80))
            val dockMargin = dp2px(mPrefsMap.getInt("home_dock_bg_margin_horizontal", 30) - 6)
            val dockBottomMargin = dp2px(mPrefsMap.getInt("home_dock_bg_margin_bottom", 30) - 92)

            val hotSeats = it.thisObject.getObjectFieldAs<FrameLayout>("mHotSeats")
            dockBlurView = View(hotSeats.context).apply {
                if (dockBgStyle == 0) {
                   setBackgroundColor(dockBgColor)
                } else if (dockBgStyle == 1) {
                    doOnAttach {
                        addBlur()
                    }

                    doOnDetach {
                        clearAllBlur()
                    }
                }

                setBlurRoundRect(dockRadius)
            }

            hotSeats.addView(
                dockBlurView,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dockHeight).apply {
                    gravity = if (isAllApp) {
                        Gravity.TOP
                    } else {
                        Gravity.BOTTOM
                    }
                    setMargins(
                        dockMargin,
                        0,
                        dockMargin,
                        dockBottomMargin
                    )
                }
            )
        }

        if (dockBgStyle == 1) {
            launcherClass.hookAfterMethod("onDarkModeChanged") {
                dockBlurView?.addBlur()
            }
        }

        // 添加动画
        animationCompatComplexClass.methodFinder()
            .filterByName("operateAllPresentAnimationRelatedViews")
            .single()
            .createAfterHook {
                dockBlurView?.run {
                    val consumer = it.args[0] as Consumer<View>
                    consumer.accept(this)
                }
            }

        showAnimationLambda?.createAfterHook {
            val view = it.args[2] as View
            if (view == dockBlurView) {
                view.translationZ = 0F
            }
        } ?: logD(TAG, lpparam.packageName, "can't find lambda\$showUserPresentAnimation")
    }

    private fun View.addBlur() {
        clearMiBackgroundBlendColor()
        setMiViewBlurMode(1)

        if (AppsTool.isDarkMode(context)) {
            addMiBackgroundBlendColor(0xB3767676.toInt(), 100)
            addMiBackgroundBlendColor(0xFF149400.toInt(), 106)
        } else {
            addMiBackgroundBlendColor(0x66B4B4B4, 100)
            addMiBackgroundBlendColor(0xFF2EF200.toInt(), 106)
        }
    }
}
