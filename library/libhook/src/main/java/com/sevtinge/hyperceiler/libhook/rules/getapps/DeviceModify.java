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
package com.sevtinge.hyperceiler.libhook.rules.getapps;

import android.os.Build;

import com.sevtinge.hyperceiler.common.utils.GetAppsDeviceConfig;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

/** Process-start Build identity. Existing consumer caches require an app restart. */
public class DeviceModify extends BaseHook {
    private final GetAppsDeviceConfig config;

    public DeviceModify() {
        this(GetAppsDeviceConfig.from(PrefsBridge.getSharedPreferences().getAll()));
    }

    public DeviceModify(GetAppsDeviceConfig config) {
        this.config = config;
    }

    @Override
    public void init() {
        if (!config.enabled || !config.hasOverrides()) return;
        Class<?> app = findClassIfExists("com.xiaomi.market.MarketApp");
        if (app == null) throw new IllegalStateException("GetApps MarketApp is unavailable");
        String[] names = {"DEVICE", "MODEL", "MANUFACTURER"};
        String[] values = {config.device, config.model, config.manufacturer};
        for (String name : names) {
            Field field;
            try {
                field = Build.class.getDeclaredField(name);
            } catch (NoSuchFieldException error) {
                throw new IllegalStateException("Missing Build identity field: " + name, error);
            }
            if (field.getType() != String.class || !Modifier.isStatic(field.getModifiers())) {
                throw new IllegalStateException("Unexpected Build identity field: " + name);
            }
        }
        if (findAndHookConstructor(app, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (param.isSkipped()) return;
                // MarketApp.<clinit> does not capture this identity. Its constructor
                // starts delegates/async resource work, so write before that body.
                applyIdentity(names, values);
            }
        }) == null) throw new IllegalStateException("GetApps constructor hook was not installed");
    }

    private void applyIdentity(String[] names, String[] values) {
        String[] original = new String[names.length];
        int attempted = -1;
        try {
            for (int i = 0; i < names.length; i++) {
                original[i] = (String) getStaticObjectField(Build.class, names[i]);
            }
            for (int i = 0; i < names.length; i++) {
                if (values[i] == null || Objects.equals(values[i], original[i])) continue;
                attempted = i;
                setStaticObjectField(Build.class, names[i], values[i]);
                if (!Objects.equals(getStaticObjectField(Build.class, names[i]), values[i])) {
                    throw new IllegalStateException("Build identity write did not persist: " + names[i]);
                }
            }
        } catch (Throwable error) {
            // Include the attempted field: a backend can write and then throw.
            for (int i = attempted; i >= 0; i--) {
                if (values[i] == null || Objects.equals(values[i], original[i])) continue;
                try {
                    // Never overwrite a value that a different writer has replaced.
                    if (Objects.equals(getStaticObjectField(Build.class, names[i]), values[i])) {
                        setStaticObjectField(Build.class, names[i], original[i]);
                        if (!Objects.equals(getStaticObjectField(Build.class, names[i]), original[i])) {
                            throw new IllegalStateException("Build identity rollback did not persist");
                        }
                    }
                } catch (Throwable rollbackError) {
                    error.addSuppressed(rollbackError);
                }
            }
            debugCallbackError("GetApps Build identity", error);
        }
    }
}
