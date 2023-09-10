package com.sevtinge.cemiuiler.module.hook.voiceassist;

import static com.sevtinge.cemiuiler.module.hook.voiceassist.VoiceAssistDexKit.mVoiceAssistResultMethodsMap;

import android.content.Intent;
import android.net.Uri;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class UseThirdPartyBrowser extends BaseHook {

    public void init() {

        Method browserActivityWithIntent = null;

        // XposedBridge.log("Hook到小爱同学进程！");
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(mVoiceAssistResultMethodsMap.get("BrowserActivityWithIntent"));
            for (DexMethodDescriptor descriptor : result) {
                browserActivityWithIntent = descriptor.getMethodInstance(lpparam.classLoader);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        logI("com.miui.voiceassist browserActivityWithIntent method is " + browserActivityWithIntent);
        // Class<?> clazz = XposedHelpers.findClass("e.D.L.pa.Wa", lpparam.classLoader);
        XposedBridge.hookMethod(browserActivityWithIntent, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                // XposedBridge.log("0)Hook到Activity启动，开始判断");
                Intent intent = (Intent) param.args[0];
                logI(intent.toString());
                try {
                    if (intent.getPackage().equals("com.android.browser")) {
                        logI("com.miui.voiceassist get browser intent");
                        logI("com.miui.voiceassist get URL " + intent.getDataString());
                        Uri uri = Uri.parse(intent.getDataString());
                        Intent newIntent = new Intent();
                        newIntent.setAction("android.intent.action.VIEW");
                        newIntent.setData(uri);
                        param.args[0] = newIntent;
                    }
                } catch (Exception e) {
                    logE(e);
                }
            }
        });
    }
}

