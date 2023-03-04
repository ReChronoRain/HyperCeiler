package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;
import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class InCallUiActivity extends BaseAppCompatActivity {

        @Override
        public Fragment initFragment() {
            return new com.sevtinge.cemiuiler.ui.InCallUiActivity.InCallUiFragment();
        }

        public static class InCallUiFragment extends SubFragment {

            @Override
            public int getContentResId() {
                return R.xml.incallui;
            }
        }


    }


