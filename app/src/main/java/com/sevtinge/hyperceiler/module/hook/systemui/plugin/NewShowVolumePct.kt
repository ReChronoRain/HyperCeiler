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
package com.sevtinge.hyperceiler.module.hook.systemui.plugin

import android.animation.*
import android.annotation.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.tool.*
import com.sevtinge.hyperceiler.module.base.tool.OtherTool.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.*

object NewShowVolumePct {
    @JvmStatic
    fun initLoader(classLoader: ClassLoader) {
        if (isMoreHyperOSVersion(2f)) {
            val volumePanelViewControllerClazz by lazy {
                loadClass("com.android.systemui.miui.volume.VolumePanelViewController", classLoader)
            }
            val volumePanelViewControllerListener by lazy {
                loadClass("com.android.systemui.miui.volume.VolumePanelViewController\$VolumeSeekBarChangeListener", classLoader)
            }

            volumePanelViewControllerClazz.methodFinder().filterByName("showVolumePanelH")
                .first().createAfterHook {
                    val mVolumeView =
                        it.thisObject.getObjectField("mVolumeView") as View
                    val windowView = mVolumeView.parent as FrameLayout
                    initPct(windowView, 3)
                }

            mVolumeDisable(volumePanelViewControllerClazz)
            mSupportSV(volumePanelViewControllerListener, classLoader)
        } else {
            val miuiVolumeDialogImplClazz by lazy {
                loadClass("com.android.systemui.miui.volume.MiuiVolumeDialogImpl", classLoader)
            }
            val miuiVolumeDialogImplListener by lazy {
                loadClass("com.android.systemui.miui.volume.MiuiVolumeDialogImpl\$VolumeSeekBarChangeListener", classLoader)
            }

            miuiVolumeDialogImplClazz.methodFinder().filterByName("showVolumeDialogH")
                .first().createAfterHook {
                    val mVolumeView =
                        it.thisObject.getObjectField("mDialogView") as View
                    val windowView = mVolumeView.parent as FrameLayout
                    initPct(windowView, 3)
                }

            mVolumeDisable(miuiVolumeDialogImplClazz)
            mSupportSV(miuiVolumeDialogImplListener, classLoader)
        }
    }

    private fun mVolumeDisable(clazz: Class<*>) {
        clazz.methodFinder().filterByName("dismissH")
            .first().createAfterHook {
                removePct(mPct)
            }
    }

    private fun mSupportSV(clazz: Class<*>, classLoader: ClassLoader) {
        runCatching {
            loadClass("miui.systemui.util.VolumeUtils", classLoader).methodFinder()
                .filterByName("getSUPER_VOLUME_SUPPORTED")
                .first().createAfterHook {
                    val result = it.result as Boolean
                    onProgressChanged(clazz, result)
                }
        }.recoverCatching {
            loadClass("miui.systemui.util.CommonUtils", classLoader).methodFinder()
                .filterByName("voiceSupportSuperVolume")
                .first().createAfterHook {
                    val result = it.result as Boolean
                    onProgressChanged(clazz, result)
                }
        }.onFailure {
            onProgressChanged(clazz, false)
        }
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

                if (mPct != null && mPct.tag != null) {
                    mTag = mPct.tag as Int
                }

                if (nowLevel == arg1 || mTag != 3 || mPct == null) return@createAfterHook

                val mColumn = it.thisObject.getObjectFieldOrNull("mColumn") ?: return@createAfterHook
                val ss = mColumn.getObjectFieldOrNull("ss") ?: return@createAfterHook

                if (mColumn.getIntField("stream") == 10) return@createAfterHook

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
                    currentLevel = if (currentLevel == max) maxLevel else (currentLevel * i3 / max) + 1
                }

                mPct.text = if (((currentLevel * 100) / maxLevel) == 100 && (HookTool.mPrefsMap.getBoolean("system_ui_unlock_super_volume") || mSupportSV)) {
                    "200%"
                } else {
                    ((currentLevel * 100) / maxLevel).toString() + "%"
                }
            }
    }
}