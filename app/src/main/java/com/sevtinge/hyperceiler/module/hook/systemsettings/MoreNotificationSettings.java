/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemsettings;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.robv.android.xposed.XposedHelpers;

public class MoreNotificationSettings extends BaseHook {

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
                    if ("importance".equals(prefKey) || "badge".equals(prefKey) || "allow_keyguard".equals(prefKey)) {
                        param.args[1] = true;
                    }
                }
            }
        });

        hookAllMethods("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "onClickInfoItem", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Context mContext = (Context)param.args[0];
                Object entry = XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mParent"), "getEntry");
                String id = (String)XposedHelpers.callMethod(XposedHelpers.callMethod(entry, "getChannel"), "getId");
                if ("miscellaneous".equals(id)) return;
                Object notification = XposedHelpers.callMethod(entry, "getSbn");
                Class<?> nuCls = findClassIfExists("com.android.systemui.miui.statusbar.notification.NotificationUtil", lpparam.classLoader);
                if (nuCls != null) {
                    boolean isHybrid = (boolean)XposedHelpers.callStaticMethod(nuCls, "isHybrid", notification);
                    if (isHybrid) return;
                }
                String pkgName = (String)XposedHelpers.callMethod(notification, "getPackageName");
                int user = (int)XposedHelpers.callMethod(notification, "getAppUid");

                Bundle bundle = new Bundle();
                bundle.putString("android.provider.extra.CHANNEL_ID", id);
                bundle.putString("package", pkgName);
                bundle.putInt("uid", user);
                bundle.putString("miui.targetPkg", pkgName);
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(":android:show_fragment", "com.android.settings.notification.ChannelNotificationSettings");
                intent.putExtra(":android:show_fragment_args", bundle);
                intent.setClassName("com.android.settings", "com.android.settings.SubSettings");
                try {
                    XposedHelpers.callMethod(mContext, "startActivityAsUser", intent, android.os.Process.myUserHandle());
                    param.setResult(null);
                    Object ModalController = XposedHelpers.callStaticMethod(findClass("com.android.systemui.Dependency", mContext.getClassLoader()), "get", findClass("com.android.systemui.statusbar.notification.modal.ModalController", mContext.getClassLoader()));
                    XposedHelpers.callMethod(ModalController, "animExitModelCollapsePanels");
                } catch (Throwable ignore) {
                    logE(TAG, lpparam.packageName, "onClickInfoItem hook error", ignore);
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
