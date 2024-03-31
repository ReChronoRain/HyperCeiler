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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.externalstorage;

import static com.sevtinge.hyperceiler.module.base.tool.HookTool.MethodHook.returnConstant;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DisableFolderCantUse extends BaseHook {

    private final static List<String> METHOD_NAME_LIST = Arrays.asList("shouldBlockFromTree", "shouldBlockDirectoryFromTree");

    @Override
    public void init() {
        Class<?> externalStorageProvider = findClass("com.android.externalstorage.ExternalStorageProvider");
        List<Method> methodList = Arrays.stream(externalStorageProvider.getDeclaredMethods())
                .filter(method -> METHOD_NAME_LIST.contains(method.getName()))
                .filter(method -> method.getReturnType() == boolean.class).collect(Collectors.toList());

        if (methodList.isEmpty()) {
            logE(TAG, lpparam.packageName, new NoSuchMethodException("shouldBlockFromTree"));
            return;
        }

        for (Method method : methodList) {
            hookMethod(method, returnConstant(false));
        }
    }
}
