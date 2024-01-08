package com.sevtinge.hyperceiler.ui.fragment;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.ShellUtils;

import moralnorm.preference.Preference;

public class PhoneFragment extends SettingsPreferenceFragment {
    Preference mPhone;

    @Override
    public int getContentResId() {
        return R.xml.phone;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.phone),
            "com.android.phone"
        );
    }

    @Override
    public void initPrefs() {
        mPhone = findPreference("prefs_key_phone_additional_network_settings");
        mPhone.setOnPreferenceClickListener(
            preference -> {
                ShellUtils.execCommand("am start -n com.android.phone/.SwitchDebugActivity", true, false);
                return true;
            }
        );
    }
}
