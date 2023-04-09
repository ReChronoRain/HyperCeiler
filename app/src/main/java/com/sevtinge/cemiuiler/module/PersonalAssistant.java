package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.personalassistant.BlurOverlay;
import com.sevtinge.cemiuiler.module.personalassistant.BlurPersonalAssistant;
import com.sevtinge.cemiuiler.module.personalassistant.EnableFoldWidget;
import com.sevtinge.cemiuiler.module.personalassistant.WidgetCrack;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class PersonalAssistant extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new BlurOverlay(), false);
        initHook(new EnableFoldWidget(), mPrefsMap.getBoolean("personal_assistant_fold_widget_enable"));
        initHook(BlurPersonalAssistant.INSTANCE, mPrefsMap.getBoolean("pa_enable"));

        initHook(new WidgetCrack(), mPrefsMap.getBoolean("hidden_function") && mPrefsMap.getBoolean("personal_assistant_widget_crack"));
    }

}
