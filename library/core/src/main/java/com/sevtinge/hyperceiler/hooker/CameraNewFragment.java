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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hooker;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.common.prefs.LayoutPreference;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

public class CameraNewFragment extends DashboardFragment {

    SwitchPreference mBlackLeica;
    SwitchPreference mLeica;
    SwitchPreference mHighQuality;
    LayoutPreference mHeader;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.camera_new;
    }

    @Override
    public void initPrefs() {
        mBlackLeica = findPreference("prefs_key_camera_black_leica");
        mLeica = findPreference("prefs_key_camera_unlock_leica");
        mHighQuality = findPreference("prefs_key_camera_super_high_quality");

        // 别问，问就是平板连方法都砍掉了，连支持都不行
        if (isPad()) {
            setFuncHint(mBlackLeica, 1);
            setFuncHint(mLeica, 1);
            setFuncHint(mHighQuality, 1);
        }

        mHeader = findPreference("prefs_key_camera_unsupported");

        setAppModWarn(mHeader, "com.android.camera");
    }
}
