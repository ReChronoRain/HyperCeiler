package com.sevtinge.hyperceiler.module.hook.home.folder;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class BigFolderIcon extends BaseHook {

    Class<?> mFolderIcon;

    @Override
    public void init() {

        mFolderIcon = findClassIfExists("com.miui.home.launcher.folder.FolderIcon2x2_4");

        findAndHookMethod(mFolderIcon, "getPreviewCount", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(5);
            }
        });
    }
}
