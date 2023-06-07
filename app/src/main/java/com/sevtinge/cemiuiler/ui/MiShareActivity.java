package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

import moralnorm.preference.SwitchPreference;

public class MiShareActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.MiShareActivity.MiShareFragment();
    }

    public static class MiShareFragment extends SubFragment {

        SwitchPreference mMiShareNotAuto;

        @Override
        public int getContentResId() {
            return R.xml.mishare;
        }

        @Override
        public void initPrefs() {
            mMiShareNotAuto = findPreference("prefs_key_disable_mishare_auto_off");
/*
            int appVersionCode = getPackageVersionCode(lpparam);

            if (appVersionCode <= 21400) {
                mMiShareNotAuto.setSummary(R.string.app_version_not_supported);
                mMiShareNotAuto.setEnabled(false);
            }
*/
        }
    }

}


