package com.sevtinge.hyperceiler.ui.fragment;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class SoGouFragment extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() {
        return R.xml.sogou_xiaomi;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.sogou_xiaomi),
            "com.sohu.inputmethod.sogou.xiaomi"
        );
    }
}
