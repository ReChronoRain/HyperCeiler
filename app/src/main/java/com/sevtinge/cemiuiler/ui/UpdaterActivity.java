package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;
import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import moralnorm.preference.EditTextPreference;

public class UpdaterActivity extends BaseAppCompatActivity {

        @Override
        public Fragment initFragment() {
            return new com.sevtinge.cemiuiler.ui.UpdaterActivity.UpdaterFragment();
        }

        public static class UpdaterFragment extends SubFragment {

            EditTextPreference mDeviceModify;

            @Override
            public int getContentResId() {
                return R.xml.updater;
            }

            @Override
            public void initPrefs() {
                mDeviceModify = findPreference("prefs_key_updater_device");

                if (!getSharedPreferences().getBoolean("prefs_key_hidden_function",false)) {
                    mDeviceModify.setVisible(false);
                }
            }

        }




    }



