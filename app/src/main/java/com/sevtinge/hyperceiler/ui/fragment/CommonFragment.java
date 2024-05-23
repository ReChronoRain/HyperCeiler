package com.sevtinge.hyperceiler.ui.fragment;

import com.sevtinge.hyperceiler.ui.fragment.settings.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.ResourcesCompat;

public class CommonFragment extends DashboardFragment {

    @Override
    protected int getPreferenceScreenResId() {
        return ResourcesCompat.getXml(requireContext(), getArguments().getString("contentResId"));
    }

    @Override
    protected String getLogTag() {
        return getClass().getSimpleName();
    }
}
