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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.common.log.XposedLog;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import java.lang.reflect.Method;

public class QuickScreenshot extends BaseHook {
    @Override
    public void init() {
        boolean hooked = false;
        // Android 17 moved screenshot key chords to the input service. Some earlier
        // builds contain both controllers, with the active path selected by a flag.
        for (String name : new String[]{
            "com.android.server.policy.PhoneWindowManager",
            "com.android.server.input.KeyGestureController"
        }) {
            Class<?> controller = findClassIfExists(name);
            if (controller == null) continue;
            Method delay;
            try {
                delay = controller.getDeclaredMethod("getScreenshotChordLongPressDelay");
            } catch (NoSuchMethodException ignored) {
                continue;
            }
            if (delay.getReturnType() != long.class) continue;
            hookMethod(delay, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    param.setResult(0L);
                }
            });
            if (name.equals("com.android.server.input.KeyGestureController")) {
                // The local handler calls the private delay through a synthetic bridge.
                // Deoptimize both callers so an AOT-inlined delay still reaches the hook.
                deoptimizeMethods(controller, "-$$Nest$mgetScreenshotChordLongPressDelay");
                Class<?> handler = findClassIfExists(name + "$LocalKeyGestureEventHandler");
                if (handler != null) deoptimizeMethods(handler, "handleKeyGestureEvent");
            }
            hooked = true;
            XposedLog.i(TAG, getPackageName(), "Screenshot chord delay hook: " + name);
        }
        if (!hooked) {
            throw new IllegalStateException("No supported screenshot chord delay method");
        }
    }
}
