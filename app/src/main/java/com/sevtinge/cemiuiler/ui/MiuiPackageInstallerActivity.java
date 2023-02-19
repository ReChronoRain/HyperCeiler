package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class MiuiPackageInstallerActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new MiuiPackageInstallerFragment();
    }


    public static class MiuiPackageInstallerFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.prefs_various_miui_package_installer;
        }
    }
}
