package com.sevtinge.hyperceiler.ui.fragment;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class HuanjiFragment extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() {
        return R.xml.huanji;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.huanji),
            "com.miui.huanji"
        );
    }
}
