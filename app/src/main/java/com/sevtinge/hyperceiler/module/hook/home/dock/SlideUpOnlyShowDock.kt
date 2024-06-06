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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.home.dock

import android.view.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*


object SlideUpOnlyShowDock : BaseHook() {
    override fun init() {
        loadClass("com.miui.home.recents.DockGestureHelper").methodFinder()
            .filterByName("dispatchTouchEvent").single().createHook {
                replace {
                    // ================
                    // DockController dockController = getDockController();
                    // if (dockController == null) {
                    //     Log.e("DockGestureHelper", "onTouchEvent: dockController=" + getDockController());
                    //     return;
                    // }
                    // ================
                    val dockController = it.thisObject.callMethod("getDockController")
                        ?: return@replace null

                    // ================
                    // int actionMasked = motionEvent.getActionMasked();
                    // if (actionMasked == 3 || actionMasked == 1) {
                    //     this.mTransitionYStyle.cancel();
                    //     this.mIsDockTransitionAnimStart = false;
                    // }
                    // ================
                    val motionEvent = it.args[0] as MotionEvent
                    val actionMasked = motionEvent.actionMasked
                    if (actionMasked == 3 || actionMasked == 1) {
                        it.thisObject.getObjectField("mTransitionYStyle")?.callMethod("cancel")
                        it.thisObject.setBooleanField("mIsDockTransitionAnimStart", false)
                    }


                    // ================
                    // if (!dockController.isFloatingDockShowing()) {
                    //     if (motionEvent.getEventTime() - motionEvent.getDownTime() >= 180) {
                    //         if (actionMasked == 3 || actionMasked == 1 || actionMasked == 6 || actionMasked == 5) {
                    //             dockController.dispatchUpEvent(motionEvent, this.mTouchTracker.getUpType());
                    //         } else {
                    //             animationTransitionDock(motionEvent, dockController);
                    //             dockController.addMovement(motionEvent);
                    //             dockController.updateLeaveSafeAreaStatus(motionEvent.getRawX(), motionEvent.getRawY(), true);
                    //         }
                    //     } else if (actionMasked != 3 && actionMasked != 1) {
                    //         dockController.addMovement(motionEvent);
                    //         dockController.updateLeaveSafeAreaStatus(motionEvent.getRawX(), motionEvent.getRawY(), false);
                    //     }
                    // ================
                    val b = dockController.callMethod("isFloatingDockShowing") as Boolean
                    if (!b) {
                        // 将判断时间设置为0
                        // if (motionEvent.eventTime - motionEvent.downTime >= 180) {
                        if (actionMasked == 3 || actionMasked == 1 || actionMasked == 6 || actionMasked == 5) {
                            // dockController.callMethod("dispatchUpEvent", motionEvent, it.thisObject.getObjectField("mTouchTracker")?.callMethod("getUpType"))
                            // 设置为10, 表示是慢速上滑,后面就会执行打开dock的逻辑,如果是5则为快速上滑,会执行跳转桌面的逻辑
                            dockController.callMethod("dispatchUpEvent", motionEvent, 10)
                        } else {
                            it.thisObject.callMethod(
                                "animationTransitionDock", motionEvent, dockController
                            )
                            dockController.callMethod("addMovement", motionEvent)
                            dockController.callMethod(
                                "updateLeaveSafeAreaStatus",
                                motionEvent.rawX,
                                motionEvent.rawY,
                                true
                            )
                        }
                        // } else if (actionMasked != 3 && actionMasked != 1) {
                        //     dockController.callMethod("addMovement", motionEvent)
                        //     dockController.callMethod(
                        //         "updateLeaveSafeAreaStatus",
                        //         motionEvent.rawX,
                        //         motionEvent.rawY,
                        //         false
                        //     )
                        // }
                    }

                    // ================
                    // if (!this.isStartedGesture) {
                    //     if (isTargetValue(actionMasked, 2) && (dockController.isLeaveSafeArea() || this.mTouchTracker.isTaskStartMove(motionEvent.getRawY()))) {
                    //         startGestureModeGesture(0);
                    //     } else if (isTargetValue(actionMasked, 1, 3) && this.mTouchTracker.getUpType() == 5) {
                    //         startGestureModeGesture(1);
                    //     }
                    // }
                    // ================
                    // val isStartedGesture = it.thisObject.getBooleanField("isStartedGesture")
                    // if (!isStartedGesture) {
                    //     if (it.thisObject.callMethod(
                    //             "isTargetValue",
                    //             actionMasked,
                    //             2
                    //         ) as Boolean && (dockController.callMethod("isLeaveSafeArea") as Boolean || it.thisObject.callMethod(
                    //             "mTouchTracker"
                    //         )?.callMethod("isTaskStartMove", motionEvent.rawY) as Boolean)
                    //     ) {
                    //         it.thisObject.callMethod("startGestureModeGesture", 0)
                    //     } else if (it.thisObject.callMethod(
                    //             "isTargetValue",
                    //             actionMasked,
                    //             1,
                    //             3
                    //         ) as Boolean && it.thisObject.callMethod("mTouchTracker")
                    //             ?.callMethod("getUpType") == 5
                    //     ) {
                    //         it.thisObject.callMethod("startGestureModeGesture", 1)
                    //     }
                    // }
                    // 这部分是跳转最近任务或者桌面的逻辑,直接去掉


                    // ================
                    // if (this.isStartedGesture) {
                    //     this.mGestureInputHelper.dispatchGestureModeTouchEvent(motionEvent);
                    // } else if ((actionMasked == 1 || actionMasked == 6) && (launcher = Application.getLauncher()) != null) {
                    //     launcher.notifyPowerKeeperGesture("gesture_end", !this.mTouchTracker.isKeyboardEventTracker());
                    // }
                    // ================


                    val isStartedGesture0 = it.thisObject.getBooleanField("isStartedGesture")
                    if (isStartedGesture0) {
                        it.thisObject.callMethod("mGestureInputHelper")
                            ?.callMethod("dispatchGestureModeTouchEvent", motionEvent)
                    } else if (actionMasked == 1 || actionMasked == 6) {
                        val launcher = findClassIfExists(
                            "com.miui.home.launcher.Application", lpparam.classLoader
                        ).callStaticMethod("getLauncher")
                        launcher?.javaClass?.getDeclaredMethod(
                            "notifyPowerKeeperGesture",
                            String::class.java,
                            Boolean::class.javaPrimitiveType
                        )?.invoke(
                            launcher,
                            "gesture_end",
                            !(it.thisObject.getObjectField("mTouchTracker")!!
                                .callMethod("isKeyboardEventTracker") as Boolean)
                        )
                    } else {
                    }
                }
            }

        // 拦截通过dock快速上滑进入桌面的方法
        loadClass("com.miui.home.recents.DockGestureHelper").methodFinder()
            .filterByName("startGestureModeGesture").single().createHook {
                replace { }
            }
    }


}
