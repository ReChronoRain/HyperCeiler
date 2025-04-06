package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media

import android.content.res.*
import android.graphics.*
import android.graphics.drawable.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.mediaViewHolder
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaControlPanel
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.seekBarObserver
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.api.dp
import com.sevtinge.hyperceiler.hook.utils.devicesdk.colorFilter

object MediaSeekBar : BaseHook() {
    private val progress by lazy {
        mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0) == 2
    }
    private val progressThickness by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_progress_thickness", 80)
    }
    private val cornerRadiusBar by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_progress_corner_radius", 36)
    }
    private val removeBackground by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_remove_media_control_panel_background")
    }

    override fun init() {
        mediaViewHolder?.constructors?.first()?.createAfterHook {
            val seekBar = it.thisObject.objectHelper().getObjectOrNullAs<SeekBar>("seekBar")

            val backgroundDrawable = GradientDrawable().apply {
                color = ColorStateList(arrayOf(intArrayOf()), intArrayOf(Color.parseColor("#20ffffff")))
                cornerRadius = cornerRadiusBar.dp.toFloat()
            }

            val onProgressDrawable = GradientDrawable().apply {
                color = ColorStateList(arrayOf(intArrayOf()), intArrayOf(Color.parseColor("#ffffffff")))
                cornerRadius = cornerRadiusBar.dp.toFloat()
            }

            val layerDrawable = LayerDrawable(
                arrayOf(backgroundDrawable, ClipDrawable(onProgressDrawable, Gravity.START, ClipDrawable.HORIZONTAL))
            ).apply {
                if (progress) {
                    setLayerHeight(0, progressThickness.dp)
                    setLayerHeight(1, progressThickness.dp)
                } else {
                    setLayerHeight(0, 9.dp)
                    setLayerHeight(1, 9.dp)
                }
            }

            seekBar?.progressDrawable = layerDrawable
        }

        seekBarObserver?.constructors?.first()?.createAfterHook {
            if (progress) {
                it.thisObject.objectHelper()
                    .setObject("seekBarEnabledMaxHeight", progressThickness.dp)
            } else {
                it.thisObject.objectHelper()
                    .setObject("seekBarEnabledMaxHeight", 9.dp)
            }
        }

        if (!removeBackground) {
            miuiMediaControlPanel?.methodFinder()?.filterByName("bindPlayer")?.first()?.createAfterHook {
                val mMediaViewHolder = it.thisObject.objectHelper().getObjectOrNullUntilSuperclass("mMediaViewHolder") ?: return@createAfterHook

                val seekBar = mMediaViewHolder.objectHelper().getObjectOrNullAs<SeekBar>("seekBar")
                seekBar?.thumb?.colorFilter = colorFilter(Color.TRANSPARENT)
            }
        }
    }
}
