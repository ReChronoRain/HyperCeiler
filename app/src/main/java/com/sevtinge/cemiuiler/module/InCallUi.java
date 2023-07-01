package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.incallui.AnswerInHeadUp;
import com.sevtinge.cemiuiler.module.incallui.HideCrbt;

public class InCallUi extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new HideCrbt(), mPrefsMap.getBoolean("incallui_hide_crbt"));
        initHook(new AnswerInHeadUp(), mPrefsMap.getBoolean("incallui_answer_in_head_up"));
    }
}


