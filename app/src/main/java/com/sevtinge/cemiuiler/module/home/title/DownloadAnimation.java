package com.sevtinge.cemiuiler.module.home.title;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class DownloadAnimation extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.miui.home.launcher.common.CpuLevelUtils", "needMamlDownload", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
