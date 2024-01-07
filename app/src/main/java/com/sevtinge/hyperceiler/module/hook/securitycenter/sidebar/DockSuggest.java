package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar;

import android.util.SparseArray;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

public class DockSuggest extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        MethodData methodData = DexKit.INSTANCE.getDexKitBridge().findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("supportFreeform")
            )
        ).singleOrThrow(() -> new RuntimeException("Method not found"));
        Method method = methodData.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "Current hooking method is " + method);
        hookMethod(method, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (mPrefsMap.getStringAsInt("security_center_sidebar_show_suggest", 0) == 1) {
                    param.setResult(false);
                } else if (mPrefsMap.getStringAsInt("security_center_sidebar_show_suggest", 0) == 2) {
                    param.setResult(true);
                }
            }
        });
    }
}
