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
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.MSG_DUMP;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.MSG_STATE_CHANGE;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.SETTINGS_KEY_EARPHONE_STATE;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.SETTINGS_KEY_RESTORE_STATE;

import android.bluetooth.BluetoothA2dp;
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

    // 实例变量
    private Context mContext;
    private IControlForSystem mEffectController;
    public static EffectInfoService mEffectInfoService;

    @Override
    public void init() {
        initEffectController();
        hookAudioServiceOnSystemReady();
    }

    /**
     * 初始化音效控制器
     */
    private void initEffectController() {
        if (isSupportFW()) {
            FWAudioEffectControlForSystem fwController = new FWAudioEffectControlForSystem();
            fwController.init();
            mEffectController = fwController;
            mEffectInfoService = new EffectInfoService(fwController);XposedLog.d(TAG, "Using FW AudioEffectControl");
        } else {
            AudioEffectControlForSystem controller = new AudioEffectControlForSystem();
            controller.init();
            mEffectController = controller;
            mEffectInfoService = new EffectInfoService(controller);
            XposedLog.d(TAG, "Using Non-FW AudioEffectControl");
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
        // 设置 Context
        mEffectController.setContext(mContext);

        // 初始化 AudioManager
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        sAudioManagerRef.set(audioManager);

        // 初始化 Handler
        initHandler();

        // 注册广播接收器和调试监听
        registerEarphoneReceiver();
        registerDebugObserver();

        // 报告初始状态
        reportEarphoneState();

        XposedLog.d(TAG, "Audio system ready, initialization completed");
    }

    /**
     * 检查是否支持 FW 模式
     */
    public static boolean isSupportFW() {
        return getProp(PROP_FW_EFFECT, false);
    }

    /**
     * 获取耳机连接状态（最终判断）
     * 综合考虑广播状态和实际设备状态
     */
    public static boolean getEarPhoneStateFinal() {
        // 如果广播已经标记为连接，直接返回
        if (sIsEarphoneConnection.get()) {
            return true;
        }

        // 检查实际设备状态
        AudioManager audioManager = sAudioManagerRef.get();
        if (audioManager != null && isEarphoneDeviceConnected(audioManager)) {
            XposedLog.d(TAG, "Earphone detected via AudioManager");
            startWaitingTimer();
            return true;
        }

        // 检查是否在等待状态
        if (sShouldWaiting.get()) {
            XposedLog.d(TAG, "In waiting state");
            return true;
        }

        return false;
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
     * 用于手动重置耳机状态
     */
    private void registerDebugObserver() {
        if (mContext == null) return;

        // 初始化设置值
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
            new EarphoneBroadcastReceiver(mEffectController, mContext, this::reportEarphoneState),
            intentFilter
        );

        XposedLog.d(TAG, "Earphone broadcast receiver registered");
    }

    /**
     * 耳机广播接收器
     */
    private static class EarphoneBroadcastReceiver extends BroadcastReceiver {

        private final IControlForSystem mController;
        private final Context mContext;
        private final Runnable mReportCallback;
        private Handler mDumpHandler;

        EarphoneBroadcastReceiver(IControlForSystem controller, Context context, Runnable reportCallback) {
            this.mController = controller;
            this.mContext = context;
            this.mReportCallback = reportCallback;}

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDumpHandler == null) {
                mDumpHandler = new DumpHandler(context.getMainLooper(), mController);
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
            switch (state) {
                case BluetoothA2dp.STATE_CONNECTED -> onConnected();
                case BluetoothA2dp.STATE_DISCONNECTED -> onDisconnected();
            }
        }

        private void handleLeAudioStateChange(Intent intent) {
            int state = intent.getIntExtra(BluetoothLeAudio.EXTRA_STATE, BluetoothLeAudio.STATE_DISCONNECTED);
            switch (state) {
                case BluetoothLeAudio.STATE_CONNECTED -> {
                    if (!sIsLeAudioConnected.getAndSet(true)) {
                        onConnected();
                    }
                }
                case BluetoothLeAudio.STATE_DISCONNECTED -> {
                    if (sIsLeAudioConnected.getAndSet(false)) {
                        onDisconnected();
                    }
                }
            }
        }

        private void handleHeadsetPlug(Intent intent) {
            int state = intent.getIntExtra("state", 0);
            switch (state) {
                case 1 -> onConnected();
                case 0 -> onDisconnected();
            }
        }

        private void onConnected() {
            XposedLog.d(TAG, "Earphone connected");
            sIsEarphoneConnection.set(true);
            mController.updateLastEffectState();
            mController.setEffectToNone(mContext);
            mReportCallback.run();
            scheduleDump();
        }

        private void onDisconnected() {
            XposedLog.d(TAG, "Earphone disconnected");
            sIsEarphoneConnection.set(false);
            mController.resetAudioEffect();
            mReportCallback.run();
            scheduleDump();
        }

        private void scheduleDump() {
            if (mDumpHandler != null) {
                mDumpHandler.removeMessages(MSG_DUMP);
                mDumpHandler.sendEmptyMessageDelayed(MSG_DUMP, DELAY_DUMP);
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
