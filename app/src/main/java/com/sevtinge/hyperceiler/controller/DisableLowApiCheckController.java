package com.sevtinge.hyperceiler.controller;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.Context;

import com.sevtinge.hyperceiler.ui.settings.core.BasePreferenceController;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

public class DisableLowApiCheckController extends BasePreferenceController {

    public DisableLowApiCheckController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return isMoreAndroidVersion(34) ? AVAILABLE :
                UNSUPPORTED_ON_DEVICE;
    }
}
