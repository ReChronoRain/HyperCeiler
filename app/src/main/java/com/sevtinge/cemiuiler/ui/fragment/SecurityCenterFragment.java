package com.sevtinge.cemiuiler.ui.fragment;

import android.Manifest;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class SecurityCenterFragment extends SettingsPreferenceFragment {

    SwitchPreference mAiClipboard;
    SwitchPreference mBlurLocation;
    Preference mNewboxBackgroundCustom;

    @Override
    public int getContentResId() {
        return R.xml.security_center;
    }

    @Override
    public void initPrefs() {
        int permission = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_SECURE_SETTINGS);

        mBlurLocation = findPreference("prefs_key_security_center_blur_location");
        mAiClipboard = findPreference("prefs_key_security_center_ai_clipboard");

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
