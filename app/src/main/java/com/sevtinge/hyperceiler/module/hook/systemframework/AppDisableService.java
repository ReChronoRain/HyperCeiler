package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;
import java.util.Arrays;

public class AppDisableService extends BaseHook {

    public ArrayList<String> mMiuiCoreApps = new ArrayList<>(Arrays.asList(
        "com.lbe.security.miui",
        "com.miui.securitycenter"
    ));

    @Override
    public void init() {

        findAndHookMethod("com.android.server.pm.PackageManagerServiceImpl", "canBeDisabled", String.class, int.class, new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                boolean canBeDisabled = (boolean) param.getResult();
                if (!canBeDisabled && !mMiuiCoreApps.contains(param.args[0])) {
                    param.setResult(true);
                }
            }
        });
    }
}
