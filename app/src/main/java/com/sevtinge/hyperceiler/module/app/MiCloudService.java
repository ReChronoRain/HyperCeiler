package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.cloudservice.CloudList;

public class MiCloudService extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(LoadHostDir.INSTANCE);
        initHook(CloudList.INSTANCE, mPrefsMap.getBoolean("micloud_service_list"));
        initHook(CloseHostDir.INSTANCE);
    }
}
