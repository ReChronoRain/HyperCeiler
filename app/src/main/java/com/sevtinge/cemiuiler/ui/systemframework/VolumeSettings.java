package com.sevtinge.cemiuiler.ui.systemframework;

import android.provider.Settings;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemframework.base.BaseSystemFrameWorkActivity;

import moralnorm.preference.DropDownPreference;

public class VolumeSettings extends BaseSystemFrameWorkActivity {

    @Override
    public Fragment initFragment() {
        return new VolumeFragment();
    }

    public static class VolumeFragment extends SubFragment {

        DropDownPreference mDefaultVolumeStream;

        @Override
        public int getContentResId() {
            return R.xml.system_framework_volume;
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
}
