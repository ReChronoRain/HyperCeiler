package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.incallui.AnswerInHeadUp;
import com.sevtinge.hyperceiler.module.hook.incallui.HideCrbt;

public class InCallUi extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new HideCrbt(), mPrefsMap.getBoolean("incallui_hide_crbt"));
        initHook(new AnswerInHeadUp(), mPrefsMap.getBoolean("incallui_answer_in_head_up"));
    }
}


