package com.sevtinge.cemiuiler.ui.fragment;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

public class LbeFragment extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() {
        return R.xml.lbe_security;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.lbe),
            "com.lbe.security.miui"
        );
    }
}
