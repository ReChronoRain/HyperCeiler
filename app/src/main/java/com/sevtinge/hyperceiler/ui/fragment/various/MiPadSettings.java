package com.sevtinge.hyperceiler.ui.fragment.various;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class MiPadSettings extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() { return R.xml.various_mipad; }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartSystemDialog();
    }
}
