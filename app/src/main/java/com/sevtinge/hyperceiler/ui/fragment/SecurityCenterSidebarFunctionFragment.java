package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class SecurityCenterSidebarFunctionFragment extends SettingsPreferenceFragment {
    String mSecurity;

    @Override
    public int getContentResId() {
        return R.xml.security_center_sidebar_function;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        mSecurity = getResources().getString(!isPad() ? R.string.security_center : R.string.security_center_pad);
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            mSecurity,
            "com.miui.securitycenter"
        );
    }
}
