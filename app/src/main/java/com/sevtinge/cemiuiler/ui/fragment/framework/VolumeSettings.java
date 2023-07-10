package com.sevtinge.cemiuiler.ui.fragment.framework;

import android.provider.Settings;
import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.DropDownPreference;

public class VolumeSettings extends SettingsPreferenceFragment {
    DropDownPreference mDefaultVolumeStream;

    @Override
    public int getContentResId() {
        return R.xml.framework_volume;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartSystemDialog();
    }

    @Override
    public void initPrefs() {
        mDefaultVolumeStream = findPreference("prefs_key_system_framework_default_volume_stream");

        mDefaultVolumeStream.setOnPreferenceChangeListener((preference, o) -> {
            Settings.Secure.putInt(getContext().getContentResolver(), "system_framework_default_volume_stream", Integer.parseInt((String) o));
            return true;
        });
    }
}
