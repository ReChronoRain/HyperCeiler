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
package com.sevtinge.hyperceiler.libhook.rules.home.gesture

import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import com.sevtinge.hyperceiler.libhook.appbase.systemframework.GlobalActionBridge
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllConstructors
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllMethods
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.appContext

object GestureLine : HomeBaseHookNew() {

    @Version(isPad = true)
    private fun initPadBase() {
        val navBarEventHelperClass =
            findClass("com.miui.home.recents.cts.NavBarEventHelper")
        navBarEventHelperClass.hookAllConstructors {
            after {
                if (PrefsBridge.getInt("home_gesture_line_long_press_action", 0) <= 0) {
                    return@after
                }
                val gestureDetector = getObjectField(it.thisObject, "mGestureDetector") ?: return@after
                callMethod(gestureDetector, "setLongPressTimeOut", 650)
                callMethod(gestureDetector, "setLongPressTouchSlop", 12)
            }
        }
        navBarEventHelperClass.hookAllMethods("onLongPress") {
            before {
                if (GlobalActionBridge.handleAction(
                        appContext,
                        "home_gesture_line_long_press"
                    )
                ) {
                    it.result = null
                }
            }
        }
        navBarEventHelperClass.hookAllMethods("onDoubleTap") {
            before {
                if (GlobalActionBridge.handleAction(
                        appContext,
                        "home_gesture_line_double_click"
                    )
                ) {
                    it.result = true
                }
            }
        }
    }

    override fun initBase() {
        val navStubGestureEventManagerClass =
            findClass("com.miui.home.recents.gesture.NavStubGestureEventManager")
        navStubGestureEventManagerClass.hookAllMethods("handleLongPressEvent") {
            before {
                if (GlobalActionBridge.handleAction(
                        appContext,
                        "home_gesture_line_long_press"
                    )
                ) {
                    it.result = null
                }
            }
        }
        navStubGestureEventManagerClass.hookAllMethods("handleDoubleClickEvent") {
            before {
                if (GlobalActionBridge.handleAction(
                        appContext,
                        "home_gesture_line_double_click"
                    )
                ) {
                    it.result = null
                }
            }
        }
    }
}
