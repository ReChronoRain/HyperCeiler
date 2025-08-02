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

import static android.os.Looper.getMainLooper;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreSmallVersion;

import android.os.Handler;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool;
import com.sevtinge.hyperceiler.hook.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.ui.R;

public class VariousFragment extends DashboardFragment {
    PreferenceCategory mDefault;
    SwitchPreference mClipboard;
    SwitchPreference mClipboardClear;
    Preference mMipad; // 平板相关功能

    Handler handler;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.various;
    }

    @Override
    public void initPrefs() {
        mDefault = findPreference("prefs_key_various_super_clipboard_key");
        mMipad = findPreference("prefs_key_various_mipad");
        mClipboard = findPreference("prefs_key_sogou_xiaomi_clipboard");
        mClipboardClear = findPreference("prefs_key_add_clipboard_clear");
        mMipad.setVisible(isPad());

        if (isMoreSmallVersion(200, 2f)) {
            setFuncHint(mClipboardClear, 2);
        } else {
            setHide(mClipboardClear, isMoreHyperOSVersion(2f));
        }
        handler = new Handler(getMainLooper());

        mClipboard.setOnPreferenceChangeListener((preference, o) -> {
            initKill();
            return true;
        });
    }

    private void initKill() {
        ThreadPoolManager.getInstance().submit(() -> {
            handler.post(() ->
                AppsTool.killApps("com.sohu.inputmethod.sogou.xiaomi",
                    "com.sohu.inputmethod.sogou"));
        });
    }
}
