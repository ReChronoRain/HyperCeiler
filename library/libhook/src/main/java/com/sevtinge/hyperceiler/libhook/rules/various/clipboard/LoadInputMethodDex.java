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

package com.sevtinge.hyperceiler.libhook.rules.various.clipboard;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

;

/**
 * 获取常用语的 classloader。
 * from <a href="https://github.com/HChenX/ClipboardList">ClipboardList</a>
 *
 * @author 焕晨HChen
 */
public class LoadInputMethodDex extends BaseHook {
    private boolean isLoaded;

    @Override
    public void init() {
        findAndHookMethod("android.inputmethodservice.InputMethodModuleManager",
            "loadDex",
            ClassLoader.class, String.class,
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    if (isLoaded) return;

                    ClassLoader classLoader = (ClassLoader) param.getArgs()[0];
                    ClipboardLimit.unlock(classLoader);

                    XposedLog.d(TAG, "Input method classloader: " + classLoader);
                    isLoaded = true;
                }
            }
        );

    }

}
