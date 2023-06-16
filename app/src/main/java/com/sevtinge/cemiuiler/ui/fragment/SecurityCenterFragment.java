package com.sevtinge.cemiuiler.ui.fragment;

import static com.sevtinge.cemiuiler.utils.api.VoyagerApisKt.isPad;

import android.Manifest;
import android.provider.Settings;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class SecurityCenterFragment extends SettingsPreferenceFragment {
    String mSecurity;
    SwitchPreference mAiClipboard;
    SwitchPreference mBlurLocation;
    Preference mNewboxBackgroundCustom;

    SwitchPreference mBeautyLight;
    SwitchPreference mBeautyLightAuto;

    @Override
    public int getContentResId() {
        return R.xml.security_center;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        mSecurity = getResources().getString(!isPad() ? R.string.security_center : R.string.security_center_pad);
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

        mNewboxBackgroundCustom = findPreference("prefs_key_security_center_newbox_bg_custom");
        mBeautyLight = findPreference("prefs_key_security_center_beauty_light");
        mBeautyLightAuto = findPreference("prefs_key_security_center_beauty_light_auto");

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

        mBeautyLight.setOnPreferenceChangeListener((preference, o) -> {
            if (!(boolean) o) {
                mBeautyLightAuto.setChecked(false);
            }
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
