package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.various.*;
import com.sevtinge.cemiuiler.module.guardprovider.DisableUploadAppList;

import java.util.Arrays;
import java.util.HashSet;

public class Various extends BaseModule {

    Class<?> mHelpers;

    String mPackageName;
    boolean isMiuiApps;


    @Override
    public void handleLoadPackage() {
        mPackageName = mLoadPackageParam.packageName;
        isMiuiApps = mPackageName.startsWith("com.miui") || mPackageName.startsWith("com.xiaomi") || miuiDialogCustomApps.contains(mPackageName);



        initHook(new MiuiAppNoOverScroll(), isMiuiOverScrollApps());
        initHook(new DialogCustom(), isMiuiDialogCustom());

        initHook(new CollapseMiuiTitle(), isCollapseMiuiTitleApps());

        initHook(UnlockIme.INSTANCE, mPrefsMap.getBoolean("various_unlock_ime"));


    }




    private boolean isMiuiOverScrollApps() {
        return mPrefsMap.getBoolean("various_no_overscroll") && miuiOverScrollApps.contains(mPackageName);
    }

    private boolean isMiuiDialogCustom() {
        return mPrefsMap.getStringAsInt("various_dialog_gravity", 0) != 0 && isMiuiApps;
    }

    private boolean isCollapseMiuiTitleApps() {
        return mPrefsMap.getStringAsInt("various_collapse_miui_title", 0) != 0 && collapseMiuiTitleApps.contains(mPackageName);
    }

    HashSet<String> miuiOverScrollApps = new HashSet<String>(Arrays.asList(
            "com.android.fileexplorer",
            "com.android.providers.downloads.ui",
            "com.android.settings"
    ));

    HashSet<String> miuiDialogCustomApps = new HashSet<String>(Arrays.asList(
            "com.android.fileexplorer",
            "com.android.providers.downloads.ui",
            "com.android.settings"
    ));

    HashSet<String> collapseMiuiTitleApps = new HashSet<String>(Arrays.asList(
            "com.android.fileexplorer",
            "com.android.providers.downloads.ui",
            "com.android.settings"
    ));


}
