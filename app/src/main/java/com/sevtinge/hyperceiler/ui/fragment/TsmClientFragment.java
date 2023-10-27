package com.sevtinge.hyperceiler.ui.fragment;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class TsmClientFragment extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() {
        return R.xml.tsmclient;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.tsmclient),
            "com.miui.tsmclient"
        );
    }
}
