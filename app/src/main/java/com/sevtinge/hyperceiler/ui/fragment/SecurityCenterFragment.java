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
package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.Manifest;
import android.provider.Settings;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class SecurityCenterFragment extends SettingsPreferenceFragment {
    String mSecurity;
    SwitchPreference mAiClipboard;
    SwitchPreference mBlurLocation;
    Preference mNewboxBackgroundCustom;
    SwitchPreference mOpenByDefaultSetting;
    SwitchPreference mSecurityBackground;
    SwitchPreference mSecurityColor;

    @Override
    public int getContentResId() {
        return R.xml.security_center;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        mSecurity = getResources().getString(!isMoreHyperOSVersion(1f) ? (!isPad() ? R.string.security_center : R.string.security_center_pad) : R.string.security_center_hyperos);
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            mSecurity,
            "com.miui.securitycenter"
        );
    }

    @Override
    public void initPrefs() {
        int permission = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_SECURE_SETTINGS);

        mBlurLocation = findPreference("prefs_key_security_center_blur_location");
        mAiClipboard = findPreference("prefs_key_security_center_ai_clipboard");
        mOpenByDefaultSetting = findPreference("prefs_key_security_center_app_default_setting");
        mSecurityColor = findPreference("prefs_key_security_center_sidebar_line_color");
        mSecurityBackground = findPreference("prefs_key_se_enable");

        mNewboxBackgroundCustom = findPreference("prefs_key_security_center_newbox_bg_custom");

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

        mSecurityColor.setVisible(!isAndroidVersion(30)); // 侧滑栏提示线自定义
        mSecurityBackground.setVisible(!isAndroidVersion(30)); // 侧滑栏背景自定义
        mOpenByDefaultSetting.setVisible(!isAndroidVersion(30)); // 应用打开链接管理

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

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mNewboxBackgroundCustom) {
            /*openMultiAction(preference, null, PickerHomeActivity.Actions.Blur);*/
        }
        return super.onPreferenceTreeClick(preference);
    }
}
