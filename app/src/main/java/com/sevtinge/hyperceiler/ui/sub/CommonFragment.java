package com.sevtinge.hyperceiler.ui.sub;

import com.sevtinge.hyperceiler.ui.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.ResourcesCompat;

public class CommonFragment extends DashboardFragment {

    @Override
    protected int getPreferenceScreenResId() {
        return ResourcesCompat.getXml(requireContext(), getArguments().getString("contentResId"));
    }
}
