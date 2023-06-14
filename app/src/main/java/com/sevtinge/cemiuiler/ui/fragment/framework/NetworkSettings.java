package com.sevtinge.cemiuiler.ui.fragment.framework;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

public class NetworkSettings extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() {
        return R.xml.framework_phone;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartSystemDialog();
    }
}
