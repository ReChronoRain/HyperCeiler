package com.sevtinge.hyperceiler.module.hook.contentextension;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;


public class UseThirdPartyBrowser extends BaseHook {

    @Override
    public void init() {
        // XposedBridge.log("Hook到传送门进程！");
        final Class<?> clazz = XposedHelpers.findClass("com.miui.contentextension.utils.AppsUtils", lpparam.classLoader);
        // getClassInfo(clazz);

        XposedHelpers.findAndHookMethod(clazz, "getIntentWithBrowser", String.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                XposedLogUtils.logI("com.miui.contentextension hooked url " + param.args[0].toString());
                Uri uri = Uri.parse(param.args[0].toString());
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                intent.setData(uri);
                return intent;
            }
        });

        XposedHelpers.findAndHookMethod(clazz, "openGlobalSearch", Context.class, String.class, String.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                XposedLogUtils.logI("com.miui.contentextension hooked all-search on, word is " + param.args[1].toString() + ", from " + param.args[2].toString());
                try {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_WEB_SEARCH);
                    intent.putExtra(SearchManager.QUERY, param.args[1].toString());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ((Context) param.args[0]).startActivity(intent);
                } catch (Exception e) {
                    XposedLogUtils.logE(TAG, e);
                }
                return null;
            }
        });
    }
}
