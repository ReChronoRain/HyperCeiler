package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.sogou.Clipboard;

public class SoGou extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(new Clipboard(), mPrefsMap.getBoolean("sogou_xiaomi_clipboard"));
    }
}
