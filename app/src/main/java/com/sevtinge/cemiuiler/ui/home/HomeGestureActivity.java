package com.sevtinge.cemiuiler.ui.home;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.PickerHomeActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.home.base.BaseHomeActivity;

import moralnorm.preference.Preference;

public class HomeGestureActivity extends BaseHomeActivity {

    @Override
    public Fragment initFragment() {
        return new HomeGestureFragment();
    }

    public static class HomeGestureFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.home_gesture;
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference != null) {
                openMultiAction(preference, null, PickerHomeActivity.Actions.Home);
            }
            return true;
        }
    }
}
