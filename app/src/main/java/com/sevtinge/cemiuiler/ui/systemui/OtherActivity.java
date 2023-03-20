package com.sevtinge.cemiuiler.ui.systemui;

import android.os.Build;
import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;

import com.sevtinge.cemiuiler.utils.SdkHelper;
import moralnorm.os.SdkVersion;
import moralnorm.preference.PreferenceCategory;

public class OtherActivity extends BaseSystemUIActivity {
    @Override
    public Fragment initFragment() {
        return new SystemUIOtherFragment();
    }

    public static class SystemUIOtherFragment extends SubFragment {

        PreferenceCategory mMonetOverlay;

        @Override
        public int getContentResId() {
            return R.xml.system_ui_other;
        }

        @Override
        public void initPrefs() {
            mMonetOverlay = findPreference("prefs_key_system_ui_monet");
            mMonetOverlay.setVisible(!SdkHelper.isAndroidR());
        }
    }
}
