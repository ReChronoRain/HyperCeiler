/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import android.view.*
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.*

// from https://github.com/Art-Chen/MIUI-Extra-YukiAPI/blob/f5a3ba5d17e0e62114fc355c3ff1a8daa9da94ff/app/src/main/java/moe/chenxy/miuiextra/hooker/entity/systemui/LinkageAnimCustomer.kt
class LinkageAnimCustomer : BaseHook() {

    var listener: Any? = null
    var animConfig: Any? = null
    var showEase: Any? = null
    fun initAnim() {
        if (listener != null) return

        listener =
            findClass("com.android.keyguard.clock.animation.ClockBaseAnimation\$1").getDeclaredConstructor(
                SurfaceControl.Transaction::class.java, Int::class.java
            ).newInstance(SurfaceControl.Transaction(), 1 /* toLock */)
        showEase =
            findClass("miuix.animation.utils.EaseManager\$InterpolateEaseStyle").declaredConstructors[0].newInstance(
                20,
                floatArrayOf(1.0f)
            )

        animConfig =
            findClass("miuix.animation.base.AnimConfig").declaredConstructors[0].newInstance()
        val listeners = XposedHelpers.getObjectField(animConfig, "listeners") as HashSet<Any>
        listeners.add(listener!!)
    }

    override fun init() {
        findAndHookMethod(
            "com.android.keyguard.clock.animation.ClockBaseAnimation",
            "doAnimationToAod",
            Boolean::class.java,
            Boolean::class.java,
            Boolean::class.java,
            object : com.sevtinge.hyperceiler.module.base.tool.HookTool.MethodHook() {
                override fun before(param: MethodHookParam?) {
                    val fromKeyguard = param?.args?.get(2) as Boolean
                    if (!fromKeyguard) return

                    val toAod = param.args[0] as Boolean
                    val hasNotification = param.args[1] as Boolean

                    val on = mPrefsMap.getInt("system_ui_lock_screen_linkage_anim_on", 300)
                    val off = mPrefsMap.getInt("system_ui_lock_screen_linkage_anim_off", 300)

                    if (toAod) {
                        val mWallpaperHideEase =
                            XposedHelpers.getObjectField(param.thisObject, "mWallpaperHideEase")
                        XposedHelpers.callMethod(mWallpaperHideEase, "setDuration", off.toLong())
                    } else {
                        initAnim()

                        XposedHelpers.callMethod(showEase, "setDuration", on.toLong())
                        val stateStyle = XposedHelpers.callStaticMethod(
                            findClass("miuix.animation.Folme"),
                            "useValue",
                            arrayOf("WallpaperParam")
                        )
                        XposedHelpers.callMethod(animConfig, "setEase", showEase)
                        XposedHelpers.callMethod(
                            stateStyle,
                            "to",
                            arrayOf("wallpaperBlack", 0f, animConfig)
                        )

                        XposedHelpers.callMethod(param.thisObject, "doAnimationToAod", false, hasNotification)
                        param.result = null
                    }
                }

            })
    }
}
