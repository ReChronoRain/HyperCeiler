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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import static com.hchen.hooktool.log.XposedLog.logI;

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

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.tool.additional.SystemPropTool;
import com.sevtinge.hyperceiler.utils.api.effect.binder.EffectInfoService;
import com.sevtinge.hyperceiler.utils.api.effect.callback.IControlForSystem;
import com.sevtinge.hyperceiler.utils.api.effect.control.AudioEffectControlForSystem;
import com.sevtinge.hyperceiler.utils.api.effect.control.FWAudioEffectControlForSystem;

/**
 * 自动切换音效-系统框架端-总控制端
 *
 * @author 焕晨HChen
 */
public class AutoEffectSwitchForSystem extends BaseHC {
    public static final String TAG = "AutoEffectSwitchForSystem";
    public Context mContext;
    private static Handler mHandler;
    private static boolean shouldWaiting = false;
    private static boolean isLeAudioConnected = false;
    private static final int STATE_CHANGE = 0;
    private static AudioManager mAudioManager = null;
    public static EffectInfoService mEffectInfoService = null;
    private AudioEffectControlForSystem mAudioEffectControlForSystem = null;
    private FWAudioEffectControlForSystem mFWAudioEffectControlForSystem = null;
    public static boolean isEarphoneConnection = false;
    private IControlForSystem mIControlForSystem = null;

    @Override
    protected void init() {
        if (isSupportFW()) {
            mFWAudioEffectControlForSystem = new FWAudioEffectControlForSystem();
            mFWAudioEffectControlForSystem.init();
            mIControlForSystem = mFWAudioEffectControlForSystem;
        } else {
            mAudioEffectControlForSystem = new AudioEffectControlForSystem();
            mAudioEffectControlForSystem.init();
            mIControlForSystem = mAudioEffectControlForSystem;
        }

        hookMethod("com.android.server.audio.AudioService",
                "onSystemReady",
                new IHook() {
                    @Override
                    public void after() {
                        mContext = (Context) getThisField("mContext");
                        if (mContext == null) return; // 不可能会是 null 吧??

                        if (mFWAudioEffectControlForSystem != null)
                            mFWAudioEffectControlForSystem.setContext(mContext);
                        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                        if (mEffectInfoService == null) {
                            if (mFWAudioEffectControlForSystem != null)
                                mEffectInfoService = new EffectInfoService(mFWAudioEffectControlForSystem);
                            else if (mAudioEffectControlForSystem != null)
                                mEffectInfoService = new EffectInfoService(mAudioEffectControlForSystem);
                        }

                        initHandler();
                        registerEarphoneReceiver();
                        registerDebug();
                        reportEarphoneState();
                        logI(TAG, "audio system ready!!");
                    }
                }
        );
    }

    private boolean isSupportFW() {
        return SystemPropTool.getProp("ro.vendor.audio.fweffect", false);
    }

    public static boolean getEarPhoneStateFinal() {
        if (isEarphoneConnection) return true;
        AudioDeviceInfo[] outputs = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo info : outputs) {
            if (info.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || info.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    info.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || info.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {
                logI(TAG, "getEarPhoneState: isEarPhoneConnection: true.");
                shouldWaiting = true;
                if (mHandler != null) {
                    mHandler.removeMessages(STATE_CHANGE);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(STATE_CHANGE), 1000L);
                }
                return true;
            }
        }

        if (shouldWaiting) {
            logI(TAG, "getEarPhoneState: isWaiting: true.");
            return true;
        }
        logI(TAG, "getEarPhoneState: isEarPhoneConnection: false.");
        return false;
    }

    private void initHandler() {
        if (mContext == null) return;
        mHandler = new Handler(mContext.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == STATE_CHANGE) {
                    shouldWaiting = false;
                    reportEarphoneState();
                }
            }
        };
    }

    private void registerDebug() {
        if (mContext == null) return;

        // 用于因为某些可能的情况导致 isEarphoneConnection 一直为 true 的情况。
        Settings.Global.putInt(mContext.getContentResolver(), "auto_effect_switch_restore_earphone_state", 0);
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor("auto_effect_switch_restore_earphone_state"),
                false,
                new ContentObserver(new Handler(mContext.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        if (selfChange) return;
                        if (Settings.Global.getInt(mContext.getContentResolver(), "auto_effect_switch_restore_earphone_state", 0) == 1) {
                            isEarphoneConnection = false;
                            Settings.Global.putInt(mContext.getContentResolver(), "auto_effect_switch_restore_earphone_state", 0);
                            logI(TAG, "restore earphone state to false!!");
                        }
                    }
                }
        );
    }

    private void reportEarphoneState() {
        if (mContext == null) return;
        Settings.Global.putInt(mContext.getContentResolver(), "auto_effect_switch_earphone_state", isEarphoneConnection ? 1 : 0);
    }

    private void registerEarphoneReceiver() {
        if (mContext == null) return;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        mContext.registerReceiver(new EarphoneBroadcastReceiver(), intentFilter);
    }

    class EarphoneBroadcastReceiver extends BroadcastReceiver {
        private DumpHandler mDumpHandler;
        public static final int DUMP = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDumpHandler == null)
                mDumpHandler = new DumpHandler(context.getMainLooper());

            String action = intent.getAction();
            if (action != null) {
                logI(TAG, "onReceive: action: " + action);
                switch (action) {
                    case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, 0);
                        switch (state) {
                            case BluetoothLeAudio.STATE_CONNECTED -> connected();
                            case BluetoothLeAudio.STATE_DISCONNECTED -> disconnected();
                        }
                    }
                    case BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED -> {
                        int state = intent.getIntExtra(BluetoothLeAudio.EXTRA_STATE, 0);
                        switch (state) {
                            case BluetoothLeAudio.STATE_CONNECTED -> {
                                if (isLeAudioConnected) return;
                                connected();
                                isLeAudioConnected = true;
                            }
                            case BluetoothLeAudio.STATE_DISCONNECTED -> {
                                if (!isLeAudioConnected) return;
                                disconnected();
                                isLeAudioConnected = false;
                            }
                        }
                    }
                    case AudioManager.ACTION_HEADSET_PLUG -> {
                        if (intent.hasExtra("state")) {
                            int state = intent.getIntExtra("state", 0);
                            switch (state) {
                                case 1 -> connected();
                                case 0 -> disconnected();
                            }
                        }
                    }
                }
            }
        }

        private void connected() {
            isEarphoneConnection = true;
            mIControlForSystem.updateLastEffectState();
            mIControlForSystem.setEffectToNone(mContext);
            dump();
        }

        private void disconnected() {
            isEarphoneConnection = false;
            mIControlForSystem.resetAudioEffect();
            dump();
        }

        private void dump() {
            if (mDumpHandler.hasMessages(DUMP))
                mDumpHandler.removeMessages(DUMP);
            mDumpHandler.sendEmptyMessageDelayed(DUMP, 1000);
        }
    }

    private class DumpHandler extends Handler {
        public DumpHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            int what = msg.what;
            if (what == EarphoneBroadcastReceiver.DUMP) {
                mIControlForSystem.dumpAudioEffectState();
            }
        }
    }
}
