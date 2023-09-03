package com.sevtinge.cemiuiler.module.hook.home.folder;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class BigFolderIcon extends BaseHook {

    Class<?> mFolderIcon;

    @Override
    public void init() {

        mFolderIcon = findClassIfExists("com.miui.home.launcher.folder.FolderIcon2x2_4");

        findAndHookMethod(mFolderIcon, "getPreviewCount", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(5);
            }
        });
    }
}
