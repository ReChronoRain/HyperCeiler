package com.sevtinge.hyperceiler.ui.fragment;

import android.content.ComponentName;
import android.content.Intent;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

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
        mPhone.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.SwitchDebugActivity"));
            startActivity(intent);
            return true;
        });
    }
}
