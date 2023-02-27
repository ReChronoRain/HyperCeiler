package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;
import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class MirrorActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.MirrorActivity.MirrorFragment();
    }

    public static class MirrorFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.mirror;
        }
    }


}





