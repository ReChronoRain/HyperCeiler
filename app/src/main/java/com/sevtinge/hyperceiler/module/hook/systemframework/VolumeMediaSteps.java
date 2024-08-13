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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Method;

public class VolumeMediaSteps extends BaseHook {
    private Class<?> SystemProperties = null;

    @Override
    public void init() throws NoSuchMethodException {
        SystemProperties = findClassIfExists("android.os.SystemProperties");
        if (SystemProperties == null) return;
        Method[] methods = SystemProperties.getDeclaredMethods();
        for (Method method : methods) {
            hookMethod(method, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    if ("ro.config.media_vol_steps".equals(param.args[0])) {
                        if (mPrefsMap.getInt("system_framework_volume_media_steps", 15) > 15)
                            param.setResult(mPrefsMap.getInt("system_framework_volume_media_steps", 15));
                    }
                }
            });
        }
    }
}
