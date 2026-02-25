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
package com.sevtinge.hyperceiler.libhook.rules.home.dock

import android.graphics.Point
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.addMiBackgroundBlendColor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.clearAllBlur
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.clearMiBackgroundBlendColor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setBlurRoundRect
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setMiBackgroundBlendColors
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setMiViewBlurMode
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callStaticMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldAs
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method
import java.util.function.Consumer

object DockCustomNew : BaseHook() {
    private val launcherClass by lazy {
        loadClassOrNull("com.miui.home.launcher.BaseLauncher")
            ?: loadClass("com.miui.home.launcher.Launcher")
    }

    private val animationCompatComplexClass by lazy {
        loadClass("com.miui.home.launcher.compat.UserPresentAnimationCompatComplex")
    }

    private val folderBlurUtilsClass by lazy {
        findClass("com.miui.home.common.utils.MiuixMaterialBlurUtilities")
    }


    private val showAnimationLambda by lazy {
        DexKit.findMember("ShowAnimationLambda") { bridge ->
            bridge.findMethod {
                matcher {
                    declaredClass("com.miui.home.launcher.compat.UserPresentAnimationCompat", StringMatchType.StartsWith)
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

    private var isSupportHyperMaterialBlur = false

    @Suppress("UNCHECKED_CAST")
    override fun init() {
        val dockBgStyle = PrefsBridge.getStringAsInt("home_dock_add_blur", 0)
        var dockBlurView: View? = null

        launcherClass.afterHookMethod("setupViews") {
            val isAllApp = PrefsBridge.getBoolean("home_dock_bg_all_app")
            val dockBgColor = PrefsBridge.getInt("home_dock_bg_color", 0)
            val dockRadius = dp2px(PrefsBridge.getInt("home_dock_bg_radius", 30))
            val dockHeight = dp2px(PrefsBridge.getInt("home_dock_bg_height", 80))
            val dockMargin = dp2px(PrefsBridge.getInt("home_dock_bg_margin_horizontal", 30) - 6)
            val dockBottomMargin = dp2px(PrefsBridge.getInt("home_dock_bg_margin_bottom", 30) - 92)

            isSupportHyperMaterialBlur = if (isMoreHyperOSVersion(3f)) {
                folderBlurUtilsClass.callStaticMethod("isSupportHyperMaterialBlur") as Boolean
            } else {
                false
            }

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
            launcherClass.afterHookMethod("onDarkModeChanged") {

                isSupportHyperMaterialBlur = if (isMoreHyperOSVersion(3f)) {
                    folderBlurUtilsClass.callStaticMethod("isSupportHyperMaterialBlur") as Boolean
                } else {
                    false
                }

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
        } ?: XposedLog.d(TAG, lpparam.packageName, $$"can't find lambda$showUserPresentAnimation")
    }

    private fun View.addBlur() {
        val isDarkMode by lazy {
            if (AppsTool.isDarkMode(context) && PrefsBridge.getStringAsInt("home_other_home_mode", 0) == 0) {
                AppsTool.isDarkMode(context)
            } else {
                PrefsBridge.getStringAsInt("home_other_home_mode", 0) == 2
            }
        }

        clearMiBackgroundBlendColor()
        setMiViewBlurMode(1)

        if (isSupportHyperMaterialBlur) {
            val list: ArrayList<Point> = ArrayList()

            if (isDarkMode) {
                list.add(Point(1719105399, 19))
                list.add(Point(863270004, 15))
                list.add(Point(855638016, 3))
            } else {
                list.add(Point(-428575628, 15))
                list.add(Point(-1722658222, 18))
                list.add(Point(869388753, 3))
            }

            setMiBackgroundBlendColors(list)
        } else {
            if (isDarkMode) {
                addMiBackgroundBlendColor(0xB3767676.toInt(), 100)
                addMiBackgroundBlendColor(0xFF149400.toInt(), 106)
            } else {
                addMiBackgroundBlendColor(0x66B4B4B4, 100)
                addMiBackgroundBlendColor(0xFF2EF200.toInt(), 106)
            }
        }
    }
}
