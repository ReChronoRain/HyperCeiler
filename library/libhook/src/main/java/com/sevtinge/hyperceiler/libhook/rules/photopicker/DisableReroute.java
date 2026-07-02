package com.sevtinge.hyperceiler.libhook.rules.photopicker;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class DisableReroute extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("android.provider.DeviceConfig", "getBoolean", String.class, String.class, boolean.class, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                String namespace = (String) param.getArgs()[0];
                String prop = (String) param.getArgs()[1];
                if ("securitycenter".equals(namespace) && ("hyper_refer_file_picker".equals(prop))) {
                    param.setResult(false);
                }
            }
        });
    }
}
