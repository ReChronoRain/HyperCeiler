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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media

import android.widget.SeekBar
import androidx.core.view.updatePadding
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.seekBarObserver
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.SquigglyProgress
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.hook.utils.getBooleanField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

// https://github.com/HowieHChen/XiaomiHelper/blob/master/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/media/CustomElement.kt
// 先这样，稳妥发版，后面再改
// 2025-07-18
object UseSquigglyProgress : BaseHook() {

    override fun init() {
        seekBarObserver!!.apply {
            constructorFinder()
                .first().createAfterHook {
                    val mediaViewHolder = it.thisObject.getObjectFieldOrNull("holder") ?:
                    it.thisObject.getObjectFieldOrNull("this$0")!!
                        .getObjectFieldOrNull("holder") ?: return@createAfterHook
                    val seekBar = mediaViewHolder.getObjectFieldOrNullAs<SeekBar>("seekBar") ?: return@createAfterHook
                    val context = seekBar.context

                    seekBar.progressDrawable = SquigglyProgress().apply {
                        waveLength = dp2px(context, 20f).toFloat()
                        lineAmplitude = dp2px(context, 1.5f).toFloat()
                        phaseSpeed = dp2px(context, 8f).toFloat()
                        strokeWidth = dp2px(context, 2f).toFloat()
                    }

                    val modRes = OtherTool.getModuleRes(context)
                    seekBar.thumb = modRes.getDrawable(R.drawable.media_seekbar_thumb, context.theme)
                    // 修复替换按钮后在进度条首尾会有截断问题
                    seekBar.updatePadding(left = seekBar.thumbOffset, right = seekBar.thumbOffset)

                    // 老写法，仅留档，HyperOS 1.0 Android 14 的系统界面有自带的原生进度条
                    /*if (isMoreHyperOSVersion(2f)) {
                        seekBar.progressDrawable = SquigglyProgress().apply {
                            waveLength = dp2px(context, 20f).toFloat()
                            lineAmplitude = dp2px(context, 1.5f).toFloat()
                            phaseSpeed = dp2px(context, 8f).toFloat()
                            strokeWidth = dp2px(context, 2f).toFloat()
                        }
                    } else {
                        val squigglyProgress = XposedHelpers.newInstance(
                            findClassIfExists("com.android.systemui.media.controls.ui.SquigglyProgress")
                        )
                        seekBar.progressDrawable = squigglyProgress as Drawable?
                    }*/
                }

            methodFinder().filterByName("onChanged")
                .first().createAfterHook {
                    val mediaViewHolder = it.thisObject.getObjectFieldOrNull("holder") ?:
                    it.thisObject.getObjectFieldOrNull("this$0")!!
                        .getObjectFieldOrNull("holder") ?: return@createAfterHook
                    val seekBar = mediaViewHolder.getObjectFieldOrNullAs<SeekBar>("seekBar") ?: return@createAfterHook
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
        }
    }
}
