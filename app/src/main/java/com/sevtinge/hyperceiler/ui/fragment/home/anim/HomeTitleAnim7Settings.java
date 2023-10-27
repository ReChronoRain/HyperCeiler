package com.sevtinge.hyperceiler.ui.fragment.home.anim;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class HomeTitleAnim7Settings extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() {
        return R.xml.home_title_anim_7;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.home),
            "com.miui.home"
        );
    }
}
