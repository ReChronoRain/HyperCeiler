/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.sevtinge.hyperceiler.hooker.home;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

/** HyperOS 4 launcher animation controls, applied through the launcher's native ratio hook. */
public class HomeAnimationSettings extends DashboardFragment {
    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_animation;
    }
}
