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
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed.appContext
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHooks

object GestureLine : HomeBaseHookNew() {

    @Version(isPad = true)
    private fun initPadBase() {
        val navBarEventHelperClass =
            findClass("com.miui.home.recents.cts.NavBarEventHelper")
        Constructors.find(navBarEventHelperClass).toList().createAfterHooks {
                if (PrefsBridge.getInt("home_gesture_line_long_press_action", 0) <= 0) {
                    return@createAfterHooks
                }
                val gestureDetector = getObjectField(it.thisObject, "mGestureDetector") ?: return@createAfterHooks
                callMethod(gestureDetector, "setLongPressTimeOut", 650)
                callMethod(gestureDetector, "setLongPressTouchSlop", 12)
        }
        navBarEventHelperClass.findAllMethods { name("onLongPress") }.createBeforeHooks {
                if (GlobalActionBridge.handleAction(
                        appContext,
                        "home_gesture_line_long_press"
                    )
                ) {
                    it.result = null
                }
        }
        navBarEventHelperClass.findAllMethods { name("onDoubleTap") }.createBeforeHooks {
                if (GlobalActionBridge.handleAction(
                        appContext,
                        "home_gesture_line_double_click"
                    )
                ) {
                    it.result = true
                }
        }
    }

    override fun initBase() {
        val navStubGestureEventManagerClass =
            findClass("com.miui.home.recents.gesture.NavStubGestureEventManager")
        navStubGestureEventManagerClass.findAllMethods { name("handleLongPressEvent") }.createBeforeHooks {
                if (GlobalActionBridge.handleAction(
                        appContext,
                        "home_gesture_line_long_press"
                    )
                ) {
                    it.result = null
                }
        }
        navStubGestureEventManagerClass.findAllMethods { name("handleDoubleClickEvent") }.createBeforeHooks {
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
