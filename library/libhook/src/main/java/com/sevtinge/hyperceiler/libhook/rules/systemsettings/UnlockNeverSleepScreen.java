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
package com.sevtinge.hyperceiler.libhook.rules.systemsettings;

import android.content.Context;
import android.util.AttributeSet;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class UnlockNeverSleepScreen extends BaseHook {
    private final ThreadLocal<Boolean> creatingPreference = ThreadLocal.withInitial(() -> false);

    @Override
    public void init() {
        findAndHookMethod("android.os.SystemProperties", "get", String.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (!creatingPreference.get()) return;

                String key = (String) param.getArgs()[0];
                if ("ro.vendor.display.type".equals(key) || "ro.display.type".equals(key)) {
                    param.setResult("lcd");
                }
            }
        });

        findAndHookConstructor("com.android.settings.KeyguardTimeoutListPreference", Context.class, AttributeSet.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                creatingPreference.set(true);
            }

            @Override
            public void after(HookParam param) {
                creatingPreference.remove();
            }
        });
    }
}
