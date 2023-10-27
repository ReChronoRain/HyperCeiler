package com.sevtinge.hyperceiler.ui.fragment;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class MiWallpaperFragment extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() {
        return R.xml.miwallpaper;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartSystemDialog();
    }
}
