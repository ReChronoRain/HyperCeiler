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

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

import java.lang.reflect.Method;

/**
 * Suppresses the "app pasted from your clipboard" toast.
 *
 * <p>The toast is posted from a lambda inside
 * ClipboardService#showAccessNotificationLocked, so the method that has to be hooked is a
 * synthetic one whose name carries a compiler assigned index and whose signature follows
 * whatever the lambda happens to capture. Both keep moving:
 * lambda$showAccessNotificationLocked$4(String, int, ArraySet) on Android 15,
 * $5(String, int, ArraySet, int) on Android 16, and on HyperOS 4.0 (Android 17) it is
 * $5(String, int, ArraySet, int, boolean[]) -- the extra boolean[] is the wasAccessShown
 * out-parameter. Matching a hard coded signature therefore threw MemberNotFoundException
 * and the toast came back.
 *
 * <p>The enclosing method name is the only stable part, so every synthetic method whose
 * name starts with that prefix is hooked instead. Skipping the lambda leaves
 * wasAccessShown false, which is what the caller should see when no notification was
 * shown.
 */
public class PstedClipboard extends BaseHook {
    private static final String CLIPBOARD_SERVICE = "com.android.server.clipboard.ClipboardService";
    private static final String LAMBDA_PREFIX = "lambda$showAccessNotificationLocked$";

    @Override
    public void init() {
        Class<?> clipboardService = findClassIfExists(CLIPBOARD_SERVICE);
        if (clipboardService == null) {
            XposedLog.w(TAG, getPackageName(), CLIPBOARD_SERVICE + " not found");
            return;
        }

        IMethodHook skip = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(null);
            }
        };

        int hooked = 0;
        for (Method method : clipboardService.getDeclaredMethods()) {
            if (method.getName().startsWith(LAMBDA_PREFIX)) {
                hookMethod(method, skip);
                hooked++;
            }
        }

        if (hooked == 0) {
            XposedLog.w(TAG, getPackageName(),
                "no " + LAMBDA_PREFIX + "* method in " + CLIPBOARD_SERVICE);
        }
    }
}
