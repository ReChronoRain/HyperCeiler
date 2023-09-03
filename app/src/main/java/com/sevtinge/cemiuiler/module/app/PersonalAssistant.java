package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.personalassistant.BlurOverlay;
import com.sevtinge.cemiuiler.module.hook.personalassistant.BlurPersonalAssistant;
import com.sevtinge.cemiuiler.module.hook.personalassistant.EnableFoldWidget;
import com.sevtinge.cemiuiler.module.hook.personalassistant.PersonalAssistantDexKit;
import com.sevtinge.cemiuiler.module.hook.personalassistant.WidgetCrack;

public class PersonalAssistant extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new PersonalAssistantDexKit());
        initHook(new BlurOverlay(), false);
        initHook(new EnableFoldWidget(), mPrefsMap.getBoolean("personal_assistant_fold_widget_enable"));
        initHook(BlurPersonalAssistant.INSTANCE, mPrefsMap.getBoolean("pa_enable"));

        initHook(new WidgetCrack(), mPrefsMap.getBoolean("hidden_function") && mPrefsMap.getBoolean("personal_assistant_widget_crack"));
    }

}
