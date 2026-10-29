/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.recent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.sevtinge.hyperceiler.common.log.ModuleLog;

/** Invoked by the authenticated WMS endpoint, never on the Flutter UI isolate. */
public final class HomeRecentsHapticOS4 {
    private HomeRecentsHapticOS4() {}
    private static Vibrator vibrator;
    private static int reported;

    @SuppressLint("MissingPermission")
    public static boolean perform(Context context,int type,int strength,int count) {
        try {
            if(vibrator==null){
                vibrator=context.getSystemService(Vibrator.class);
            }
            if(vibrator==null||!vibrator.hasVibrator())return false;
            var pattern=HomeRecentsHapticPattern.create(type,strength,count,vibrator.hasAmplitudeControl());
            var effect=VibrationEffect.createWaveform(pattern.timings(),pattern.amplitudes(),-1);
            vibrator.vibrate(effect,VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH));
            return true;
        } catch(RuntimeException error){
            if(reported++<3)ModuleLog.w("HyperCeiler.HomeLayout","recents card vibration unavailable",error);
            return false;
        }
    }
}
