package com.sevtinge.cemiuiler.module.settings;

import android.app.NotificationChannel;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.robv.android.xposed.XposedHelpers;

public class NotificationImportance extends BaseHook {

    Class<?> mBaseNotificationSettings;
    Class<?> mChannelNotificationSettings;

    @Override
    public void init() {
        mBaseNotificationSettings = findClassIfExists("com.android.settings.notification.BaseNotificationSettings");
        mChannelNotificationSettings = findClassIfExists("com.android.settings.notification.ChannelNotificationSettings");


        hookAllMethods(mBaseNotificationSettings, "setPrefVisible", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object pref = param.args[0];
                if (pref != null) {
                    String prefKey = (String) XposedHelpers.callMethod(pref, "getKey");
                    if ("importance".equals(prefKey)) {
                        param.args[1] = true;
                    }
                }
            }
        });

        findAndHookMethod(mChannelNotificationSettings, "setupChannelDefaultPrefs", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Object pref = XposedHelpers.callMethod(param.thisObject, "findPreference", "importance");
                XposedHelpers.setObjectField(param.thisObject, "mImportance", pref);
                int mBackupImportance = (int) XposedHelpers.getObjectField(param.thisObject, "mBackupImportance");
                if (mBackupImportance > 0) {
                    int index = (int) XposedHelpers.callMethod(pref, "findSpinnerIndexOfValue", String.valueOf(mBackupImportance));
                    if (index > -1) {
                        XposedHelpers.callMethod(pref, "setValueIndex", index);
                    }
                    Class<?> ImportanceListener = XposedHelpers.findClassIfExists("androidx.preference.Preference$OnPreferenceChangeListener", lpparam.classLoader);
                    InvocationHandler handler = new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if (method.getName().equals("onPreferenceChange")) {
                                int mBackupImportance = Integer.parseInt((String) args[1]);
                                XposedHelpers.setObjectField(param.thisObject, "mBackupImportance", mBackupImportance);
                                NotificationChannel mChannel = (NotificationChannel) XposedHelpers.getObjectField(param.thisObject, "mChannel");
                                mChannel.setImportance(mBackupImportance);
                                XposedHelpers.callMethod(mChannel, "lockFields", 4);
                                Object mBackend = XposedHelpers.getObjectField(param.thisObject, "mBackend");
                                String mPkg = (String) XposedHelpers.getObjectField(param.thisObject, "mPkg");
                                int mUid = (int) XposedHelpers.getObjectField(param.thisObject, "mUid");
                                XposedHelpers.callMethod(mBackend, "updateChannel", mPkg, mUid, mChannel);
                                XposedHelpers.callMethod(param.thisObject, "updateDependents", false);
                            }
                            return true;
                        }
                    };
                    Object mImportanceListener = Proxy.newProxyInstance(
                        lpparam.classLoader,
                        new Class[]{ImportanceListener},
                        handler
                    );
                    XposedHelpers.callMethod(pref, "setOnPreferenceChangeListener", mImportanceListener);
                }
            }
        });
    }
}
