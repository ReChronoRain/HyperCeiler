package com.sevtinge.hyperceiler.ui.fragment.securitycenter;

import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public abstract class SecurityCenterBaseSettings extends SettingsPreferenceFragment  {

    String mSecurity;
}
