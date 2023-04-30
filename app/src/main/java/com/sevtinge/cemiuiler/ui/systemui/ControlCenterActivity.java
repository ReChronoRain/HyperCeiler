package com.sevtinge.cemiuiler.ui.systemui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.VariousActivity;
import com.sevtinge.cemiuiler.ui.base.PreferenceFragment;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;
import com.sevtinge.cemiuiler.utils.SdkHelper;
import moralnorm.preference.SwitchPreference;

public class ControlCenterActivity extends BaseSystemUIActivity {

    @Override
    public Fragment initFragment() {
        return new ControlCenterActivity.ControlCenterFragment();
    }

    public static class ControlCenterFragment extends SubFragment {

        SwitchPreference mFixMediaPanel;

        @Override
        public int getContentResId() {
            return R.xml.system_ui_control_center;
        }

        @Override
        public void initPrefs() {
            mFixMediaPanel = findPreference("prefs_key_system_ui_control_center_fix_media_control_panel");
            mFixMediaPanel.setVisible(SdkHelper.isAndroidS() || SdkHelper.isAndroidSv2());
            mFixMediaPanel.setOnPreferenceChangeListener((preference, o) -> true);
    }
}
}
