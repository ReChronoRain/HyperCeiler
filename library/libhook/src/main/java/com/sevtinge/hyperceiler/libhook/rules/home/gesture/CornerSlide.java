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
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookConstructor;

import android.content.Context;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;

import com.sevtinge.hyperceiler.libhook.appbase.systemframework.GlobalActionBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.EzXposed;
import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class CornerSlide extends BaseHook {
    public int inDirection = 0;

    Context mContext;
    Class<?> mGestureOperationHelperClass;

    @Override
    public void init() {
        findAndHookMethod("com.android.systemui.shared.recents.system.AssistManager",
            "isSupportGoogleAssist", int.class,
            returnConstant(true));
        Class<?> FsGestureAssistHelper = findClassIfExists("com.miui.home.recents.FsGestureAssistHelper");
        Class<?> gestureModeAssistantClass = findClassIfExists("com.miui.home.recents.GestureModeAssistant");
        mGestureOperationHelperClass = findClassIfExists("com.miui.home.recents.GestureOperationHelper");
        if (isPad()) {
            findAndHookMethod(FsGestureAssistHelper, "canTriggerAssistantAction",
                float.class, float.class, int.class,
                new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        boolean isDisabled = (boolean) EzxHelpUtils.callStaticMethod(FsGestureAssistHelper,
                            "isAssistantGestureDisabled", param.getArgs()[2]);
                        if (!isDisabled) {
                            int mAssistantWidth = (int) EzxHelpUtils.getObjectField(param.getThisObject(), "mAssistantWidth");
                            float f = (float) param.getArgs()[0];
                            float f2 = (float) param.getArgs()[1];
                            if (f < mAssistantWidth || f > f2 - mAssistantWidth) {
                                param.setResult(true);
                                return;
                            }
                        }
                        param.setResult(false);
                    }
                }
            );
        } else {
            findAndHookMethod(FsGestureAssistHelper, "canTriggerAssistantAction",
                float.class, float.class, long.class,
                new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        boolean isDisabled = (boolean) EzxHelpUtils.callStaticMethod(FsGestureAssistHelper,
                            "isAssistantGestureDisabled", param.getArgs()[2]);
                        if (!isDisabled) {
                            int mAssistantWidth = (int) EzxHelpUtils.getObjectField(param.getThisObject(), "mAssistantWidth");
                            float f = (float) param.getArgs()[0];
                            float f2 = (float) param.getArgs()[1];
                            if (f < mAssistantWidth || f > f2 - mAssistantWidth) {
                                param.setResult(true);
                                return;
                            }
                        }
                        param.setResult(false);
                    }
                }
            );
        }

        // final int[] inDirection = {0};
        hookAllMethods(FsGestureAssistHelper,
            "handleTouchEvent", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    MotionEvent motionEvent = (MotionEvent) param.getArgs()[0];
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        updateDirection(param.getThisObject(), motionEvent);
                    }
                }
            }
        );

        if (gestureModeAssistantClass != null) {
            hookAllMethods(gestureModeAssistantClass,
                "onStartGesture", new IMethodHook() {
                    @Override
                    public void after(HookParam param) {
                        updateDirectionFromDownPoint(
                            param.getThisObject(),
                            EzxHelpUtils.getFloatField(param.getThisObject(), "mDownX"),
                            EzxHelpUtils.getFloatField(param.getThisObject(), "mDownY")
                        );
                    }
                }
            );
        }

        findAndHookConstructor("com.miui.home.recents.NavStubView",
            Context.class, new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    mContext = (Context) param.getArgs()[0];
                }
            }
        );

        findAndHookMethod("com.miui.home.recents.SystemUiProxyWrapper",
            "startAssistant", Bundle.class,
            new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    Bundle bundle = (Bundle) param.getArgs()[0];
                    if (bundle.getInt("triggered_by", 0) != 83 || bundle.getInt("invocation_type", 0) != 1) {
                        return;
                    }
                    String direction = inDirection == 1 ? "right" : "left";
                    if (GlobalActionBridge.handleAction(
                        EzXposed.getAppContext(),
                        "home_navigation_assist_" + direction + "_slide"
                    )) {
                        param.setResult(null);
                    }
                }
            }
        );
    }

    private void updateDirection(Object helper, MotionEvent motionEvent) {
        updateDirectionFromDownPoint(helper, motionEvent.getRawX(), motionEvent.getRawY());
    }

    private void updateDirectionFromDownPoint(Object helper, float downX, float downY) {
        if (isPad() && updateDirectionWithGestureRegions(downX, downY)) {
            return;
        }
        int assistantWidth = EzxHelpUtils.getIntField(helper, "mAssistantWidth");
        inDirection = downX < assistantWidth ? 0 : 1;
    }

    private boolean updateDirectionWithGestureRegions(float downX, float downY) {
        if (mGestureOperationHelperClass == null) {
            return false;
        }
        try {
            RectF leftRegion = (RectF) EzxHelpUtils.getStaticObjectField(mGestureOperationHelperClass, "REGION_BOTTOM_LEFT_CORNER");
            RectF rightRegion = (RectF) EzxHelpUtils.getStaticObjectField(mGestureOperationHelperClass, "REGION_BOTTOM_RIGHT_CORNER");
            if (leftRegion != null && leftRegion.contains(downX, downY)) {
                inDirection = 0;
                return true;
            }
            if (rightRegion != null && rightRegion.contains(downX, downY)) {
                inDirection = 1;
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
