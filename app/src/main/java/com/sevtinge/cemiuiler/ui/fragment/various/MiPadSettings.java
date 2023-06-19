package com.sevtinge.cemiuiler.ui.fragment.various;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

public class MiPadSettings extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() { return R.xml.various_mipad; }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartSystemDialog();
    }
}
