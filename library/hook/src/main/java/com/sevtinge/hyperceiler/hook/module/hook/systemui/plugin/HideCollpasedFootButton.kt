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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin

import android.view.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreHyperOSVersion
import de.robv.android.xposed.*

object HideCollpasedFootButton {
    fun initLoaderHook(classLoader: ClassLoader) {
        val mRingerButtonHelper by lazy {
            loadClass("com.android.systemui.miui.volume.MiuiRingerModeLayout\$RingerButtonHelper", classLoader)
        }
        val mTimerItem by lazy {
            loadClass("com.android.systemui.miui.volume.TimerItem", classLoader)
        }

        XposedHelpers.findAndHookMethod(
            mRingerButtonHelper, "updateState",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val thisObj = param?.thisObject
                    val mIcon = XposedHelpers.getObjectField(thisObj, "mIcon") as View
                    val mStandardView =
                        XposedHelpers.getObjectField(thisObj, "mStandardView") as View
                    val mExpanded = XposedHelpers.getBooleanField(thisObj, "mExpanded")
                    if (mExpanded) {
                        mIcon.visibility = View.VISIBLE
                        mStandardView.visibility = View.VISIBLE
                    } else {
                        mIcon.visibility = View.GONE
                        mStandardView.visibility = View.GONE

                    }

                }
            })


        if (isMoreHyperOSVersion(2f)) {
            XposedHelpers.findAndHookMethod(
                mRingerButtonHelper, "onExpanded", Boolean::class.java, Boolean::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam?) {
                        val thisObj = param?.thisObject
                        val z1 = param?.args?.get(0) as Boolean
                        val z2 = param.args?.get(1) as Boolean

                        val mStandardView =
                            XposedHelpers.getObjectField(thisObj, "mStandardView") as View
                        val mExpanded = XposedHelpers.getBooleanField(thisObj, "mExpanded")

                        if (mExpanded != z1 || z2) {
                            mStandardView.visibility = View.GONE
                        } else {
                            mStandardView.visibility = View.VISIBLE
                        }

                    }
                })
        } else {
            XposedHelpers.findAndHookMethod(
                mRingerButtonHelper, "onExpanded", Boolean::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam?) {
                        val thisObj = param?.thisObject
                        val z1 = param?.args?.get(0) as Boolean
                        val mStandardView =
                            XposedHelpers.getObjectField(thisObj, "mStandardView") as View
                        val mExpanded = XposedHelpers.getBooleanField(thisObj, "mExpanded")

                        if (mExpanded != z1) {
                            mStandardView.visibility = View.GONE
                        } else {
                            mStandardView.visibility = View.VISIBLE
                        }
                    }
                })
        }

        XposedHelpers.findAndHookMethod(
            mTimerItem, "updateExpanded", Boolean::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val thisObj = param?.thisObject
                    val mCountDownProgressBar =
                        XposedHelpers.getObjectField(thisObj, "mCountDownProgressBar") as View

                    mCountDownProgressBar.visibility = View.GONE
                }
            })
    }
}
