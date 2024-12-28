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

package com.sevtinge.hyperceiler.module.hook.systemsettings;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import static de.robv.android.xposed.XposedHelpers.setStaticObjectField;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.List;

public class MoreVpnTypes extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.internal.net.VpnProfile", "isLegacyType", int.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        if (isMoreAndroidVersion(35)) {
            setStaticObjectField(findClassIfExists("com.android.settings.vpn2.MiuiVpnEditFragment"), "VPN_TYPES", List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
            setStaticObjectField(findClassIfExists("com.android.settings.vpn2.ConfigDialog"), "VPN_TYPES", List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
            mResHook.setResReplacement("com.android.settings", "array", "vpn_types", R.array.hook_system_settings_vpn_types);
        }
    }
}
