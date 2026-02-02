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

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookConstructor;

import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.moduleload.GlobalActions;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.EzXposed;
import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class CornerSlide extends BaseHook {
    public int inDirection = 0;

    Context mContext;

    @Override
    public void init() {
        findAndHookMethod("com.android.systemui.shared.recents.system.AssistManager",
            "isSupportGoogleAssist", int.class,
            returnConstant(true));
        Class<?> FsGestureAssistHelper = findClassIfExists("com.miui.home.recents.FsGestureAssistHelper");
        findAndHookMethod(FsGestureAssistHelper, "canTriggerAssistantAction",
            float.class, float.class, long.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
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

        // final int[] inDirection = {0};
        hookAllMethods(FsGestureAssistHelper,
            "handleTouchEvent", new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    MotionEvent motionEvent = (MotionEvent) param.getArgs()[0];
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        float mDownX = EzxHelpUtils.getFloatField(param.getThisObject(), "mDownX");
                        int mAssistantWidth = EzxHelpUtils.getIntField(param.getThisObject(), "mAssistantWidth");
                        inDirection = mDownX < mAssistantWidth ? 0 : 1;
                    }
                }
            }
        );

        findAndHookConstructor("com.miui.home.recents.NavStubView",
            Context.class, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    mContext = (Context) param.getArgs()[0];
                }
            }
        );

        findAndHookMethod("com.miui.home.recents.SystemUiProxyWrapper",
            "startAssistant", Bundle.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    Bundle bundle = (Bundle) param.getArgs()[0];
                    if (bundle.getInt("triggered_by", 0) != 83 || bundle.getInt("invocation_type", 0) != 1) {
                        return;
                    }
                    String direction = inDirection == 1 ? "right" : "left";
                    GlobalActions.handleAction(
                        EzXposed.getAppContext(),
                        "home_navigation_assist_" + direction + "_slide"
                    );
                }
            }
        );
    }
}
