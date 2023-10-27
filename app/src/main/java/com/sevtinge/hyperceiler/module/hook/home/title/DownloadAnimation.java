package com.sevtinge.hyperceiler.module.hook.home.title;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DownloadAnimation extends BaseHook {
    @Override
    public void init() {
        try{
            hookAllMethods("com.miui.home.launcher.common.DeviceLevelUtils", "needMamlProgressIcon", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
            hookAllMethods("com.miui.home.launcher.common.DeviceLevelUtils", "needRemoveDownloadAnimationDevice", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        } catch (Exception e) {
            hookAllMethods("com.miui.home.launcher.common.CpuLevelUtils", "needMamlDownload", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        }
    }
}
