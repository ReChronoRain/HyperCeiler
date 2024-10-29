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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.callback.IAction;
import com.sevtinge.hyperceiler.module.base.tool.HookTool;

public class VolumeDisableSafe extends BaseHC {
    private static boolean isHeadsetOn = false;

    private static final int mode = HookTool.mPrefsMap.getStringAsInt("system_framework_volume_disable_safe_new", 0);

    @Override
    public void init() {
        if (isMoreAndroidVersion(34)) {
            hook("com.android.server.audio.SoundDoseHelperStubImpl", "updateSafeMediaVolumeIndex", int.class,
                    new IAction() {
                        @Override
                        public void before() throws Throwable {
                            if (mode == 1) {
                                setResult(2147483646);
                                return;
                            }
                            if (isHeadsetOn) setResult(2147483646);
                        }
                    });

            chain("com.android.server.audio.SoundDoseHelper", method("safeMediaVolumeIndex", int.class)
                    .hook(
                            new IAction() {
                                @Override
                                public void before() throws Throwable {
                                    if (mode == 1) {
                                        setResult(2147483646);
                                        return;
                                    }
                                    if (isHeadsetOn) setResult(2147483646);
                                }
                            })

                    .anyConstructor()
                    .hook(new IAction() {
                        @Override
                        public void after() throws Throwable {
                            if (mode == 1) {
                                return;
                            }
                            Context context = second();
                            IntentFilter intentFilter = new IntentFilter();
                            intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
                            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                            intentFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
                            context.registerReceiver(new Listener(), intentFilter);
                        }
                    })

            );
        } else {
            hook("com.android.server.audio.AudioService", "safeMediaVolumeIndex",
                    new IAction() {
                        @Override
                        public void before() throws Throwable {
                            if (mode == 1) {
                                setResult(2147483646);
                                return;
                            }
                            if (isHeadsetOn) setResult(2147483646);
                        }
                    });
        }
    }

    private static class Listener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothDevice.ACTION_ACL_CONNECTED -> isHeadsetOn = true;
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED -> isHeadsetOn = false;
                    case AudioManager.ACTION_HEADSET_PLUG -> {
                        if (intent.hasExtra("state")) {
                            int state = intent.getIntExtra("state", 0);
                            if (state == 1) {
                                isHeadsetOn = true;
                            } else if (state == 0) {
                                isHeadsetOn = false;
                            }
                        }
                    }
                }
            }
        }
    }
}
