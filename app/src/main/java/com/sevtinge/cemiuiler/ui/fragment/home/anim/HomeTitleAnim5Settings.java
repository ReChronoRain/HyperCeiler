package com.sevtinge.cemiuiler.ui.fragment.home.anim;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

public class HomeTitleAnim5Settings extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() {
        return R.xml.home_title_anim_5;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.home),
            "com.miui.home"
        );
    }
}
