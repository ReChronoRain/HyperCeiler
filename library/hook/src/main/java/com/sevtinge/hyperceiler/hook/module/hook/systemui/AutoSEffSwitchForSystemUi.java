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
package com.sevtinge.hyperceiler.hook.module.hook.systemui;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;

import com.hchen.hooktool.HCBase;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.utils.SystemPropTool;
import com.sevtinge.hyperceiler.hook.IEffectInfo;

import java.util.function.Supplier;

public class AutoSEffSwitchForSystemUi extends HCBase {
    private static final String TAG = "AutoSEffSwitchForSystemUi";
    private boolean isInit = false;
    private static IEffectInfo mIEffectInfo;

    @Override
    protected void init() {
        if (isSupportFW())
            onSupportFW();
        else
            onNotSupportFW();
    }

    public static boolean isSupportFW() {
        return SystemPropTool.getProp("ro.vendor.audio.fweffect", false);
    }

    @Override
    protected void onApplication(Context context) {

        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return;
        Bundle bundle = intent.getBundleExtra("effect_info");
        if (bundle == null) return;
        mIEffectInfo = IEffectInfo.Stub.asInterface(bundle.getBinder("effect_info"));
        logI(TAG, "onApplication: EffectInfoService: " + mIEffectInfo);
    }

    public static boolean getEarPhoneStateFinal() {
        if (mIEffectInfo != null) {
            try {
                return mIEffectInfo.isEarphoneConnection();
            } catch (RemoteException e) {
                logE(TAG, e);
                return false;
            }
        }
        logW(TAG, "getEarPhoneStateFinal: mIEffectInfo is null!!");
        return false;
    }

    private void onSupportFW() {
        hookMethod("android.media.audiofx.AudioEffectCenter",
                "setEffectActive",
                String.class, boolean.class,
                new IHook() {
                    @Override
                    public void before() {
                        if (getEarPhoneStateFinal()) {
                            logI(TAG, "earphone is connection, skip set effect: " + getArg(0) + "!!");
                            returnNull();
                        }
                    }
                }
        );
    }

    private void onNotSupportFW() {
        hookMethod("com.android.systemui.shared.plugins.PluginInstance$PluginFactory",
                "createPluginContext",
                new IHook() {
                    @Override
                    public void after() {
                        if (isInit) return;
                        Supplier<?> mClassLoaderFactory = (Supplier<?>) getThisField("mClassLoaderFactory");
                        load((ClassLoader) mClassLoaderFactory.get());
                        isInit = true;
                    }
                }
        );
    }

    private void load(ClassLoader classLoader) {
        hookMethod("miui.systemui.quicksettings.soundeffect.DolbyAtomsSoundEffectTile",
                classLoader,
                "handleClick",
                new IHook() {
                    @Override
                    public void before() {
                        if (getEarPhoneStateFinal()) {
                            logI(TAG, "earphone is connection, skip set effect: " + getArg(0) + "!!");
                            returnNull();
                        }
                    }
                }
        );
    }
}
