package com.sevtinge.hyperceiler.ui.settings;

import android.content.Context;

/**
 * Interface for classes whose instances can provide the availability of the preference.
 */
public interface SelfAvailablePreference {
    /**
     * @return the availability of the preference. Please make sure the availability in managed
     * profile is taken into account.
     */
    boolean isAvailable(Context context);
}