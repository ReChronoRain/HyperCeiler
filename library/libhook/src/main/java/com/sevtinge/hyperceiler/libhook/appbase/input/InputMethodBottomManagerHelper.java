/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.appbase.input;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

public final class InputMethodBottomManagerHelper {
    private static final String INPUT_METHOD_BOTTOM_MANAGER =
        "com.miui.inputmethod.InputMethodBottomManager";

    private InputMethodBottomManagerHelper() {
    }

    @Nullable
    public static Class<?> findBottomManagerClass(ClassLoader classLoader) {
        return EzxHelpUtils.findClassIfExists(INPUT_METHOD_BOTTOM_MANAGER, classLoader);
    }

    @Nullable
    public static Object getEnabledInputMethodList(ClassLoader classLoader) {
        Class<?> inputMethodBottomManager = findBottomManagerClass(classLoader);
        if (inputMethodBottomManager == null) {
            return null;
        }

        Object bottomViewHelper = BaseHook.getStaticObjectField(inputMethodBottomManager, "sBottomViewHelper");
        if (bottomViewHelper == null) {
            return null;
        }

        Object inputMethodManager = BaseHook.getObjectField(bottomViewHelper, "mImm");
        if (inputMethodManager == null) {
            return null;
        }

        return BaseHook.callMethod(inputMethodManager, "getEnabledInputMethodList");
    }
}
