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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.home.other;

import com.sevtinge.hyperceiler.module.base.BaseHook;

/* from 绿龟龟
（我并不会Java，所以此hook代码由ai帮助生成，应该还算是有模有样吧（笑*/

public class DisableParallelAnim extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.recents.anim.StateManager",
                "cancelAnim",
                String.class,     
                boolean.class,      
                "com.android.systemui.shared.recents.utilities.ShellTransitionCallback",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.args[1] = true;
                    }
                });
    }
}
