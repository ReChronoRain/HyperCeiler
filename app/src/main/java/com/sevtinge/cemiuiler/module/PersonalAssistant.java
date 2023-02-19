package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.personalassistant.BlurOverlay;
import com.sevtinge.cemiuiler.module.personalassistant.EnableFoldWidget;
import com.sevtinge.cemiuiler.module.personalassistant.WidgetCrack;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class PersonalAssistant extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new BlurOverlay(), false);
        initHook(new EnableFoldWidget(), mPrefsMap.getBoolean("personal_assistant_fold_widget_enable"));

        initHook(new WidgetCrack(), mPrefsMap.getBoolean("personal_assistant_widget_crack"));
    }

    /*public static void handleLoad(LoadPackageParam lpparam) {
        mLoadPackageParam = lpparam;
        initHook(new EnableFoldWidget(), mPrefsMap.getBoolean("home_personal_assistant_enable_fold_widget"));
        initHook(new BlurOverlayHook(), false);
        initHook(new DisplayPadWidget(),false);
    }*/
}
