package com.sevtinge.hyperceiler.module.app;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.personalassistant.BlurPersonalAssistant;
import com.sevtinge.hyperceiler.module.hook.personalassistant.BlurPersonalAssistantBackGround;
import com.sevtinge.hyperceiler.module.hook.personalassistant.EnableFoldWidget;

public class PersonalAssistant extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // initHook(new BlurOverlay(), false);
        initHook(new EnableFoldWidget(), mPrefsMap.getBoolean("personal_assistant_fold_widget_enable"));

        if (mPrefsMap.getStringAsInt("personal_assistant_value", 0) == 2 && !isAndroidVersion(30)) {
            initHook(BlurPersonalAssistant.INSTANCE);
        } else if (mPrefsMap.getStringAsInt("personal_assistant_value", 0) == 1 && !isAndroidVersion(30)) {
            initHook(BlurPersonalAssistantBackGround.INSTANCE);
        }
    }

}
