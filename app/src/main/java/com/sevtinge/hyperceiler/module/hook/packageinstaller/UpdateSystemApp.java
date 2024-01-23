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
package com.sevtinge.hyperceiler.module.hook.packageinstaller;

import android.content.pm.ApplicationInfo;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;

public class UpdateSystemApp extends BaseHook {

    Class<?> mClz;

    @Override
    public void init() {
        findAndHookMethod("android.os.SystemProperties", "getBoolean", String.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if ("persist.sys.allow_sys_app_update".equals(param.args[0])) {
                    param.setResult(true);
                }
            }
        });

        char letterClz = 'a';

        for (int i = 0; i < 26; i++) {
            mClz = findClass("j2." + letterClz);
            if (mClz != null) {
                int length = mClz.getDeclaredMethods().length;
                if (length >= 15 && length <= 25) {
                    List<Method> methods = List.of(mClz.getDeclaredMethods());
                    for (Method method : methods) {
                        try {
                            if (method.getParameterTypes()[0] == ApplicationInfo.class) {
                                hookMethod(method, new MethodHook() {
                                    @Override
                                    protected void before(MethodHookParam param) throws Throwable {
                                        param.setResult(false);
                                    }
                                });
                                break;

                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
            }
            letterClz = (char) (letterClz + 1);
        }
    }
}
