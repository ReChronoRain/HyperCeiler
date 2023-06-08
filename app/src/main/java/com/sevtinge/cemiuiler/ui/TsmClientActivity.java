package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class TsmClientActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.TsmClientActivity.TsmClientFragment();
    }

    public static class TsmClientFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.tsmclient;
        }
    }


}
