package com.sevtinge.hyperceiler.module.hook.systemsettings;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.PropUtils;

public class ShowAutoUIMode extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.settings.utils.SettingsFeatures",
                "shouldShowAutoUIModeSetting", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        boolean result = PropUtils.getProp("persist.miui.auto_ui_enable", false);
                        logE(TAG, "prop: " + result);
                        if (result) {
                            param.setResult(true);
                        }
                    }
                }
        );
    }
}
