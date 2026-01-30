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
package com.sevtinge.hyperceiler.libhook.rules.systemui.other;

import static com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getProp;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;

import com.sevtinge.hyperceiler.libhook.IEffectInfo;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class AutoSEffSwitchForSystemUi extends BaseHook {
    private static final String TAG = "AutoSEffSwitchForSystemUi";
    private final boolean isInit = false;
    private static IEffectInfo mIEffectInfo;

    @Override
    public void init() {
        if (isSupportFW()) onSupportFW();

        runOnApplicationAttach(context -> {
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (intent == null) return;
            Bundle bundle = intent.getBundleExtra("effect_info");
            if (bundle == null) return;
            mIEffectInfo = IEffectInfo.Stub.asInterface(bundle.getBinder("effect_info"));
            XposedLog.d(TAG, "com.android.systemui", "onApplication: EffectInfoService: " + mIEffectInfo);
        });
    }

    public static boolean isSupportFW() {
        return getProp("ro.vendor.audio.fweffect", false);
    }

    public static boolean getEarPhoneStateFinal() {
        if (mIEffectInfo != null) {
            try {
                return mIEffectInfo.isEarphoneConnection();
            } catch (RemoteException e) {
                XposedLog.e(TAG, "com.android.systemui", e);
                return false;
            }
        }
        XposedLog.w(TAG, "com.android.systemui", "getEarPhoneStateFinal: mIEffectInfo is null!!");
        return false;
    }

    private void onSupportFW() {
        findAndHookMethod("android.media.audiofx.AudioEffectCenter",
                "setEffectActive",
                String.class, boolean.class,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        if (getEarPhoneStateFinal()) {
                            XposedLog.d(TAG, "com.android.systemui", "earphone is connection, skip set effect: " + param.getArgs()[0] + "!!");
                            param.setResult(null);
                        }
                    }
                }
        );
    }


    public static void onNotSupportFW(ClassLoader classLoader) {
        EzxHelpUtils.findAndHookMethod("miui.systemui.quicksettings.soundeffect.DolbyAtomsSoundEffectTile", classLoader,
            "handleClick",
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (getEarPhoneStateFinal()) {
                        XposedLog.d(TAG, "com.android.systemui", "earphone is connection, skip set effect: " + param.getArgs()[0] + "!!");
                        param.setResult(null);
                    }
                }
            }
        );
    }

}
