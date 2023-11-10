package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.personalassistant.BlurPersonalAssistant;
import com.sevtinge.hyperceiler.module.hook.personalassistant.BlurPersonalAssistantBackGround;
import com.sevtinge.hyperceiler.module.hook.personalassistant.EnableFoldWidget;

public class PersonalAssistant extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);

        // initHook(new BlurOverlay(), false);
        initHook(new EnableFoldWidget(), mPrefsMap.getBoolean("personal_assistant_fold_widget_enable"));

        if (mPrefsMap.getStringAsInt("personal_assistant_value", 0) == 2) {
            initHook(BlurPersonalAssistant.INSTANCE, mPrefsMap.getBoolean("pa_enable"));
        } else if (mPrefsMap.getStringAsInt("personal_assistant_value", 0) == 1) {
            initHook(BlurPersonalAssistantBackGround.INSTANCE, mPrefsMap.getBoolean("pa_enable"));
        }

        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }

}
