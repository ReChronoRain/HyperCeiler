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
package com.sevtinge.hyperceiler.hook.module.hook.home.title;

import android.view.MotionEvent;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;
import kotlin.jvm.internal.Intrinsics;

// from MIUI-EXTRA by Art-Chen

public class FixAnimation extends BaseHook {

    private Object mAppToHomeAnim2Bak;

    private final Runnable mRunnable = () -> {
    };

    public final Object getMAppToHomeAnim2Bak() {
        return mAppToHomeAnim2Bak;
    }

    public final void setMAppToHomeAnim2Bak(Object obj) {
        mAppToHomeAnim2Bak = obj;
    }

    public final Runnable getMRunnable() {
        return mRunnable;
    }

    @Override
    public void init() {

        Intrinsics.checkNotNull(lpparam);

        findAndHookMethod("com.miui.home.recents.NavStubView", "onInputConsumerEvent", MotionEvent.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                Intrinsics.checkNotNullParameter(param, "param");
                setMAppToHomeAnim2Bak(XposedHelpers.getObjectField(param.thisObject, "mAppToHomeAnim2"));
                if (getMAppToHomeAnim2Bak() != null) {
                    XposedHelpers.setObjectField(param.thisObject, "mAppToHomeAnim2", null);
                }
            }

            @Override
            protected void after(MethodHookParam param) {
                Intrinsics.checkNotNullParameter(param, "param");
                Object obj = param.args[0];
                Intrinsics.checkNotNull(obj, "null cannot be cast to non-null type android.view.MotionEvent");
                MotionEvent motionEvent = (MotionEvent) obj;
                //logI(TAG, FixAnimation.this.lpparam.packageName, "onInputConsumerEvent: Action: " + motionEvent.getAction() + ", return " + param.getResult() + ". x: " + motionEvent.getX() + " y: " + motionEvent.getY());
                if (XposedHelpers.getObjectField(param.thisObject, "mAppToHomeAnim2") != null || getMAppToHomeAnim2Bak() == null) {
                    return;
                }
                XposedHelpers.setObjectField(param.thisObject, "mAppToHomeAnim2", getMAppToHomeAnim2Bak());
            }

        });

        findAndHookMethod("com.miui.home.launcher.ItemIcon", "initPerformClickRunnable", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Intrinsics.checkNotNullParameter(param, "param");
                param.setResult(getMRunnable());
            }

        });
    }
}
