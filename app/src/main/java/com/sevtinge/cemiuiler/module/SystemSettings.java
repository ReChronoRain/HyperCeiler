package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.systemsettings.VoipAssistantController;

public class SystemSettings extends BaseModule {

        @Override
        public void handleLoadPackage() {
            initHook(new VoipAssistantController(), mPrefsMap.getBoolean("system_settings_voip_assistant_controller"));
        }
    }


