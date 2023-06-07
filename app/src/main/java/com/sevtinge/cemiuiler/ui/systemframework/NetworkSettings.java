package com.sevtinge.cemiuiler.ui.systemframework;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.PreferenceFragment;
import com.sevtinge.cemiuiler.ui.systemframework.base.BaseSystemFrameWorkActivity;

public class NetworkSettings extends BaseSystemFrameWorkActivity {

    @Override
    public Fragment initFragment() {
        return new PreferenceFragment(R.xml.system_framework_phone);
    }
}
