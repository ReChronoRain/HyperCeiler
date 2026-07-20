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

package com.sevtinge.hyperceiler.libhook.rules.systemsettings;

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import java.util.List;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class MoreVpnTypes extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.internal.net.VpnProfile", "isLegacyType", int.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                int type = (int) param.getArgs()[0];
                if (type >= 0 && type <= 5) {
                    param.setResult(true);
                }
            }
        });

        Class<?> editFragment = findClassIfExists("com.android.settings.vpn2.MiuiVpnEditFragment");
        Class<?> configDialog = findClassIfExists("com.android.settings.vpn2.ConfigDialog");
        if (!canSetVpnTypes(editFragment) || !canSetVpnTypes(configDialog)) return;
        if (editFragment == null && configDialog == null) return;

        setVpnTypes(editFragment);
        setVpnTypes(configDialog);
        setResReplacement("com.android.settings", "array", "vpn_types", R.array.hook_system_settings_vpn_types);
    }

    private boolean canSetVpnTypes(Class<?> clazz) {
        return clazz == null || findFieldIfExists(clazz, "VPN_TYPES") != null;
    }

    private void setVpnTypes(Class<?> clazz) {
        if (clazz != null) {
            setStaticObjectField(clazz, "VPN_TYPES", List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
        }
    }
}
