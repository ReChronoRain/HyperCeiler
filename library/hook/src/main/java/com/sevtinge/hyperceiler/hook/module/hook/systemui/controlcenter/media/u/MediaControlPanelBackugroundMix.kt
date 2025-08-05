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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.u

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.MiuiStub
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaControlPanel
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.notificationUtil
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.playerTwoCircleView
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.seekBarObserver
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.statusBarStateControllerImpl
import com.sevtinge.hyperceiler.hook.utils.blur.MiBlurUtilsKt.setBlurRoundRect
import com.sevtinge.hyperceiler.hook.utils.blur.MiBlurUtilsKt.setMiBackgroundBlendColors
import com.sevtinge.hyperceiler.hook.utils.blur.MiBlurUtilsKt.setMiViewBlurMode
import com.sevtinge.hyperceiler.hook.utils.devicesdk.colorFilter
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isDarkMode
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import de.robv.android.xposed.XposedHelpers
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.helper.ObjectHelper.`-Static`.objectHelper
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook


// from https://github.com/YuKongA/MediaControl-BlurBg/blob/752de17a31d940683648cee7b957d4ff48d381a3/app/src/main/kotlin/top/yukonga/mediaControlBlur/MainHook.kt
class MediaControlPanelBackgroundMix : BaseHook() {

    private val isMode by lazy {
        mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0) == 2
    }

    override fun init() {
        runCatching {
            var lockScreenStatus: Boolean? = null
            var darkModeStatus: Boolean? = null

            // 导致拖动 SeekBar 改变歌曲标题/艺术家名字颜色的实际位置不在这里，目前暂时作为代替解决方案。
            if (isMoreHyperOSVersion(2f)) {
                seekBarObserver?.methodFinder()?.filterByName("onChanged")?.first()
                    ?.createBeforeHook {
                        val context = AndroidAppHelper.currentApplication().applicationContext
                        val isBackgroundBlurOpened =
                            XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context) as Boolean
                        val mMediaViewHolder =
                            it.thisObject.objectHelper().getObjectOrNullUntilSuperclass("holder")
                                ?: return@createBeforeHook
                        val titleText =
                            mMediaViewHolder.getObjectFieldOrNullAs<TextView>("titleText")
                        val artistText = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("artistText")
                        val grey = if (isDarkMode()) Color.LTGRAY else Color.DKGRAY
                        val color = if (isDarkMode()) Color.WHITE else Color.BLACK

                        if (isBackgroundBlurOpened) {
                            artistText?.setTextColor(grey)
                            titleText?.setTextColor(color)
                        }
                    }
            }

            var mediaControlPanelInstance: Any? = null
            miuiMediaControlPanel?.methodFinder()?.filterByName("bindPlayer")?.first()
                ?.createAfterHook {
                    mediaControlPanelInstance = it.thisObject
                    val context =
                        it.thisObject.objectHelper().getObjectOrNullUntilSuperclass("mContext") as? Context
                            ?: return@createAfterHook

                    val isBackgroundBlurOpened =
                        XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context)
                            as Boolean

                    val mMediaViewHolder =
                        it.thisObject.objectHelper().getObjectOrNullUntilSuperclass("mMediaViewHolder")
                            ?: return@createAfterHook

                    val action0 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action0")
                    val action1 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action1")
                    val action2 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action2")
                    val action3 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action3")
                    val action4 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action4")
                    val titleText = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("titleText")
                    val artistText = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("artistText")
                    val seamlessIcon = mMediaViewHolder.getObjectFieldOrNullAs<ImageView>("seamlessIcon")
                    val seekBar = mMediaViewHolder.getObjectFieldOrNullAs<SeekBar>("seekBar")
                    val elapsedTimeView = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("elapsedTimeView")
                    val totalTimeView = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("totalTimeView")

                    val grey = if (isDarkMode()) Color.LTGRAY else Color.DKGRAY
                    val color = if (isDarkMode()) Color.WHITE else Color.BLACK
                    seekBar?.thumb?.colorFilter = colorFilter(
                        if (isMode) Color.TRANSPARENT else grey
                    )
                    elapsedTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f)
                    totalTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f)
                    if (!isBackgroundBlurOpened) {
                        action0?.setColorFilter(color)
                        action1?.setColorFilter(color)
                        action2?.setColorFilter(color)
                        action3?.setColorFilter(color)
                        action4?.setColorFilter(color)
                        titleText?.setTextColor(Color.WHITE)
                        seamlessIcon?.setColorFilter(Color.WHITE)
                        seekBar?.progressDrawable?.colorFilter = colorFilter(grey)
                        seekBar?.thumb?.colorFilter = colorFilter(
                            if (isMode) Color.TRANSPARENT else grey
                        )
                    } else {
                        artistText?.setTextColor(grey)
                        elapsedTimeView?.setTextColor(grey)
                        totalTimeView?.setTextColor(grey)
                        if (!isMoreHyperOSVersion(2f)) titleText?.setTextColor(grey)
                        action0?.setColorFilter(color)
                        action1?.setColorFilter(color)
                        action2?.setColorFilter(color)
                        action3?.setColorFilter(color)
                        action4?.setColorFilter(color)
                        titleText?.setTextColor(color)
                        seamlessIcon?.setColorFilter(color)
                        seekBar?.progressDrawable?.colorFilter = colorFilter(grey)
                        seekBar?.thumb?.colorFilter = colorFilter(
                            if (isMode) Color.TRANSPARENT else grey
                        )
                    }
                }

            playerTwoCircleView?.methodFinder()?.filterByName("onDraw")?.first()
                ?.createBeforeHook {
                    val context = AndroidAppHelper.currentApplication().applicationContext

                    val isBackgroundBlurOpened =
                        XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context) as Boolean
                    if (!isBackgroundBlurOpened) return@createBeforeHook

                    val mPaint1 = it.thisObject.getObjectFieldOrNullAs<Paint>("mPaint1")
                    val mPaint2 = it.thisObject.getObjectFieldOrNullAs<Paint>("mPaint2")
                    if (mPaint1?.alpha == 0) return@createBeforeHook

                    mPaint1?.alpha = 0
                    mPaint2?.alpha = 0
                    it.thisObject.objectHelper().setObject("mRadius", 0f)

                    (it.thisObject as ImageView).setMiViewBlurMode(1)
                    (it.thisObject as ImageView).setBlurRoundRect(
                        getNotificationElementRoundRect(context)
                    )

                    val mStatusBarStateController = XposedHelpers.getObjectField(MiuiStub.sysUIProvider, "mStatusBarStateController")
                    val getLazyClass = XposedHelpers.callMethod(mStatusBarStateController, "get")
                    val getState = XposedHelpers.callMethod(getLazyClass, "getState")

                    (it.thisObject as ImageView).apply {
                        getNotificationElementBlendColors(context, getState == 1,
                            isDarkMode()
                        )?.let { iArr ->
                            setMiBackgroundBlendColors(iArr, 1f)
                        }
                    }

                    statusBarStateControllerImpl?.methodFinder()?.filterByName("getState")
                        ?.first()?.createAfterHook { hookParam1 ->
                            val getStatusBarState = hookParam1.result as Int
                            val isInLockScreen = getStatusBarState == 1
                            val isDarkMode = isDarkMode()
                            if (lockScreenStatus == null || darkModeStatus == null || lockScreenStatus != isInLockScreen || darkModeStatus != isDarkMode) {                                lockScreenStatus = isInLockScreen
                                darkModeStatus = isDarkMode
                                (it.thisObject as ImageView).apply {
                                    getNotificationElementBlendColors(context, getState == 1,
                                        isDarkMode()
                                    )?.let { iArr ->
                                        setMiBackgroundBlendColors(iArr, 1f)
                                    }
                                }

                                if (mediaControlPanelInstance != null) {
                                    val isBackgroundBlurOpened = XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context) as Boolean
                                    if (isBackgroundBlurOpened) {
                                        val mMediaViewHolder = mediaControlPanelInstance.objectHelper().getObjectOrNullUntilSuperclass("mMediaViewHolder")
                                        if (mMediaViewHolder != null) {
                                            val action0 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action0")
                                            val action1 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action1")
                                            val action2 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action2")
                                            val action3 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action3")
                                            val action4 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action4")
                                            val titleText = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("titleText")
                                            val artistText = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("artistText")
                                            val seamlessIcon = mMediaViewHolder.getObjectFieldOrNullAs<ImageView>("seamlessIcon")
                                            val seekBar = mMediaViewHolder.getObjectFieldOrNullAs<SeekBar>("seekBar")
                                            val elapsedTimeView = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("elapsedTimeView")
                                            val totalTimeView = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("totalTimeView")
                                            val grey = if (isDarkMode()) Color.LTGRAY else Color.DKGRAY
                                            val color = if (isDarkMode()) Color.WHITE else Color.BLACK
                                            artistText?.setTextColor(grey)
                                            elapsedTimeView?.setTextColor(grey)
                                            totalTimeView?.setTextColor(grey)
                                            action0?.setColorFilter(color)
                                            action1?.setColorFilter(color)
                                            action2?.setColorFilter(color)
                                            action3?.setColorFilter(color)
                                            action4?.setColorFilter(color)
                                            titleText?.setTextColor(color)
                                            seamlessIcon?.setColorFilter(color)
                                            seekBar?.progressDrawable?.colorFilter =
                                                colorFilter(
                                                    if (isMode) Color.TRANSPARENT else grey
                                                )
                                        }
                                    }
                                }
                            }
                        }
                }

            playerTwoCircleView?.methodFinder()?.filterByName("setBackground")?.first()
                ?.createBeforeHook {
                    val context = AndroidAppHelper.currentApplication().applicationContext
                    val isBackgroundBlurOpened =
                        XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context) as Boolean
                    if (!isBackgroundBlurOpened) return@createBeforeHook
                    (it.thisObject as ImageView).background = null
                    it.result = null
                }
        }.onFailure { t ->
            logE(TAG, lpparam.packageName, t)
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun getNotificationElementBlendColors(context: Context, isInLockScreen: Boolean, darkMode: Boolean): IntArray? {
        var resources = context.resources
        val theme = context.theme
        if (darkMode) {
            val configuration = Configuration(resources.configuration)
            configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            val context = ContextThemeWrapper(context, theme).createConfigurationContext(configuration)
            resources = context.resources
        }
        var arrayInt: IntArray? = null
        try {
            if (isInLockScreen) {
                val arrayId = resources.getIdentifier("notification_element_blend_keyguard_colors", "array", "com.android.systemui")
                arrayInt = resources.getIntArray(arrayId)
            } else {
                val arrayId = resources.getIdentifier("notification_element_blend_shade_colors", "array", "com.android.systemui")
                arrayInt = resources.getIntArray(arrayId)
            }
            return arrayInt
        } catch (_: Exception) {
            logD(TAG, "Notification element blend colors not found [1/3]!")
        }

        try {
            if (isInLockScreen) {
                val color1 = getResourceValue(resources, "notification_element_blend_keyguard_color_1", "color", theme)
                val color2 = getResourceValue(resources, "notification_element_blend_keyguard_color_2", "color", theme)
                val integer1 = getResourceValue(resources, "notification_element_blend_keyguard_mode_1", "integer")
                val integer2 = getResourceValue(resources, "notification_element_blend_keyguard_mode_2", "integer")
                arrayInt = intArrayOf(color1, integer1, color2, integer2)
            } else {
                val color1 = getResourceValue(resources, "notification_element_blend_shade_color_1", "color", theme)
                val color2 = getResourceValue(resources, "notification_element_blend_shade_color_2", "color", theme)
                val integer1 = getResourceValue(resources, "notification_element_blend_shade_mode_1", "integer")
                val integer2 = getResourceValue(resources, "notification_element_blend_shade_mode_2", "integer")
                arrayInt = intArrayOf(color1, integer1, color2, integer2)
            }
            return arrayInt
        } catch (_: Exception) {
            logD(TAG, "Notification element blend colors not found [2/3]!")
        }

        try {
            if (isInLockScreen) {
                val arrayId = resources.getIdentifier("notification_element_keyguard_colors", "array", "com.android.systemui")
                arrayInt = resources.getIntArray(arrayId)
            } else {
                val arrayId = resources.getIdentifier("notification_element_blend_colors", "array", "com.android.systemui")
                arrayInt = resources.getIntArray(arrayId)
            }
            return arrayInt
        } catch (_: Exception) {
            logD(TAG, "Notification element colors not found [3/3]!")
        }

        logE(TAG, "Notification element blend colors not found!")
        return arrayInt
    }

    @SuppressLint("DiscouragedApi")
    private fun getResourceValue(resources: Resources, name: String, type: String, theme: Resources.Theme? = null): Int {
        val id = resources.getIdentifier(name, type, "com.android.systemui")
        return when (type) {
            "color" -> resources.getColor(id, theme)
            "integer" -> resources.getInteger(id)
            else -> throw IllegalArgumentException("Unsupported resource type: $type")
        }
    }

    @SuppressLint("DiscouragedApi")
    fun getNotificationElementRoundRect(context: Context): Int {
        val resources = context.resources
        val dimenId =
            resources.getIdentifier("notification_item_bg_radius", "dimen", "com.android.systemui")
        return resources.getDimensionPixelSize(dimenId)
    }
}
