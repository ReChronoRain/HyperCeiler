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
package com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.initPct
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.mPct
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.removePct
import com.sevtinge.hyperceiler.hook.module.rules.systemui.base.api.mSupportSV
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.hook.utils.getIntField
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import java.util.WeakHashMap

object NewShowVolumePct {

    private val streamCache = WeakHashMap<Any, Int>()
    @JvmStatic
    fun initLoader(classLoader: ClassLoader) {
        val volumePanelViewControllerClazz by lazy {
            loadClass("com.android.systemui.miui.volume.VolumePanelViewController", classLoader)
        }
        val volumePanelViewControllerListener by lazy {
            if (isMoreHyperOSVersion(3f)) {
                loadClass($$"miui.systemui.controlcenter.panel.main.volume.VolumeSliderController$seekBarListener$1", classLoader)
            } else {
                loadClass($$"com.android.systemui.miui.volume.VolumePanelViewController$VolumeSeekBarChangeListener", classLoader)
            }
        }

        volumePanelViewControllerClazz.methodFinder().filterByName("showVolumePanelH")
            .first().createAfterHook {
                val mVolumeView =
                    it.thisObject.getObjectField("mVolumeView") as View
                val windowView = mVolumeView.parent as FrameLayout
                initPct(windowView, 3)
            }

        mVolumeDisable(volumePanelViewControllerClazz)
        onProgressChanged(volumePanelViewControllerListener, mSupportSV)
    }

    private fun mVolumeDisable(clazz: Class<*>) {
        clazz.methodFinder().filterByName("dismissH")
            .first().createAfterHook {
                removePct(mPct)
            }
    }

    private fun getOrCacheStream(volumeColumn: Any): Int {
        streamCache[volumeColumn]?.let { return it }
        val stream = try {
            volumeColumn.getObjectFieldOrNullAs<Int>("stream")
                ?: (volumeColumn.callMethod("getStream") as? Int)
                ?: -1
        } catch (_: Throwable) {
            -1
        }
        streamCache[volumeColumn] = stream
        return stream
    }

    @SuppressLint("SetTextI18n")
    private fun onProgressChanged(clazz: Class<*>, mSupportSV: Boolean) {
        clazz.methodFinder().filterByName("onProgressChanged")
            .first().createAfterHook {
                var nowLevel = -233
                var mTag = 0
                var currentLevel: Int
                val seekBar = it.args[0] as SeekBar
                val arg1 = it.args[1] as Int
                val arg2 = it.args[2] as Boolean
                val isObj: Any = if (isMoreHyperOSVersion(3f)) {
                    it.thisObject.getObjectFieldOrNull("this$0")
                } else {
                    it.thisObject
                } ?: return@createAfterHook

                if (mPct != null && mPct.tag != null) {
                    mTag = mPct.tag as Int
                }

                if (nowLevel == arg1 || mTag != 3 || mPct == null) return@createAfterHook

                val mColumn =
                    isObj.getObjectFieldOrNull("mColumn") ?: return@createAfterHook
                val ss = mColumn.getObjectFieldOrNull("ss") ?: return@createAfterHook

                if (getOrCacheStream(mColumn) == 10) return@createAfterHook

                currentLevel = if (arg2) {
                    arg1
                } else {
                    val anim = mColumn.getObjectFieldOrNullAs<ObjectAnimator>("anim")
                    if (anim == null || !anim.isRunning) return@createAfterHook
                    mColumn.getIntField("animTargetProgress")
                }

                nowLevel = currentLevel
                mPct.visibility = View.VISIBLE

                val levelMin = ss.getIntField("levelMin")
                if (levelMin > 0 && currentLevel < levelMin * 1000) {
                    currentLevel = levelMin * 1000
                }

                val max = seekBar.max
                val maxLevel = max / 1000
                if (currentLevel != 0) {
                    val i3 = maxLevel - 1
                    currentLevel =
                        if (currentLevel == max) maxLevel else (currentLevel * i3 / max) + 1
                }

                mPct.text = if (((currentLevel * 100) / maxLevel) == 100 && mSupportSV) {
                    "200%"
                } else {
                    ((currentLevel * 100) / maxLevel).toString() + "%"
                }
            }
    }
}
