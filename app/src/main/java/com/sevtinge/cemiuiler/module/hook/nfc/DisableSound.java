package com.sevtinge.cemiuiler.module.hook.nfc;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class DisableSound extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.nfc.NfcService",
            "initSoundPool", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(null);
                }
            }
        );
    }
}
