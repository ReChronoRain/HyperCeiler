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
package com.sevtinge.hyperceiler.libhook.rules.gallery;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookConstructor;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

;

public class LongerTrashbinTime extends BaseHook {
    @Override
    public void init() {
        findAndHookConstructor("com.miui.gallery.trash.TrashUtils$UserInfo", String.class, String.class, String.class, long.class, long.class, long.class, long.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.getArgs()[3] = 31536000000L;
                param.getArgs()[6] = 31536000000L;
            }
        });
    }
}
