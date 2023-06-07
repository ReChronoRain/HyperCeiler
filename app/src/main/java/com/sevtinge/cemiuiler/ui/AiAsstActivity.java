package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class AiAsstActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new AiAsstActivity.AiAsstFragment();
    }


    public static class AiAsstFragment extends SubFragment {
        @Override
        public int getContentResId() {
            return R.xml.aiasst;
        }
    }
}
