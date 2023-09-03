package com.sevtinge.cemiuiler.module.hook.systemframework.corepatch;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;

public class ReturnConstant extends XC_MethodHook {
    private final XSharedPreferences prefs;
    private final String prefsKey;
    private final Object value;

    public ReturnConstant(XSharedPreferences prefs, String prefsKey, Object value) {
        this.prefs = prefs;
        this.prefsKey = prefsKey;
        this.value = value;
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        super.beforeHookedMethod(param);
        prefs.reload();
        if (prefs.getBoolean(prefsKey, true)) {
            param.setResult(value);
        }
    }
}
