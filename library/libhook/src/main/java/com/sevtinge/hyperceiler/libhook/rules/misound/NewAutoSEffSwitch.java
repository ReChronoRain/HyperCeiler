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
package com.sevtinge.hyperceiler.libhook.rules.misound;

import static com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getProp;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.BINDER_KEY_EFFECT_INFO;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.PREFS_KEY_LOCK_SELECTION;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.SETTINGS_KEY_EARPHONE_STATE;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;

import com.sevtinge.hyperceiler.libhook.IEffectInfo;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.DeviceEffectMemory;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 新版连接耳机自动切换原声 - MiSound 端
 *
 * @author 焕晨HChen
 */
public class NewAutoSEffSwitch extends BaseHook {

    private static final String TAG = "NewAutoSEffSwitch";
    private static final String PROP_FW_EFFECT = "ro.vendor.audio.fweffect";

    // 使用原子引用保证线程安全
    private static final AtomicReference<IEffectInfo> sEffectInfoRef = new AtomicReference<>();
    private static final AtomicReference<AudioManager> sAudioManagerRef = new AtomicReference<>();
    private static final AtomicReference<DeviceEffectMemory> sDeviceMemoryRef = new AtomicReference<>();
    private static final AtomicBoolean sLockSelectionEnabled = new AtomicBoolean(true);

    private Context mContext;
    private BaseEffectControlUI mEffectControlUI;

    @Override
    public void init() {
        loadConfig();
        initEffectControlUI();
        initBinderConnection();
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        sLockSelectionEnabled.set(mPrefsMap.getBoolean(PREFS_KEY_LOCK_SELECTION));XposedLog.d(TAG, "Config loaded: lockSelection=" + sLockSelectionEnabled.get());
    }

    /**
     * 初始化音效控制 UI
     */
    private void initEffectControlUI() {
        if (isSupportFW()) {
            mEffectControlUI = new NewFWAudioEffectControl();
            XposedLog.d(TAG, "Using FW AudioEffectControl UI");
        } else {
            mEffectControlUI = new NewAudioEffectControl();
            XposedLog.d(TAG, "Using Non-FW AudioEffectControl UI");
        }
        mEffectControlUI.init();
    }

    /**
     * 初始化 Binder 连接
     */
    private void initBinderConnection() {
        runOnApplicationAttach(this::onApplicationAttach);
    }

    /**
     * Application 附加时的处理
     */
    private void onApplicationAttach(Context context) {
        mContext = context;

        // 连接到 EffectInfoService
        connectToEffectInfoService();

        // 初始化 AudioManager
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        sAudioManagerRef.set(audioManager);

        // 注册状态监听
        registerStateObserver();

        // 如果是 FW 模式，初始化 AudioEffectCenter
        if (isSupportFW()) {
            initAudioEffectCenter();
        }
    }

    /**
     * 连接到 EffectInfoService
     */
    private void connectToEffectInfoService() {
        try {
            Intent intent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (intent == null) {
                XposedLog.w(TAG, "Battery intent is null");
                return;
            }

            Bundle bundle = intent.getBundleExtra(BINDER_KEY_EFFECT_INFO);
            if (bundle == null) {
                XposedLog.w(TAG, "Effect info bundle is null");
                return;
            }

            IBinder binder = bundle.getBinder(BINDER_KEY_EFFECT_INFO);
            if (binder == null) {
                XposedLog.w(TAG, "Binder is null");
                return;
            }

            IEffectInfo effectInfo = IEffectInfo.Stub.asInterface(binder);
            sEffectInfoRef.set(effectInfo);

            // 传递给 UI 控制器
            mEffectControlUI.setEffectInfo(effectInfo);

            XposedLog.d(TAG, "Connected to EffectInfoService: " + effectInfo);
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to connect to EffectInfoService", e);
        }
    }

    /**
     * 注册耳机状态监听
     */
    private void registerStateObserver() {
        mContext.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(SETTINGS_KEY_EARPHONE_STATE),
            false,
            new ContentObserver(new Handler(mContext.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    if (selfChange) return;

                    int state = Settings.Global.getInt(
                        mContext.getContentResolver(),
                        SETTINGS_KEY_EARPHONE_STATE,
                        0
                    );
                    XposedLog.d(TAG, "Earphone state changed: " + state);

                    mEffectControlUI.onEarphoneStateChanged();
                }
            }
        );
    }

    /**
     * 初始化 AudioEffectCenter（FW 模式）
     */
    private void initAudioEffectCenter() {
        try {
            Class<?> centerClass = findClass("android.media.audiofx.AudioEffectCenter");
            callStaticMethod(centerClass, "getInstance", mContext);
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to init AudioEffectCenter", e);
        }
    }

    /**
     * 检查是否支持 FW 模式
     */
    public static boolean isSupportFW() {
        return getProp(PROP_FW_EFFECT, false);
    }

    /**
     * 获取耳机连接状态
     */
    public static boolean getEarPhoneStateFinal() {
        IEffectInfo effectInfo = sEffectInfoRef.get();
        if (effectInfo != null) {
            try {
                return effectInfo.isEarphoneConnection();
            } catch (RemoteException e) {
                XposedLog.e(TAG, "Failed to get earphone state", e);
            }
        }
        return false;
    }

    /**
     * 是否启用锁定选择
     */
    public static boolean isLockSelectionEnabled() {
        return sLockSelectionEnabled.get();
    }

    /**
     * 获取 IEffectInfo 实例
     */
    public static IEffectInfo getEffectInfo() {
        return sEffectInfoRef.get();
    }
}
