package com.sevtinge.hyperceiler.module.hook.voiceassist;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

public class UseThirdPartyBrowser extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method method = (Method) DexKit.getDexKitBridge("AddWatermark", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("IntentUtils", "permission click No Application can handle your intent")
                        )).singleOrNull();
                return methodData.getMethodInstance(lpparam.classLoader);
            }
        });
        hookMethod(method, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                android.content.Intent intent = (android.content.Intent) param.args[0];
                logI(TAG, lpparam.packageName, intent.toString());

                try {
                    if ("com.android.browser".equals(intent.getPackage())) {
                        logI(TAG, lpparam.packageName, "com.miui.voiceassist get URL " + intent.getDataString());
                        android.net.Uri uri = android.net.Uri.parse(intent.getDataString());
                        android.content.Intent newIntent = new android.content.Intent();
                        newIntent.setAction("android.intent.action.VIEW");
                        newIntent.setData(uri);
                        param.args[0] = newIntent;
                    }
                } catch (Exception e) {
                    logE(TAG, lpparam.packageName, e);
                }
            }
        });
    }
}
