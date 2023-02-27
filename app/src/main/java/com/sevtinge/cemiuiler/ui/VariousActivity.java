package com.sevtinge.cemiuiler.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

import moralnorm.os.SdkVersion;
import moralnorm.preference.SwitchPreference;

public class VariousActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Fragment initFragment() {
        return new VariousFragment();
    }

    public static class VariousFragment extends SubFragment {

        SwitchPreference mDisableBluetoothRestrict;

        @Override
        public int getContentResId() {
            return R.xml.various;
        }

        @Override
        public void initPrefs() {
            mDisableBluetoothRestrict = findPreference("prefs_key_various_disable_bluetooth_restrict");
            mDisableBluetoothRestrict.setVisible(SdkVersion.isAndroidT);

            mDisableBluetoothRestrict.setOnPreferenceChangeListener((preference, o) -> {
                return true;
            });

        }
    }
}
