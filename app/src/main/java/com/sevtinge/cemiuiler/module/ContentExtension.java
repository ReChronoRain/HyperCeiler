package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.clock.EnableHourGlass;
import com.sevtinge.cemiuiler.module.contentextension.UseThirdPartyBrowser;

public class ContentExtension extends BaseModule {

        @Override
        public void handleLoadPackage() {
            initHook(new UseThirdPartyBrowser(), mPrefsMap.getBoolean("content_extension_browser"));
        }
    }

