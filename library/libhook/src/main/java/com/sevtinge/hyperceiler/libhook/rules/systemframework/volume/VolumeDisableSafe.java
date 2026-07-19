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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.volume;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class VolumeDisableSafe extends BaseHook {
    private static final String HOT_RELOAD_CONTEXT_KEY =
        "VolumeDisableSafe.audioContext";
    private static final String HOT_RELOAD_HEADSET_STATE_KEY =
        "VolumeDisableSafe.headsetOn";
    private static boolean isHeadsetOn = false;

    private static final int mode = PrefsBridge.getStringAsInt("system_framework_volume_disable_safe_new", 0);
    private volatile boolean mReceiverRegistered;

    @Override
    public void init() {
        Class<?> SoundDoseHelperStub = findClass("com.android.server.audio.SoundDoseHelperStubImpl");
        Class<?> SoundDoseHelper = findClass("com.android.server.audio.SoundDoseHelper");
        Boolean restoredHeadsetOn = getHotReloadRuntimeState(
            HOT_RELOAD_HEADSET_STATE_KEY, Boolean.class);
        if (restoredHeadsetOn != null) {
            isHeadsetOn = restoredHeadsetOn;
        }
        if (mode != 1) {
            Context restoredContext = getHotReloadRuntimeState(HOT_RELOAD_CONTEXT_KEY, Context.class);
            if (restoredContext != null) {
                registerHeadsetReceiver(restoredContext);
            }
        }

        findAndHookMethod(SoundDoseHelperStub, "updateSafeMediaVolumeIndex", int.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (mode == 1) {
                    param.setResult(2147483646);
                    return;
                }
                if (isHeadsetOn) param.setResult(2147483646);
            }
        });

        findAndHookMethod(SoundDoseHelper, "safeMediaVolumeIndex", int.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (mode == 1) {
                    param.setResult(2147483646);
                    return;
                }
                if (isHeadsetOn) param.setResult(2147483646);
            }
        });

        hookAllConstructors(SoundDoseHelper, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                if (mode == 1) {
                    return;
                }
                Context context = (Context) param.getArgs()[1];
                registerHeadsetReceiver(context);
            }
        });

    }

    private void registerHeadsetReceiver(Context context) {
        if (context == null || mReceiverRegistered) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        Listener receiver = new Listener();
        context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED);
        mReceiverRegistered = true;
        putHotReloadRuntimeState(HOT_RELOAD_CONTEXT_KEY, context);
        putHotReloadRuntimeState(HOT_RELOAD_HEADSET_STATE_KEY, isHeadsetOn);
        registerReceiverHotReloadCleanup(context, receiver);
        registerHotReloadCleanup(() -> mReceiverRegistered = false);
    }

    private static class Listener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothDevice.ACTION_ACL_CONNECTED -> setHeadsetOn(true);
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED -> setHeadsetOn(false);
                    case AudioManager.ACTION_HEADSET_PLUG -> {
                        if (intent.hasExtra("state")) {
                            int state = intent.getIntExtra("state", 0);
                            if (state == 1) {
                                setHeadsetOn(true);
                            } else if (state == 0) {
                                setHeadsetOn(false);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void setHeadsetOn(boolean value) {
        isHeadsetOn = value;
        putHotReloadRuntimeState(HOT_RELOAD_HEADSET_STATE_KEY, value);
    }
}
