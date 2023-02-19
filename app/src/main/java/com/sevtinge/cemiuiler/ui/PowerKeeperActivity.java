package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;
import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class PowerKeeperActivity  extends BaseAppCompatActivity {

        @Override
        public Fragment initFragment() {
            return new com.sevtinge.cemiuiler.ui.PowerKeeperActivity.PowerKeeperFragment();
        }

        public static class PowerKeeperFragment extends SubFragment {

            @Override
            public int getContentResId() {
                return R.xml.powerkeeper;
            }
        }


    }


