package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.trustservice.*;

public class TrustService extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(new DisableMrm(), mPrefsMap.getBoolean("trustservice_disable_mrm"));
    }
}
