package com.sevtinge.cemiuiler.ui.systemui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.PreferenceFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;

public class OtherActivity extends BaseSystemUIActivity {

    @Override
    public Fragment initFragment() {
        return new PreferenceFragment(R.xml.system_ui_other);
    }
}
