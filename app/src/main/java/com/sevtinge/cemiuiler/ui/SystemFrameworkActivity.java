package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.PreferenceFragment;

public class SystemFrameworkActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new PreferenceFragment(R.xml.system_framework);
    }
}
