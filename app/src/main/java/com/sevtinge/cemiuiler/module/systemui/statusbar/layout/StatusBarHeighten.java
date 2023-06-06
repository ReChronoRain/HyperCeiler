package com.sevtinge.cemiuiler.module.systemui.statusbar.layout;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class StatusBarHeighten extends BaseHook {
    @Override
    public void init() {
        int opt = mPrefsMap.getInt("system_ui_statusbar_height", 19);

        int heightDpi = opt == 19 ? 27 : opt;
        mResHook.setDensityReplacement("*", "dimen", "status_bar_height_default", heightDpi);
        mResHook.setDensityReplacement("*", "dimen", "status_bar_height", heightDpi);
        mResHook.setDensityReplacement("*", "dimen", "status_bar_height_portrait", heightDpi);
        mResHook.setDensityReplacement("*", "dimen", "status_bar_height_landscape", heightDpi);
    }
}
