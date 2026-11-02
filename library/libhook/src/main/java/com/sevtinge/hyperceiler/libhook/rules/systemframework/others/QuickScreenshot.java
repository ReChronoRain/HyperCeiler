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
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class QuickScreenshot extends BaseHook {

    /**
     * 截屏快捷键长按时延的取值方法。它的宿主类在框架重构中搬过家
     * （PhoneWindowManager -> KeyGestureController），且各 ROM 版本停在哪一侧并不一致，
     * 所以这里不锁定单一宿主，而是按「方法名」在所有候选宿主里试挂：
     * 哪个宿主实际带这个方法就挂哪个，与 ROM / Android 版本号无关。
     */
    private static final String[] DELAY_HOSTS = {
        "com.android.server.input.KeyGestureController",
        "com.android.server.policy.PhoneWindowManager",
    };

    @Override
    public void init() {
        IMethodHook hook = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(0L);
            }
        };

        for (String host : DELAY_HOSTS) {
            Class<?> clazz = findClassIfExists(host);
            if (clazz == null) {
                continue;
            }
            if (!hookAllMethods(clazz, "getScreenshotChordLongPressDelay", hook).isEmpty()) {
                break;
            }
        }
    }
}
