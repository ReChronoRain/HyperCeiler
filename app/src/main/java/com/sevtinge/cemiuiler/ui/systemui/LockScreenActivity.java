package com.sevtinge.cemiuiler.ui.systemui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;

public class LockScreenActivity extends BaseSystemUIActivity {

    @Override
    public Fragment initFragment() {
        return new LockScreenFragment();
    }

    public static class LockScreenFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.system_ui_lock_screen;
        }
    }
}
