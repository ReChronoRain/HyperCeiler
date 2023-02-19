package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;
import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class ScreenShotActivity extends BaseAppCompatActivity {

        @Override
        public Fragment initFragment() {
            return new com.sevtinge.cemiuiler.ui.ScreenShotActivity.ScreenShotFragment();
        }

        public static class ScreenShotFragment extends SubFragment {

            @Override
            public int getContentResId() {
                return R.xml.screenshot;
            }
        }


    }


