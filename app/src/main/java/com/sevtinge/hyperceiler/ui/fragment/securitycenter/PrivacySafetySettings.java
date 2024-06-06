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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.ui.fragment.securitycenter;

import android.Manifest;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import com.sevtinge.hyperceiler.R;

import moralnorm.preference.SwitchPreference;

public class PrivacySafetySettings extends SecurityCenterBaseSettings {

    SwitchPreference mAiClipboard;
    SwitchPreference mBlurLocation;

    @Override
    public int getContentResId() {
        return R.xml.security_center_privacy_safety;
    }

    @Override
    public void initPrefs() {
        mBlurLocation = findPreference("prefs_key_security_center_blur_location");
        mAiClipboard = findPreference("prefs_key_security_center_ai_clipboard");

        int permission = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_SECURE_SETTINGS);
        if (permission != PermissionChecker.PERMISSION_GRANTED) {
            mBlurLocation.setSummary(R.string.security_center_no_permission);
            mAiClipboard.setSummary(R.string.security_center_no_permission);
            mBlurLocation.setEnabled(false);
            mAiClipboard.setEnabled(false);
        } else {
            boolean mBlurLocationEnable = Settings.Secure.getInt(getContext().getContentResolver(), "mi_lab_blur_location_enable", 0) == 1;
            boolean mAiClipboardEnable = Settings.Secure.getInt(getContext().getContentResolver(), "mi_lab_ai_clipboard_enable", 0) == 1;

            mBlurLocation.setChecked(mBlurLocationEnable);
            mAiClipboard.setChecked(mAiClipboardEnable);
        }

        boolean mBlurLocationEnable = Settings.Secure.getInt(getContext().getContentResolver(), "mi_lab_blur_location_enable", 0) == 1;
        boolean mAiClipboardEnable = Settings.Secure.getInt(getContext().getContentResolver(), "mi_lab_ai_clipboard_enable", 0) == 1;

        mBlurLocation.setChecked(mBlurLocationEnable);
        mAiClipboard.setChecked(mAiClipboardEnable);

        mBlurLocation.setOnPreferenceChangeListener((preference, o) -> {
            Settings.Secure.putInt(getContext().getContentResolver(), "mi_lab_blur_location_enable", (Boolean) o ? 1 : 0);
            return true;
        });

        mAiClipboard.setOnPreferenceChangeListener((preference, o) -> {
            Settings.Secure.putInt(getContext().getContentResolver(), "mi_lab_ai_clipboard_enable", (Boolean) o ? 1 : 0);
            return true;
        });
    }
}
