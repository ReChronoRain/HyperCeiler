package com.sevtinge.cemiuiler.module.app;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.contentextension.DoublePress;
import com.sevtinge.cemiuiler.module.hook.contentextension.HorizontalContentExtension;
import com.sevtinge.cemiuiler.module.hook.contentextension.LinkOpenMode;
import com.sevtinge.cemiuiler.module.hook.contentextension.SuperImage;
import com.sevtinge.cemiuiler.module.hook.contentextension.UnlockTaplus;
import com.sevtinge.cemiuiler.module.hook.contentextension.UseThirdPartyBrowser;

public class ContentExtension extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UseThirdPartyBrowser(), mPrefsMap.getBoolean("content_extension_browser"));
        initHook(new DoublePress(), mPrefsMap.getBoolean("content_extension_double_press"));
        initHook(new SuperImage(), mPrefsMap.getBoolean("content_extension_super_image"));
        initHook(new LinkOpenMode());
        initHook(HorizontalContentExtension.INSTANCE, mPrefsMap.getBoolean("content_extension_unlock_taplus_horizontal"));

        if (!isAndroidR()){
            initHook(UnlockTaplus.INSTANCE, mPrefsMap.getBoolean("content_extension_unlock_taplus"));
        }
    }
}

