package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.incallui.HideCrbt;
import com.sevtinge.cemiuiler.module.joyose.DisableCloudControl;

public class InCallUi extends BaseModule {

        @Override
        public void handleLoadPackage() {
            initHook(new HideCrbt(), mPrefsMap.getBoolean("incallui_hide_crbt"));
        }
    }


