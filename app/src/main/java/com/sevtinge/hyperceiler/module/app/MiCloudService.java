package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.cloudservice.CloudList;

public class MiCloudService extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(CloudList.INSTANCE, mPrefsMap.getBoolean("micloud_service_list"));
    }
}
