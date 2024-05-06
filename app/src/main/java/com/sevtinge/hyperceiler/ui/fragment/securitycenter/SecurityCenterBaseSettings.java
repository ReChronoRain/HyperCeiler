package com.sevtinge.hyperceiler.ui.fragment.securitycenter;

import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public abstract class SecurityCenterBaseSettings extends SettingsPreferenceFragment  {

    String mSecurity;

    @Override
    public View.OnClickListener addRestartListener() {
        mSecurity = getResources().getString(!isMoreHyperOSVersion(1f) ? (!isPad() ? R.string.security_center : R.string.security_center_pad) : R.string.security_center_hyperos);
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
                mSecurity,
                "com.miui.securitycenter"
        );
    }
}
