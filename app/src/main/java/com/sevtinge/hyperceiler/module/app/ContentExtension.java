package com.sevtinge.hyperceiler.module.app;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.contentextension.DoublePress;
import com.sevtinge.hyperceiler.module.hook.contentextension.HorizontalContentExtension;
import com.sevtinge.hyperceiler.module.hook.contentextension.LinkOpenMode;
import com.sevtinge.hyperceiler.module.hook.contentextension.SuperImage;
import com.sevtinge.hyperceiler.module.hook.contentextension.Taplus;
import com.sevtinge.hyperceiler.module.hook.contentextension.UnlockTaplus;
import com.sevtinge.hyperceiler.module.hook.contentextension.UseThirdPartyBrowser;

public class ContentExtension extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UseThirdPartyBrowser(), mPrefsMap.getBoolean("content_extension_browser"));
        initHook(new DoublePress(), mPrefsMap.getBoolean("content_extension_double_press"));
        initHook(new SuperImage(), mPrefsMap.getBoolean("content_extension_super_image"));
        initHook(new Taplus(), mPrefsMap.getBoolean("security_center_taplus"));
        initHook(new LinkOpenMode());
        initHook(HorizontalContentExtension.INSTANCE, mPrefsMap.getBoolean("content_extension_unlock_taplus_horizontal"));

        if (!isAndroidVersion(30)) {
            initHook(UnlockTaplus.INSTANCE, mPrefsMap.getBoolean("content_extension_unlock_taplus"));
        }
    }
}

