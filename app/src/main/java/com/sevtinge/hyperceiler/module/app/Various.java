package com.sevtinge.hyperceiler.module.app;

import android.content.Context;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.various.ClipboardList;
import com.sevtinge.hyperceiler.module.hook.various.CollapseMiuiTitle;
import com.sevtinge.hyperceiler.module.hook.various.DialogCustom;
import com.sevtinge.hyperceiler.module.hook.various.MiuiAppNoOverScroll;
import com.sevtinge.hyperceiler.module.hook.various.UnlockIme;
import com.sevtinge.hyperceiler.utils.XposedUtils;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Various extends BaseModule {
    Class<?> mHelpers;
    String mPackageName;
    boolean isMiuiApps;

    public static List<String> mAppsUsingInputMethod = new ArrayList<>();

    @Override
    public void handleLoadPackage() {
        if (mAppsUsingInputMethod.isEmpty()) {
            mAppsUsingInputMethod = getAppsUsingInputMethod(XposedUtils.findContext());
        }
        mPackageName = mLoadPackageParam.packageName;
        isMiuiApps = mPackageName.startsWith("com.miui") || mPackageName.startsWith("com.xiaomi") || miuiDialogCustomApps.contains(mPackageName);

        initHook(new MiuiAppNoOverScroll(), isMiuiOverScrollApps());
        initHook(new DialogCustom(), isMiuiDialogCustom());

        initHook(new CollapseMiuiTitle(), isCollapseMiuiTitleApps());

        initHook(new UnlockIme(), mPrefsMap.getBoolean("various_unlock_ime") && isInputMethod(mPackageName));

        initHook(new ClipboardList(), mPrefsMap.getBoolean("various_phrase_clipboardlist") && isInputMethod(mPackageName));

        // initHook(new NoBrightness(), isPay(mPackageName));

    }

    private List<String> getAppsUsingInputMethod(Context context) {
        try {
            if (context == null) {
                XposedLogUtils.logE("getAppsUsingInputMethod", "context is null");
                return new ArrayList<>();
            }
            List<String> pkgName = new ArrayList<>();
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> enabledInputMethods = inputMethodManager.getEnabledInputMethodList();
            for (InputMethodInfo inputMethodInfo : enabledInputMethods) {
                pkgName.add(inputMethodInfo.getServiceInfo().packageName);
            }
            return pkgName;
        } catch (Throwable e) {
            XposedLogUtils.logE("getAppsUsingInputMethod", "have e: " + e);
            return new ArrayList<>();
        }
    }

    private boolean isInputMethod(String pkgName) {
        if (mAppsUsingInputMethod.isEmpty()) {
            return false;
        }
        for (String inputMethod : mAppsUsingInputMethod) {
            if (inputMethod.equals(pkgName)) {
                return true;
            }
        }
        return false;
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
