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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;

import com.sevtinge.hyperceiler.libhook.IEffectInfo;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

/**
 * 新版连接耳机自动切换原声
 *
 * @author 焕晨HChen
 */
public class NewAutoSEffSwitch extends BaseHook {
    public static final String TAG = "NewAutoSEffSwitch";
    private Context mContext;
    public static AudioManager mAudioManager;
    private static IEffectInfo mIEffectInfo;
    private NewFWAudioEffectControl mNewFWAudioEffectControl = null;
    private NewAudioEffectControl mNewAudioEffectControl = null;

    @Override
    public void init() {
        if (isSupportFW()) {
            mNewFWAudioEffectControl = new NewFWAudioEffectControl();
            mNewFWAudioEffectControl.init();
        } else {
            mNewAudioEffectControl = new NewAudioEffectControl();
            mNewAudioEffectControl.init();
        }

        runOnApplicationAttach(context -> {
            mContext = context;
            Intent intent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (intent == null) return;
            Bundle bundle = intent.getBundleExtra("effect_info");
            if (bundle == null) return;
            mIEffectInfo = IEffectInfo.Stub.asInterface(bundle.getBinder("effect_info"));
            XposedLog.d(TAG, "com.miui.misound", "onApplication: EffectInfoService: " + mIEffectInfo);
            if (mIEffectInfo == null) return;
            if (mNewFWAudioEffectControl != null)
                mNewFWAudioEffectControl.mIEffectInfo = mIEffectInfo;
            else if (mNewAudioEffectControl != null) {
                mNewAudioEffectControl.mIEffectInfo = mIEffectInfo;
            }

            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor("auto_effect_switch_earphone_state"),
                false,
                new ContentObserver(new Handler(mContext.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        if (selfChange) return;
                        int result = Settings.Global.getInt(mContext.getContentResolver(), "auto_effect_switch_earphone_state", 0);
                        XposedLog.d(TAG, "com.miui.misound", "settings observer earphone state change to: " + result);

                        if (mNewFWAudioEffectControl != null)
                            mNewFWAudioEffectControl.updateEffectSelectionState();
                        else if (mNewAudioEffectControl != null) {
                            mNewAudioEffectControl.updateEffectSelectionState();
                        }
                    }
                }
            );

            if (mNewFWAudioEffectControl != null)
                callStaticMethod(
                    findClass("android.media.audiofx.AudioEffectCenter"),
                        "getInstance",
                        mContext
                    );
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
                XposedLog.e(TAG, "com.miui.misound", e);
                return false;
            }
        }
        XposedLog.w(TAG, "com.miui.misound", "getEarPhoneStateFinal: mIEffectInfo is null!!");
        return false;
    }
}
