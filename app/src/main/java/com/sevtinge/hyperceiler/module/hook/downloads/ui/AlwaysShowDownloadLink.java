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
package com.sevtinge.hyperceiler.module.hook.downloads.ui;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class AlwaysShowDownloadLink extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("h1.h", "F", String.class,new MethodHook() {
            @Override
            public void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        findAndHookMethod("h1.h", "R", "i1.a",new MethodHook() {
            @Override
            public void before(MethodHookParam param) throws Throwable {
                // @TODO 显示来源应用和任务来源
                logD(TAG, lpparam.packageName, "source: " + getObjectField(param.args[0], "r") + "  path: " + getObjectField(param.args[0], "i"));
                setObjectField(param.args[0], "y", "");
            }
        });
    }
}
