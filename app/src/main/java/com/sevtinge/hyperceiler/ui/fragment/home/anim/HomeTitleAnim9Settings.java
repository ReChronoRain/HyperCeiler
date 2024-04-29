package com.sevtinge.hyperceiler.ui.fragment.home.anim;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class HomeTitleAnim9Settings extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() {
        return R.xml.home_title_anim_9;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
                getResources().getString(R.string.mihome),
                "com.miui.home"
        );
    }
}
