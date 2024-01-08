package com.sevtinge.hyperceiler.module.app;

import android.content.Context;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.sogou.Clipboard;
import com.sevtinge.hyperceiler.module.hook.various.ClipboardList;
import com.sevtinge.hyperceiler.module.hook.various.UnlockIme;
import com.sevtinge.hyperceiler.utils.XposedUtils;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import java.util.ArrayList;
import java.util.List;

public class Various extends BaseModule {
    String mPackageName;

    public static List<String> mAppsUsingInputMethod = new ArrayList<>();

    @Override
    public void handleLoadPackage() {
        if (mAppsUsingInputMethod.isEmpty()) {
            mAppsUsingInputMethod = getAppsUsingInputMethod(XposedUtils.findContext(XposedUtils.FlAG_ONLY_ANDROID));
        }
        mPackageName = mLoadPackageParam.packageName;
        initHook(new UnlockIme(), mPrefsMap.getBoolean("various_unlock_ime") && isInputMethod(mPackageName));
        initHook(new Clipboard(), mPrefsMap.getBoolean("sogou_xiaomi_clipboard") &&
            ("com.sohu.inputmethod.sogou.xiaomi".equals(mPackageName) || "com.sohu.inputmethod.sogou".equals(mPackageName)));
        initHook(new ClipboardList(), mPrefsMap.getBoolean("various_phrase_clipboardlist") && isInputMethod(mPackageName));
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
}
