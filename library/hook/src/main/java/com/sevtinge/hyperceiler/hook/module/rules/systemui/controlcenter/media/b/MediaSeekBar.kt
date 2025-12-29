package com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.media.b

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.widget.SeekBar
import androidx.core.graphics.toColorInt
import androidx.core.view.updatePadding
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool
import com.sevtinge.hyperceiler.hook.module.rules.systemui.base.controlcenter.PublicClass.mediaViewHolderNew
import com.sevtinge.hyperceiler.hook.module.rules.systemui.base.controlcenter.PublicClass.seekBarObserverNew
import com.sevtinge.hyperceiler.hook.module.rules.systemui.base.controlcenter.drawable.SquigglyProgress
import com.sevtinge.hyperceiler.hook.utils.api.dp
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.hook.utils.devicesdk.colorFilter
import com.sevtinge.hyperceiler.hook.utils.getBooleanField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

// Android 16
// HyperOS 2.0.230.12.WOCCNXM
// Xiaomi 新增且仅调用 Lcom/android/systemui/statusbar/notification/mediacontrol/MiuiMediaViewHolder;
// 和 Android 15- 调用链不一致，故新开
// 2025.08.03
object MediaSeekBar : BaseHook() {
    private val progressThickness by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_progress_thickness", 80)
    }
    private val cornerRadiusBar by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_progress_corner_radius", 36)
    }
    private val progressColor by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_color", -1)
    }
    private val thumbColor by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_thumb_color", -1)
    }
    private val mode by lazy {
        mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0)
    }
    private val modeThumb by lazy {
        mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_thumb_mode", 0)
    }

    override fun init() {
        // 1 -> SquigglyProgress, 2 -> GradientDrawable
        mediaViewHolderNew!!.constructorFinder()
            .first().createAfterHook {
                val seekBar = it.thisObject.getObjectFieldOrNullAs<SeekBar>("seekBar") ?: return@createAfterHook
                val context = seekBar.context

                // https://github.com/HowieHChen/XiaomiHelper/blob/4241dda/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/media/CustomElement.kt
                when (mode) {
                    1 -> {
                        seekBar.progressDrawable = SquigglyProgress().apply {
                            waveLength = dp2px(context, 20f).toFloat()
                            lineAmplitude = dp2px(context, 1.5f).toFloat()
                            phaseSpeed = dp2px(context, 8f).toFloat()
                            strokeWidth = dp2px(context, 2f).toFloat()
                        }
                    }

                    2 -> {
                        val backgroundDrawable = GradientDrawable().apply {
                            color = ColorStateList(
                                arrayOf(intArrayOf()),
                                intArrayOf("#20ffffff".toColorInt())
                            )
                            cornerRadius = cornerRadiusBar.dp.toFloat()
                        }

                        val onProgressDrawable = GradientDrawable().apply {
                            color = ColorStateList(arrayOf(intArrayOf()), intArrayOf("#ffffffff".toColorInt()))
                            cornerRadius = cornerRadiusBar.dp.toFloat()
                        }

                        val layerDrawable = LayerDrawable(
                            arrayOf(
                                backgroundDrawable,
                                ClipDrawable(onProgressDrawable, Gravity.START, ClipDrawable.HORIZONTAL)
                            )
                        ).apply {
                            if (mode == 2) {
                                setLayerHeight(0, progressThickness.dp)
                                setLayerHeight(1, progressThickness.dp)
                            } else {
                                setLayerHeight(0, 9.dp)
                                setLayerHeight(1, 9.dp)
                            }
                        }

                        seekBar.progressDrawable = layerDrawable
                    }
                }

                if (modeThumb == 1) {
                    val modRes = OtherTool.getModuleRes(context)
                    seekBar.thumb = modRes.getDrawable(R.drawable.media_seekbar_thumb, context.theme)
                    // 修复替换按钮后在进度条首尾会有截断问题
                    seekBar.updatePadding(left = seekBar.thumbOffset, right = seekBar.thumbOffset)
                }

            }

        seekBarObserverNew!!.methodFinder().filterByName("onChanged")
            .first().createAfterHook {
                val mediaHolder = it.thisObject.getObjectFieldOrNull("this$0")!!
                    .getObjectFieldOrNull("holder") ?: return@createAfterHook
                val seekBar =
                    mediaHolder.getObjectFieldOrNullAs<SeekBar>("seekBar") ?: return@createAfterHook

                if (progressColor != -1)
                    seekBar.progressDrawable.colorFilter = PorterDuffColorFilter(progressColor, PorterDuff.Mode.SRC_IN)
                if (thumbColor != -1 && modeThumb != 2)
                    seekBar.thumb.colorFilter = PorterDuffColorFilter(thumbColor, PorterDuff.Mode.SRC_IN)

                if (mode == 1) {
                    val drawable = seekBar.progressDrawable
                    if (drawable !is SquigglyProgress) return@createAfterHook
                    val progress = it.args[0] ?: return@createAfterHook
                    val seekAvailable = progress.getBooleanField("seekAvailable")
                    val playing = progress.getBooleanField("playing")
                    val scrubbing = progress.getBooleanField("scrubbing")
                    val enabled = progress.getBooleanField("enabled")

                    if (!enabled) {
                        drawable.animate = false
                    } else {
                        drawable.animate = playing && !scrubbing
                        drawable.transitionEnabled = !seekAvailable
                    }
                }

                if (modeThumb == 2) {
                    seekBar.thumb.colorFilter = colorFilter(Color.TRANSPARENT)
                }
            }
    }
}
