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

package com.sevtinge.hyperceiler.module.hook.home;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DisablePrestart extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {

        findAndHookMethod("com.miui.launcher.utils.SystemProperties", "getBoolean", String.class, boolean.class, new MethodHook() {
            private final Set<String> prop = new HashSet<>(Arrays.asList(
                    "persist.sys.usap_pool_enabled",
                    "persist.sys.dynamic_usap_enabled",
                    "persist.sys.prestart.proc",
                    "persist.sys.prestart.feedback.enable",
                    "persist.sys.launch_response_optimization.enable"
            ));

            @Override
            protected void before(MethodHookParam param) throws Throwable {
                String key = (String) param.args[0];
                if (prop.contains(key)) {
                    param.setResult(false);
                }
            }
        });

    }
}
