package com.sevtinge.cemiuiler.module.systemsettings.aiimage;

import android.content.Context;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockSuperResolution extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "getSrForVideoStatus", Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "getSrForImageStatus", Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "getS2hStatus", Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "isSrForVideoSupport", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "isSrForImageSupport", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "isS2hSupport", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
    }
}
