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
package com.sevtinge.hyperceiler.libhook.rules.home;

import android.view.MotionEvent;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;
import io.github.libxposed.api.XposedInterface.HookHandle;

public class ToastSlideAgain extends BaseHook {
    public HookHandle unhook = null;

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.recents.NavStubView",
            "onPointerEvent", MotionEvent.class,
            new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    unhook = hookToast();
                    // logI("im hook onPointerEvent");
                }

                @Override
                public void after(HookParam param) {
                    unHook(unhook);
                }
            }
        );

        findAndHookMethod("com.miui.home.recents.GestureModeApp",
             "onStartGesture", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    unhook = hookToast();
                    // logI("im hook onStartGesture");
                }

                @Override
                public void after(HookParam param) {
                    unHook(unhook);
                }
            }
        );
    }

    public HookHandle hookToast() {
        return findAndHookMethod(findClassIfExists("android.widget.Toast"),
            "show", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    param.setResult(null);
                    // logI("im hook Toast show");
                }
            }
        );
    }

    public void unHook(HookHandle unhook) {
        if (unhook != null) {
            unhook.unhook();
            // logI("the unhook is: " + unhook);
        }  // logE("the unhook is: null");

    }
}
