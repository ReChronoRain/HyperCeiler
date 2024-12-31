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
package com.sevtinge.hyperceiler.ui.fragment.app;

import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.os.Handler;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.KillApp;
import com.sevtinge.hyperceiler.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

public class VariousFragment extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    DropDownPreference mSuperModePreference;
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
        int mode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_various_super_clipboard_e", "0"));
        mSuperModePreference = findPreference("prefs_key_various_super_clipboard_e");
        mDefault = findPreference("prefs_key_various_super_clipboard_key");
        mMipad = findPreference("prefs_key_various_mipad");
        mClipboard = findPreference("prefs_key_sogou_xiaomi_clipboard");
        mClipboardClear = findPreference("prefs_key_add_clipboard_clear");
        mMipad.setVisible(isPad());
        mClipboardClear.setVisible(isMoreHyperOSVersion(2f));
        handler = new Handler();

        mClipboard.setOnPreferenceChangeListener((preference, o) -> {
            initKill();
            return true;
        });
        setSuperMode(mode);
        mSuperModePreference.setOnPreferenceChangeListener(this);
    }

    private void initKill() {
        ThreadPoolManager.getInstance().submit(() -> {
            handler.post(() ->
                KillApp.killApps("com.sohu.inputmethod.sogou.xiaomi",
                    "com.sohu.inputmethod.sogou"));
        });
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mSuperModePreference) {
            setSuperMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setSuperMode(int mode) {
        mDefault.setVisible(mode == 1);
    }
}
