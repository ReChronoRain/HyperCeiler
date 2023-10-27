package com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class MobileNetworkTypeSettings extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() {
        return R.xml.system_ui_status_bar_mobile_network_type;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }
}
