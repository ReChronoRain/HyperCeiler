package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.various.CollapseMiuiTitle;
import com.sevtinge.hyperceiler.module.hook.various.DialogCustom;
import com.sevtinge.hyperceiler.module.hook.various.MiuiAppNoOverScroll;

import java.util.Arrays;
import java.util.HashSet;

public class SystemVarious extends BaseModule {
    Class<?> mHelpers;
    String mPackageName;
    boolean isMiuiApps;

    @Override
    public void handleLoadPackage() {
        initHook(new MiuiAppNoOverScroll(), isMiuiOverScrollApps());
        initHook(new DialogCustom(), isMiuiDialogCustom());

        initHook(new CollapseMiuiTitle(), isCollapseMiuiTitleApps());

        // initHook(new NoBrightness(), isPay(mPackageName));
    }

    private boolean isPay(String param) {
        return mPrefsMap.getBoolean("various_nobrightness") && checkPay(param);
    }

    private boolean checkPay(String packageParam) {
        switch (packageParam) {
            case "com.tencent.mobileqq", "com.tencent.mm",
                "com.eg.android.AlipayGphone" -> {
                return true;
            }
            default -> {
                return false;
            }
        }
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

    HashSet<String> miuiOverScrollApps = new HashSet<>(Arrays.asList(
        "com.android.fileexplorer",
        "com.android.providers.downloads.ui",
        "com.android.settings"
    ));

    HashSet<String> miuiDialogCustomApps = new HashSet<>(Arrays.asList(
        "com.android.fileexplorer",
        "com.android.providers.downloads.ui",
        "com.android.settings"
    ));

    HashSet<String> collapseMiuiTitleApps = new HashSet<>(Arrays.asList(
        "com.android.fileexplorer",
        "com.android.providers.downloads.ui",
        "com.android.settings"
    ));


}
