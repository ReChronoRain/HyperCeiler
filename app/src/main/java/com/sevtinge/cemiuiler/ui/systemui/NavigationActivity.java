package com.sevtinge.cemiuiler.ui.systemui;

import android.widget.SeekBar;
import androidx.fragment.app.Fragment;
import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;
import moralnorm.preference.SeekBarPreference;
import moralnorm.preference.SeekBarPreferenceEx;

public class NavigationActivity extends BaseSystemUIActivity {


    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.systemui.NavigationActivity.NavigationFragment();
    }

    public static class NavigationFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.system_ui_navigation;
        }



    }
}


