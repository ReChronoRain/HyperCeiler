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

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.tool.ParamTool;
import com.sevtinge.hyperceiler.module.base.BaseTool;

public class VolumeDisableSafe extends BaseTool {
    private static boolean isHeadsetOn = false;

    private static final int mode = mPrefsMap.getStringAsInt("system_framework_volume_disable_safe_new", 0);

    @Override
    public void doHook() {
        if (isMoreAndroidVersion(34)) {
            hcHook.findClass("sound", "com.android.server.audio.SoundDoseHelperStubImpl")
                    .getMethod("updateSafeMediaVolumeIndex", int.class)
                    .hook(new IAction() {
                        @Override
                        public void before(ParamTool param) {
                            if (mode == 1) {
                                param.setResult(2147483646);
                                return;
                            }
                            if (isHeadsetOn) param.setResult(2147483646);
                        }
                    })
                    .findClass("SoundDoseHelper", "com.android.server.audio.SoundDoseHelper")
                    .getMethod("safeMediaVolumeIndex", int.class)
                    .hook(new IAction() {
                        @Override
                        public void before(ParamTool param) {
                            if (mode == 1) {
                                param.setResult(2147483646);
                                return;
                            }
                            if (isHeadsetOn) param.setResult(2147483646);
                        }
                    })
                    .getAnyConstructor()
                    .hook(new IAction() {
                        @Override
                        public void after(ParamTool param) {
                            if (mode == 1) {
                                return;
                            }
                            Context context = param.second();
                            IntentFilter intentFilter = new IntentFilter();
                            intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
                            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                            intentFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
                            context.registerReceiver(new Listener(), intentFilter);
                        }
                    })
            ;
        } else {
            hcHook.findClass("audio", "com.android.server.audio.AudioService")
                    .getAnyMethod("safeMediaVolumeIndex")
                    .hook(new IAction() {
                        @Override
                        public void before(ParamTool param) {
                            if (mode == 1) {
                                param.setResult(2147483646);
                                return;
                            }
                            if (isHeadsetOn) param.setResult(2147483646);
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
