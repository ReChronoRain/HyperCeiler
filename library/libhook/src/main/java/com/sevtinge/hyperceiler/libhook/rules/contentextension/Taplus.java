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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.contentextension;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class Taplus extends BaseHook {
    private static final String STATE_FRAGMENT = "Taplus.settingsFragment";
    private static final String STATE_FRAGMENT_CONTEXT = "Taplus.settingsFragmentContext";
    private static final String STATE_LISTENING_CONTEXT = "Taplus.listeningContext";

    public boolean mListening = false;

    @Override
    public void init() {
        Context restoredListeningContext = getHotReloadRuntimeState(
            STATE_LISTENING_CONTEXT, Context.class
        );
        if (restoredListeningContext != null) {
            setListening(restoredListeningContext);
        }
        Object restoredFragment = getHotReloadRuntimeState(STATE_FRAGMENT, Object.class);
        Context restoredFragmentContext = getHotReloadRuntimeState(
            STATE_FRAGMENT_CONTEXT, Context.class
        );
        if (restoredFragment != null && restoredFragmentContext != null) {
            registerFragmentObserver(restoredFragment, restoredFragmentContext);
        }

        findAndHookMethod("com.miui.contentextension.setting.fragment.MainSettingsFragment",
            "onCreate", Bundle.class, new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    Context mContext = (Context) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(param.getThisObject(), "mContext");
                    registerFragmentObserver(param.getThisObject(), mContext);
                }
            }
        );

        findAndHookMethod("com.miui.contentextension.setting.fragment.MainSettingsFragment",
            "onDestroy", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    Context mContext = (Context) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(param.getThisObject(), "mContext");
                    ContentObserver contentObserver = (ContentObserver) com.sevtinge.hyperceiler.libhook.base.BaseHook.getAdditionalInstanceField(param.getThisObject(), "taplusListener");
                    if (mContext != null && contentObserver != null) {
                        try {
                            mContext.getContentResolver().unregisterContentObserver(contentObserver);
                        } catch (IllegalArgumentException ignored) {
                            // 已在热重载清理阶段注销。
                        }
                    }
                    putHotReloadRuntimeState(STATE_FRAGMENT, null);
                    putHotReloadRuntimeState(STATE_FRAGMENT_CONTEXT, null);
                }
            }
        );

        findAndHookMethod("com.miui.contentextension.utils.TaplusSettingUtils",
            "setTaplusEnableStatus", Context.class, boolean.class,
            new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    Context mContext = (Context) param.getArgs()[0];
                    boolean z = (boolean) param.getArgs()[1];
                    Settings.System.putInt(
                        mContext.getContentResolver(),
                        "key_enable_taplus", z ? 1 : 0);
                    // XposedLog.e(TAG, "con: " + param.getArgs()[0] + " boo: " + param.getArgs()[1]);
                }
            }
        );

        findAndHookMethod("com.miui.contentextension.utils.TaplusSettingUtils",
            "isTaplusEnable", Context.class,
            new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    Context mContext = (Context) param.getArgs()[0];
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
                                XposedLog.e(TAG, "key_enable_taplus: " + e);
                            }
                        }
                    } catch (Throwable e) {
                        XposedLog.e(TAG, "isTaplusEnable: " + e);
                    }
                    if ((system && !prefer) || (!system && prefer)) {
                        try {
                            sharedPreferences.edit().putBoolean("key_enable_taplus", system).apply();
                        } catch (Throwable e) {
                            XposedLog.e(TAG, "putBoolean: " + e);
                        }
                    }
                    if (!mListening) setListening(mContext);
                    param.setResult(system);
                    // XposedLog.e(TAG, "coo: " + param.getArgs()[0]);
                }

                /*@Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedLog.e(TAG, "after: " + param.getResult());
                }*/
            }
        );
    }

    private void registerFragmentObserver(Object fragment, Context context) {
        if (fragment == null || context == null) return;
        ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                boolean enabled = getTaplus(context);
                com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(
                    fragment, "enablePrefConfig", enabled
                );
            }
        };
        context.getContentResolver().registerContentObserver(
            Settings.System.getUriFor("key_enable_taplus"), false, contentObserver
        );
        registerContentObserverHotReloadCleanup(context.getContentResolver(), contentObserver);
        com.sevtinge.hyperceiler.libhook.base.BaseHook.setAdditionalInstanceField(
            fragment, "taplusListener", contentObserver
        );
        putHotReloadRuntimeState(STATE_FRAGMENT, fragment);
        putHotReloadRuntimeState(STATE_FRAGMENT_CONTEXT, context);
    }

    public void setListening(Context context) {
        if (mListening || context == null) return;
        ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                boolean z;
                z = getTaplus(context);
                com.sevtinge.hyperceiler.libhook.base.BaseHook.callStaticMethod(
                    findClassIfExists("com.miui.contentextension.utils.TaplusSettingUtils"),
                    "setTaplusEnableStatus", context, z);
                /*com.sevtinge.hyperceiler.libhook.base.BaseHook.callStaticMethod(
                    findClassIfExists(
                        "com.miui.contentextension.utils.ContentCatcherUtil"),
                    "switchCatcherConfig", context, z);*/
            }
        };
        context.getContentResolver().registerContentObserver(
            Settings.System.getUriFor("key_enable_taplus"),
            false, contentObserver);
        mListening = true;
        registerContentObserverHotReloadCleanup(context.getContentResolver(), contentObserver);
        putHotReloadRuntimeState(STATE_LISTENING_CONTEXT, context);
        // com.sevtinge.hyperceiler.libhook.base.BaseHook.setAdditionalInstanceField(param.getThisObject(), "taplusListener", contentObserver);
    }

    public boolean getTaplus(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), "key_enable_taplus") == 1;
        } catch (Throwable e) {
            XposedLog.e(TAG, "getTaplus: " + e);
        }
        return false;
    }
}
