package com.sevtinge.hyperceiler.ui.fragment.framework;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

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
