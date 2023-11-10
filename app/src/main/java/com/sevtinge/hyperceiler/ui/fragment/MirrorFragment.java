package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class MirrorFragment extends SettingsPreferenceFragment {
    @Override
    public int getContentResId() {
        return R.xml.mirror;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(!isMoreHyperOSVersion(1f) ? R.string.mirror : R.string.mirror_hyperos),
            "com.xiaomi.mirror"
        );
    }
}
