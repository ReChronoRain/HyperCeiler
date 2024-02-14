package com.sevtinge.hyperceiler.module.hook.systemsettings;

import android.content.ComponentName;
import android.content.Intent;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class RunningServices extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.settings.SettingsActivity",
            "getStartingFragmentClass", Intent.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    Intent intent = (Intent) param.args[0];
                    ComponentName componentName = intent.getComponent();
                    if (componentName != null) {
                        String className = componentName.getClassName();
                        if ("com.android.settings.RunningServices".equals(className)) {
                            param.setResult("com.android.settings.applications.RunningServices");
                        }
                    }
                }
            }
        );
    }
}
