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
package com.sevtinge.hyperceiler.libhook.rules.externalstorage;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

;

public class DisableFolderCantUse extends BaseHook {

    private final static List<String> METHOD_NAME_LIST = Arrays.asList("shouldBlockFromTree", "shouldBlockDirectoryFromTree");

    @Override
    public void init() {
        Class<?> externalStorageProvider = findClass("com.android.externalstorage.ExternalStorageProvider");
        List<Method> methodList = Arrays.stream(externalStorageProvider.getDeclaredMethods())
                .filter(method -> METHOD_NAME_LIST.contains(method.getName()))
                .filter(method -> method.getReturnType() == boolean.class).toList();

        if (methodList.isEmpty()) {
            XposedLog.e(TAG, getPackageName(), new NoSuchMethodException("shouldBlockFromTree"));
            return;
        }

        for (Method method : methodList) {
            hookMethod(method, returnConstant(false));
        }
    }
}
