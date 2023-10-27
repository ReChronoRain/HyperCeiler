package com.sevtinge.hyperceiler.module.app;

import static com.sevtinge.hyperceiler.utils.BuildUtils.getBuildType;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.personalassistant.BlurOverlay;
import com.sevtinge.hyperceiler.module.hook.personalassistant.BlurPersonalAssistant;
import com.sevtinge.hyperceiler.module.hook.personalassistant.BlurPersonalAssistantBackGround;
import com.sevtinge.hyperceiler.module.hook.personalassistant.EnableFoldWidget;
import com.sevtinge.hyperceiler.module.hook.personalassistant.WidgetCrack;

public class PersonalAssistant extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);

        initHook(new BlurOverlay(), false);
        initHook(new EnableFoldWidget(), mPrefsMap.getBoolean("personal_assistant_fold_widget_enable"));

        if (mPrefsMap.getStringAsInt("personal_assistant_value", 1) != 1 && !isAndroidR()) {
            initHook(BlurPersonalAssistant.INSTANCE, mPrefsMap.getBoolean("pa_enable"));
        } else {
            initHook(BlurPersonalAssistantBackGround.INSTANCE, mPrefsMap.getBoolean("pa_enable"));
        }

        initHook(new WidgetCrack(), mPrefsMap.getBoolean("various_enable_super_function") && mPrefsMap.getBoolean("personal_assistant_widget_crack") && (getBuildType().equals("debug")));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }

}
