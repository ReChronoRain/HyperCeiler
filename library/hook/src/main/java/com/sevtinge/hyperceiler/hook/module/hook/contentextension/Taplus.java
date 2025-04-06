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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.contentextension;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class Taplus extends BaseHook {
    public boolean mListening = false;

    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.contentextension.setting.fragment.MainSettingsFragment",
            "onCreate", Bundle.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    ContentObserver contentObserver = new ContentObserver(new Handler(mContext.getMainLooper())) {
                        @Override
                        public void onChange(boolean selfChange) {
                            boolean z;
                            z = getTaplus(mContext);
                            XposedHelpers.callMethod(param.thisObject, "enablePrefConfig", z);
                        }
                    };
                    mContext.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor("key_enable_taplus"),
                        false, contentObserver);
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "taplusListener", contentObserver);
                }
            }
        );

        findAndHookMethod("com.miui.contentextension.setting.fragment.MainSettingsFragment",
            "onDestroy", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    ContentObserver contentObserver = (ContentObserver) XposedHelpers.getAdditionalInstanceField(param.thisObject, "taplusListener");
                    mContext.getContentResolver().unregisterContentObserver(contentObserver);
                }
            }
        );

        findAndHookMethod("com.miui.contentextension.utils.TaplusSettingUtils",
            "setTaplusEnableStatus", Context.class, boolean.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    Context mContext = (Context) param.args[0];
                    boolean z = (boolean) param.args[1];
                    Settings.System.putInt(
                        mContext.getContentResolver(),
                        "key_enable_taplus", z ? 1 : 0);
                    // logE(TAG, "con: " + param.args[0] + " boo: " + param.args[1]);
                }
            }
        );

        findAndHookMethod("com.miui.contentextension.utils.TaplusSettingUtils",
            "isTaplusEnable", Context.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    Context mContext = (Context) param.args[0];
                    SharedPreferences sharedPreferences = mContext.getSharedPreferences("pref_com_miui_contentextension", 0);
                    boolean system = false;
                    boolean prefer = false;
                    try {
                        if (mContext != null) {
                            try {
                                system = Settings.System.getInt(
                                    mContext.getContentResolver(),
                                    "key_enable_taplus", 0) == 1;
                                prefer = sharedPreferences.getBoolean("key_enable_taplus", false);
                            } catch (Throwable e) {
                                system = false;
                                prefer = false;
                                logE(TAG, "key_enable_taplus: " + e);
                            }
                        }
                    } catch (Throwable e) {
                        logE(TAG, "isTaplusEnable: " + e);
                    }
                    if ((system && !prefer) || (!system && prefer)) {
                        try {
                            sharedPreferences.edit().putBoolean("key_enable_taplus", system).apply();
                        } catch (Throwable e) {
                            logE(TAG, "putBoolean: " + e);
                        }
                    }
                    if (!mListening) setListening(mContext);
                    param.setResult(system);
                    // logE(TAG, "coo: " + param.args[0]);
                }

                /*@Override
                protected void after(MethodHookParam param) throws Throwable {
                    logE(TAG, "after: " + param.getResult());
                }*/
            }
        );
    }

    public void setListening(Context context) {
        mListening = true;
        ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                boolean z;
                z = getTaplus(context);
                XposedHelpers.callStaticMethod(
                    findClassIfExists("com.miui.contentextension.utils.TaplusSettingUtils"),
                    "setTaplusEnableStatus", context, z);
                /*XposedHelpers.callStaticMethod(
                    findClassIfExists(
                        "com.miui.contentextension.utils.ContentCatcherUtil"),
                    "switchCatcherConfig", context, z);*/
            }
        };
        context.getContentResolver().registerContentObserver(
            Settings.System.getUriFor("key_enable_taplus"),
            false, contentObserver);
        // XposedHelpers.setAdditionalInstanceField(param.thisObject, "taplusListener", contentObserver);
    }

    public boolean getTaplus(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), "key_enable_taplus") == 1;
        } catch (Throwable e) {
            logE(TAG, "getTaplus: " + e);
        }
        return false;
    }
}
