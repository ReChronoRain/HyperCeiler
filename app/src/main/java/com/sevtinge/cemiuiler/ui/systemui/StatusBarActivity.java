package com.sevtinge.cemiuiler.ui.systemui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.Preference;

public class StatusBarActivity extends BaseSystemUIActivity {

    @Override
    public Fragment initFragment() {
        return new StatusBarFragment();
    }

    public static class StatusBarFragment extends SubFragment {

        Preference mDeviceStatus; //硬件指示器

        @Override
        public int getContentResId() {
            return R.xml.system_ui_status_bar;
        }

        @Override
        public void initPrefs() {
            mDeviceStatus = findPreference("prefs_key_system_ui_status_bar_device");
            mDeviceStatus.setVisible(!SdkHelper.isAndroidR());
        }
    }
}
