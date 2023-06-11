package com.sevtinge.cemiuiler.ui.fragment.systemui.statusbar;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

public class BatteryDetailIndicatorSettings extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() {
        return R.xml.system_ui_status_bar_hardware_detail_indicator;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }
}
