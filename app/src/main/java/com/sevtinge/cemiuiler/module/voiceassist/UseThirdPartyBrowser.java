package com.sevtinge.cemiuiler.module.voiceassist;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import android.content.Intent;
import android.net.Uri;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sevtinge.cemiuiler.module.voiceassist.VoiceAssistDexKit.mVoiceAssistResultMethodsMap;

public class UseThirdPartyBrowser extends BaseHook {

    public void init() {

        Method browserActivityWithIntent = null;

        //XposedBridge.log("Hook到小爱同学进程！");
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(mVoiceAssistResultMethodsMap.get("BrowserActivityWithIntent"));
            for (DexMethodDescriptor descriptor : result) {
                browserActivityWithIntent = descriptor.getMethodInstance(lpparam.classLoader);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        log("com.miui.voiceassist browserActivityWithIntent method is " + browserActivityWithIntent);
        //Class<?> clazz = XposedHelpers.findClass("e.D.L.pa.Wa", lpparam.classLoader);
        XposedBridge.hookMethod(browserActivityWithIntent, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                //XposedBridge.log("0)Hook到Activity启动，开始判断");
                Intent intent = (Intent) param.args[0];
                log(intent.toString());
                try {
                    if (intent.getPackage().equals("com.android.browser")) {
                        log("com.miui.voiceassist get browser intent");
                        log("com.miui.voiceassist get URL " + intent.getDataString());
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

