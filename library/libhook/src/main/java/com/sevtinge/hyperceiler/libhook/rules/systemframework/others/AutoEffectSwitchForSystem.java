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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import static com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getProp;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.DELAY_DUMP;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.DELAY_STATE_CHANGE;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.DEVICE_TYPE_BLUETOOTH_A2DP;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.DEVICE_TYPE_BLUETOOTH_LE;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.DEVICE_TYPE_USB;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.DEVICE_TYPE_WIRED;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_DOLBY;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_KEEP;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_MISOUND;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_NONE;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.MSG_DUMP;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.MSG_STATE_CHANGE;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.PREFS_KEY_DEFAULT_EFFECT;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.PREFS_KEY_LOCK_SELECTION;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.PREFS_KEY_REMEMBER_DEVICE;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.SETTINGS_KEY_EARPHONE_STATE;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.SETTINGS_KEY_RESTORE_STATE;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudio;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.DeviceEffectMemory;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.DeviceEffectMemory.EffectState;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.binder.EffectInfoService;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.callback.IControlForSystem;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.control.AudioEffectControlForSystem;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.control.FWAudioEffectControlForSystem;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

/**
 * 自动切换音效 - 系统框架端 - 总控制端
 * 监听耳机连接状态，自动切换音效
 *
 * @author 焕晨HChen
 */
public class AutoEffectSwitchForSystem extends BaseHook {

    private static final String TAG = "AutoEffectSwitchForSystem";
    private static final String PROP_FW_EFFECT = "ro.vendor.audio.fweffect";
    private static final String SETTINGS_KEY_LOCK_SELECTION = "misound_bluetooth_effect_lock_selection";
    private static final String SETTINGS_KEY_REMEMBER_DEVICE = "misound_bluetooth_effect_remember_device";
    private static final String SETTINGS_KEY_DEFAULT_EFFECT = "misound_bluetooth_effect_default_effect";

    // 耳机连接类型
    private static final int[] EARPHONE_DEVICE_TYPES = {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET
    };

    // 使用原子类型保证线程安全
    private static final AtomicBoolean sIsEarphoneConnection = new AtomicBoolean(false);
    private static final AtomicBoolean sShouldWaiting = new AtomicBoolean(false);
    private static final AtomicBoolean sIsLeAudioConnected = new AtomicBoolean(false);
    private static final AtomicReference<AudioManager> sAudioManagerRef = new AtomicReference<>();
    private static final AtomicReference<Handler> sHandlerRef = new AtomicReference<>();
    private static final AtomicReference<String> sCurrentDeviceIdRef = new AtomicReference<>();
    private static final AtomicReference<BluetoothDevice> sCurrentBluetoothDeviceRef = new AtomicReference<>();

    // 实例变量
    private Context mContext;
    private IControlForSystem mEffectController;
    private DeviceEffectMemory mDeviceEffectMemory;
    public static EffectInfoService mEffectInfoService;

    // 配置项 - 使用 volatile 保证可见性
    private volatile boolean mLockSelection = true;
    private volatile boolean mRememberDevice = false;
    private volatile String mDefaultEffect = EFFECT_NONE;

    @Override
    public void init() {
        loadConfigFromPrefs();
        initEffectController();
        hookAudioServiceOnSystemReady();
    }

    /**
     * 从 Prefs 加载初始配置
     */
    private void loadConfigFromPrefs() {
        mLockSelection = mPrefsMap.getBoolean(PREFS_KEY_LOCK_SELECTION);
        mRememberDevice = mPrefsMap.getBoolean(PREFS_KEY_REMEMBER_DEVICE);
        mDefaultEffect = mPrefsMap.getString(PREFS_KEY_DEFAULT_EFFECT, EFFECT_NONE);

        XposedLog.d(TAG, "Initial config loaded from prefs: lockSelection=" + mLockSelection +
            ", rememberDevice=" + mRememberDevice +
            ", defaultEffect=" + mDefaultEffect);
    }

    /**
     * 从 Settings 实时读取配置
     */
    private void reloadConfigFromSettings() {
        if (mContext == null) return;

        try {
            int lockSelection = Settings.Global.getInt(
                mContext.getContentResolver(),
                SETTINGS_KEY_LOCK_SELECTION,
                mLockSelection ? 1 : 0
            );
            mLockSelection = lockSelection == 1;

            int rememberDevice = Settings.Global.getInt(
                mContext.getContentResolver(),
                SETTINGS_KEY_REMEMBER_DEVICE,
                mRememberDevice ? 1 : 0
            );
            mRememberDevice = rememberDevice == 1;

            String defaultEffect = Settings.Global.getString(
                mContext.getContentResolver(),
                SETTINGS_KEY_DEFAULT_EFFECT
            );
            if (defaultEffect != null && !defaultEffect.isEmpty()) {
                mDefaultEffect = defaultEffect;
            }

            XposedLog.d(TAG, "Config reloaded from settings: lockSelection=" + mLockSelection +
                ", rememberDevice=" + mRememberDevice +
                ", defaultEffect=" + mDefaultEffect);
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to reload config from settings", e);
        }
    }

    /**
     * 将配置同步到 Settings（供其他进程读取）
     */
    private void syncConfigToSettings() {
        if (mContext == null) return;

        try {
            Settings.Global.putInt(
                mContext.getContentResolver(),
                SETTINGS_KEY_LOCK_SELECTION,
                mLockSelection ? 1 : 0
            );
            Settings.Global.putInt(
                mContext.getContentResolver(),
                SETTINGS_KEY_REMEMBER_DEVICE,
                mRememberDevice ? 1 : 0
            );
            Settings.Global.putString(
                mContext.getContentResolver(),
                SETTINGS_KEY_DEFAULT_EFFECT,
                mDefaultEffect
            );XposedLog.d(TAG, "Config synced to settings");
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to sync config to settings", e);
        }
    }

    /**
     * 初始化音效控制器
     */
    private void initEffectController() {
        if (isSupportFW()) {
            FWAudioEffectControlForSystem fwController = new FWAudioEffectControlForSystem();
            fwController.init();
            mEffectController = fwController;
            mEffectInfoService = new EffectInfoService(fwController);
            XposedLog.d(TAG, "Using FW AudioEffectControl, EffectInfoService created");
        } else {
            AudioEffectControlForSystem controller = new AudioEffectControlForSystem();
            controller.init();
            mEffectController = controller;
            mEffectInfoService = new EffectInfoService(controller);
            XposedLog.d(TAG, "Using Non-FW AudioEffectControl, EffectInfoService created");
        }
    }

    /**
     * Hook AudioService.onSystemReady 方法
     */
    private void hookAudioServiceOnSystemReady() {
        findAndHookMethod("com.android.server.audio.AudioService",
            "onSystemReady",
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                    if (mContext == null) {
                        XposedLog.e(TAG, "Failed to get context from AudioService");
                        return;
                    }

                    onAudioSystemReady();
                }
            }
        );
    }

    /**
     * 音频系统就绪后的初始化
     */
    private void onAudioSystemReady() {
        mEffectController.setContext(mContext);

        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        sAudioManagerRef.set(audioManager);

        // 同步初始配置到 Settings
        syncConfigToSettings();

        // 初始化设备记忆
        if (mRememberDevice) {
            mDeviceEffectMemory = new DeviceEffectMemory(mContext);
            XposedLog.d(TAG, "DeviceEffectMemory initialized");
        }

        initHandler();
        registerEarphoneReceiver();
        registerDebugObserver();
        registerConfigObserver();
        reportEarphoneState();

        XposedLog.d(TAG, "Audio system ready, initialization completed");
    }

    /**
     * 注册配置变化监听
     */
    private void registerConfigObserver() {
        if (mContext == null) return;

        ContentObserver configObserver = new ContentObserver(new Handler(mContext.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                if (selfChange) return;
                reloadConfigFromSettings();

                // 如果启用了设备记忆但还没初始化，则初始化
                if (mRememberDevice && mDeviceEffectMemory == null) {
                    mDeviceEffectMemory = new DeviceEffectMemory(mContext);
                    XposedLog.d(TAG, "DeviceEffectMemory initialized after config change");
                }
            }
        };

        // 监听所有配置项
        mContext.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(SETTINGS_KEY_LOCK_SELECTION),
            false,
            configObserver
        );mContext.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(SETTINGS_KEY_REMEMBER_DEVICE),
            false,
            configObserver
        );
        mContext.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(SETTINGS_KEY_DEFAULT_EFFECT),
            false,
            configObserver
        );

        XposedLog.d(TAG, "Config observer registered");
    }

    /**
     * 检查是否支持 FW 模式
     */
    public static boolean isSupportFW() {
        return getProp(PROP_FW_EFFECT, false);
    }

    /**
     * 获取耳机连接状态（最终判断）
     */
    public static boolean getEarPhoneStateFinal() {
        if (sIsEarphoneConnection.get()) {
            return true;
        }

        AudioManager audioManager = sAudioManagerRef.get();
        if (audioManager != null && isEarphoneDeviceConnected(audioManager)) {
            XposedLog.d(TAG, "Earphone detected via AudioManager");
            startWaitingTimer();
            return true;
        }

        if (sShouldWaiting.get()) {
            XposedLog.d(TAG, "In waiting state");
            return true;
        }

        return false;
    }

    /**
     * 是否锁定音效选择
     */
    public static boolean isLockSelection() {
        return sIsEarphoneConnection.get();
    }

    /**
     * 检查是否有耳机设备连接
     */
    private static boolean isEarphoneDeviceConnected(AudioManager audioManager) {
        AudioDeviceInfo[] outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo info : outputs) {
            for (int type : EARPHONE_DEVICE_TYPES) {
                if (info.getType() == type) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 启动等待计时器
     */
    private static void startWaitingTimer() {
        sShouldWaiting.set(true);
        Handler handler = sHandlerRef.get();
        if (handler != null) {
            handler.removeMessages(MSG_STATE_CHANGE);
            handler.sendEmptyMessageDelayed(MSG_STATE_CHANGE, DELAY_STATE_CHANGE);
        }
    }

    /**
     * 初始化 Handler
     */
    private void initHandler() {
        if (mContext == null) return;

        Handler handler = new Handler(mContext.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == MSG_STATE_CHANGE) {
                    sShouldWaiting.set(false);
                    reportEarphoneState();
                }
            }
        };
        sHandlerRef.set(handler);
    }

    /**
     * 注册调试用的 ContentObserver
     */
    private void registerDebugObserver() {
        if (mContext == null) return;

        Settings.Global.putInt(mContext.getContentResolver(), SETTINGS_KEY_RESTORE_STATE, 0);

        mContext.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(SETTINGS_KEY_RESTORE_STATE),
            false,
            new ContentObserver(new Handler(mContext.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    if (selfChange) return;

                    int value = Settings.Global.getInt(
                        mContext.getContentResolver(),
                        SETTINGS_KEY_RESTORE_STATE,
                        0
                    );

                    if (value == 1) {
                        sIsEarphoneConnection.set(false);
                        Settings.Global.putInt(
                            mContext.getContentResolver(),
                            SETTINGS_KEY_RESTORE_STATE,
                            0
                        );
                        XposedLog.d(TAG, "Earphone state manually restored to false");
                    }
                }
            }
        );
    }

    /**
     * 报告耳机状态到系统设置
     */
    private void reportEarphoneState() {
        if (mContext == null) return;

        try {
            Settings.Global.putInt(
                mContext.getContentResolver(),
                SETTINGS_KEY_EARPHONE_STATE,
                sIsEarphoneConnection.get() ? 1 : 0
            );
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to report earphone state", e);
        }
    }

    /**
     * 注册耳机广播接收器
     */
    private void registerEarphoneReceiver() {
        if (mContext == null) return;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);

        mContext.registerReceiver(
            new EarphoneBroadcastReceiver(this),
            intentFilter
        );

        XposedLog.d(TAG, "Earphone broadcast receiver registered");
    }

    // ==================== 设备连接/断开处理 ====================

    /**
     * 处理设备连接
     */
    void onDeviceConnected(String deviceId, String deviceName, String deviceType) {
        XposedLog.d(TAG, "Device connected: id=" + deviceId + ", name=" + deviceName + ", type=" + deviceType);

        // 每次连接时重新读取配置
        reloadConfigFromSettings();

        sIsEarphoneConnection.set(true);
        sCurrentDeviceIdRef.set(deviceId);

        // 保存当前扬声器的音效状态
        if (mRememberDevice) {
            // 确保 DeviceEffectMemory 已初始化
            if (mDeviceEffectMemory == null) {
                mDeviceEffectMemory = new DeviceEffectMemory(mContext);
            }
            EffectState speakerState = mEffectController.getCurrentEffectState();
            if (speakerState != null) {
                mDeviceEffectMemory.saveSpeakerEffect(speakerState);
            }
        }

        // 更新上一次状态（用于简单恢复模式）
        mEffectController.updateLastEffectState();

        // 应用音效
        applyEffectForDevice(deviceId, deviceName);

        reportEarphoneState();
        scheduleDump();
    }

    /**
     * 处理设备断开
     */
    void onDeviceDisconnected(String deviceId) {
        XposedLog.d(TAG, "Device disconnected: id=" + deviceId);

        // 每次断开时重新读取配置
        reloadConfigFromSettings();

        // 如果启用了设备记忆，保存当前设备的音效状态
        if (mRememberDevice && deviceId != null && mDeviceEffectMemory != null) {
            EffectState currentState = mEffectController.getCurrentEffectState();
            if (currentState != null) {
                currentState.deviceName = getDeviceName(deviceId);
                mDeviceEffectMemory.saveDeviceEffect(deviceId, currentState);
                XposedLog.d(TAG, "Saved effect state for device: " + deviceId);
            }
        }

        sIsEarphoneConnection.set(false);
        sCurrentDeviceIdRef.set(null);
        sCurrentBluetoothDeviceRef.set(null);

        // 恢复音效
        restoreEffectForSpeaker();

        reportEarphoneState();
        scheduleDump();
    }

    /**
     * 为设备应用音效
     */
    private void applyEffectForDevice(String deviceId, String deviceName) {
        // 如果启用了设备记忆，尝试获取该设备的音效设置
        if (mRememberDevice && mDeviceEffectMemory != null) {
            EffectState savedState = mDeviceEffectMemory.getDeviceEffect(deviceId);
            if (savedState != null) {
                XposedLog.d(TAG, "Applying saved effect for device: " + deviceId + " -> " + savedState);
                mEffectController.applyEffectState(savedState);
                return;
            }
        }

        // 没有记忆或未启用记忆，使用默认行为
        applyDefaultEffect();
    }

    /**
     * 应用默认音效
     */
    private void applyDefaultEffect() {
        XposedLog.d(TAG, "Applying default effect: " + mDefaultEffect);

        switch (mDefaultEffect) {
            case EFFECT_NONE -> {
                mEffectController.setEffectToNone(mContext);
                XposedLog.d(TAG, "Applied default effect: none");
            }
            case EFFECT_DOLBY -> {
                mEffectController.applyEffectState(new EffectState(EFFECT_DOLBY, false, false));
                XposedLog.d(TAG, "Applied default effect: dolby");
            }
            case EFFECT_MISOUND -> {
                mEffectController.applyEffectState(new EffectState(EFFECT_MISOUND, false, false));
                XposedLog.d(TAG, "Applied default effect: misound");
            }
            case EFFECT_KEEP -> {
                // 保持当前音效，不做任何改变
                XposedLog.d(TAG, "Applied default effect: keep current");
            }
            default -> {
                mEffectController.setEffectToNone(mContext);
                XposedLog.d(TAG, "Applied default effect: none (fallback)");
            }
        }
    }

    /**
     * 恢复扬声器音效
     */
    private void restoreEffectForSpeaker() {
        // 如果启用了设备记忆，尝试恢复扬声器的音效设置
        if (mRememberDevice && mDeviceEffectMemory != null) {
            EffectState speakerState = mDeviceEffectMemory.getSpeakerEffect();
            if (speakerState != null) {
                XposedLog.d(TAG, "Restoring speaker effect: " + speakerState);
                mEffectController.applyEffectState(speakerState);
                return;
            }
        }

        // 没有记忆或未启用记忆，使用简单恢复
        mEffectController.resetAudioEffect();
    }

    /**
     * 获取设备名称
     */
    private String getDeviceName(String deviceId) {
        if (deviceId == null) return "Unknown";

        BluetoothDevice btDevice = sCurrentBluetoothDeviceRef.get();
        if (btDevice != null) {
            String name = DeviceEffectMemory.getBluetoothDeviceName(btDevice);
            if (name != null) return name;
        }

        if (DEVICE_TYPE_WIRED.equals(deviceId)) return "Wired Headset";
        if (DEVICE_TYPE_USB.equals(deviceId)) return "USB Headset";

        return deviceId;
    }

    /**
     * 延迟输出状态
     */
    private void scheduleDump() {
        Handler handler = sHandlerRef.get();
        if (handler != null) {
            handler.removeMessages(MSG_DUMP);
            handler.sendEmptyMessageDelayed(MSG_DUMP, DELAY_DUMP);
        }
    }

    /**
     * 初始化设备记忆（需要在 MiSound 进程中调用）
     */
    public void initDeviceMemory(Context context) {
        if (mDeviceEffectMemory == null) {
            mDeviceEffectMemory = new DeviceEffectMemory(context);
            XposedLog.d(TAG, "DeviceEffectMemory initialized");
        }
    }

    /**
     * 获取设备记忆实例
     */
    public DeviceEffectMemory getDeviceEffectMemory() {
        return mDeviceEffectMemory;
    }

    /**
     * 获取当前连接的设备 ID
     */
    public static String getCurrentDeviceId() {
        return sCurrentDeviceIdRef.get();
    }

    /**
     * 获取音效控制器
     */
    public IControlForSystem getEffectController() {
        return mEffectController;
    }

    /**
     * 是否启用锁定选择
     */
    public boolean isLockSelectionEnabled() {
        return mLockSelection;
    }

    /**
     * 是否启用设备记忆
     */
    public boolean isRememberDeviceEnabled() {
        return mRememberDevice;
    }

    // ==================== 耳机广播接收器 ====================

    /**
     * 耳机广播接收器
     */
    private static class EarphoneBroadcastReceiver extends BroadcastReceiver {

        private final AutoEffectSwitchForSystem mHost;
        private Handler mDumpHandler;

        EarphoneBroadcastReceiver(AutoEffectSwitchForSystem host) {
            this.mHost = host;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDumpHandler == null) {
                mDumpHandler = new DumpHandler(context.getMainLooper(), mHost.mEffectController);
            }

            String action = intent.getAction();
            if (action == null) return;

            XposedLog.d(TAG, "Received broadcast: " + action);

            switch (action) {
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ->
                    handleA2dpStateChange(intent);
                case BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED ->
                    handleLeAudioStateChange(intent);
                case AudioManager.ACTION_HEADSET_PLUG ->
                    handleHeadsetPlug(intent);
            }
        }

        private void handleA2dpStateChange(Intent intent) {
            int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            switch (state) {
                case BluetoothA2dp.STATE_CONNECTED -> {
                    sCurrentBluetoothDeviceRef.set(device);
                    String deviceId = DeviceEffectMemory.generateBluetoothDeviceId(device);
                    String deviceName = DeviceEffectMemory.getBluetoothDeviceName(device);
                    if (deviceName == null) deviceName = "Bluetooth A2DP";
                    mHost.onDeviceConnected(deviceId, deviceName, DEVICE_TYPE_BLUETOOTH_A2DP);
                }
                case BluetoothA2dp.STATE_DISCONNECTED -> {
                    String deviceId = DeviceEffectMemory.generateBluetoothDeviceId(device);
                    mHost.onDeviceDisconnected(deviceId);
                }
            }
        }

        private void handleLeAudioStateChange(Intent intent) {
            int state = intent.getIntExtra(BluetoothLeAudio.EXTRA_STATE, BluetoothLeAudio.STATE_DISCONNECTED);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            switch (state) {
                case BluetoothLeAudio.STATE_CONNECTED -> {
                    if (!sIsLeAudioConnected.getAndSet(true)) {
                        sCurrentBluetoothDeviceRef.set(device);
                        String deviceId = DeviceEffectMemory.generateBluetoothDeviceId(device);
                        String deviceName = DeviceEffectMemory.getBluetoothDeviceName(device);
                        if (deviceName == null) deviceName = "Bluetooth LE Audio";
                        mHost.onDeviceConnected(deviceId, deviceName, DEVICE_TYPE_BLUETOOTH_LE);
                    }
                }
                case BluetoothLeAudio.STATE_DISCONNECTED -> {
                    if (sIsLeAudioConnected.getAndSet(false)) {
                        String deviceId = DeviceEffectMemory.generateBluetoothDeviceId(device);
                        mHost.onDeviceDisconnected(deviceId);
                    }
                }
            }
        }

        private void handleHeadsetPlug(Intent intent) {
            int state = intent.getIntExtra("state", 0);
            int microphone = intent.getIntExtra("microphone", 0);
            String name = intent.getStringExtra("name");

            switch (state) {
                case 1 -> {
                    String deviceId = DeviceEffectMemory.generateWiredDeviceId();
                    String deviceName = name != null ? name : "Wired Headset";
                    mHost.onDeviceConnected(deviceId, deviceName, DEVICE_TYPE_WIRED);
                }
                case 0 -> {
                    String deviceId = DeviceEffectMemory.generateWiredDeviceId();
                    mHost.onDeviceDisconnected(deviceId);
                }
            }
        }
    }

    /**
     * 用于延迟输出音效状态的 Handler
     */
    private static class DumpHandler extends Handler {

        private final IControlForSystem mController;

        DumpHandler(Looper looper, IControlForSystem controller) {
            super(looper);
            this.mController = controller;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_DUMP && mController != null) {
                mController.dumpAudioEffectState();
            }
        }
    }
}
