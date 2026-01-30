package com.sevtinge.hyperceiler.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.aiasst.NewAiCaptions;
import com.sevtinge.hyperceiler.libhook.rules.aiasst.UnlockAllCaptions;
import com.sevtinge.hyperceiler.libhook.rules.aiasst.UnlockSplitTranslation;

@HookBase(targetPackage = "com.xiaomi.aiasst.vision")
public class AiAsst extends BaseLoad {

    public AiAsst() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {
        initHook(NewAiCaptions.INSTANCE, mPrefsMap.getBoolean("aiasst_ai_captions"));
        initHook(UnlockAllCaptions.INSTANCE, mPrefsMap.getBoolean("aiasst_all_captions"));
        initHook(UnlockSplitTranslation.INSTANCE, mPrefsMap.getBoolean("aiasst_unlock_split_screen_translation"));
    }
}
