/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.recent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import com.sevtinge.hyperceiler.libhook.provider.HomeRecentsHapticPrefs;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

import com.sevtinge.hyperceiler.common.log.ModuleLog;

/** Invoked by an authenticated feedback endpoint, never on the Flutter UI isolate. */
public final class HomeRecentsHapticOS4 {
    private HomeRecentsHapticOS4() {}
    private static volatile Vibrator vibrator;
    private static int reported;

    @SuppressLint("MissingPermission")
    public static boolean perform(Context context,int type,int strength,int count) {
        return perform(context,new HomeRecentsHapticPrefs.Profile(true,true,type,strength),count);
    }
    @SuppressLint("MissingPermission")
    public static boolean perform(Context context,HomeRecentsHapticPrefs.Profile profile,int count) {
        try {
            var effect=waveform(context,profile,count);
            if(effect==null)return false;
            vibrator.vibrate(effect,VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH));
            return true;
        } catch(RuntimeException error){
            if(reported++<3)ModuleLog.w("HyperCeiler.HomeLayout","recents card vibration unavailable",error);
            return false;
        }
    }

    public static VibrationEffect waveform(Context context,int type,int strength,int count) {
        return waveform(context,new HomeRecentsHapticPrefs.Profile(true,true,type,strength),count);
    }
    public static VibrationEffect waveform(Context context,HomeRecentsHapticPrefs.Profile profile,int count) {
        if(vibrator==null)vibrator=context.getSystemService(Vibrator.class);
        if(vibrator==null||!vibrator.hasVibrator())return null;
        if(profile.custom()) {
            if(count<1||count>8||profile.effectId()<0)return null;
            // No substitute waveform: an unsupported vendor ID must not play a different touch.
            try {
                if(vibrator.areEffectsSupported(profile.effectId())[0]==Vibrator.VIBRATION_EFFECT_SUPPORT_NO)return null;
                var effect=(VibrationEffect)HiddenApiBypass.invoke(VibrationEffect.class,null,"get",profile.effectId(),false);
                int strength=profile.strength()<35?0:profile.strength()<70?1:2;
                effect=(VibrationEffect)HiddenApiBypass.invoke(VibrationEffect.class,effect,"applyEffectStrength",strength);
                var composition=VibrationEffect.startComposition();
                for(int i=0;i<count;++i) {
                    if(i>0)HiddenApiBypass.invoke(VibrationEffect.Composition.class,composition,"addEffect",
                        VibrationEffect.createWaveform(new long[]{24},new int[]{0},-1));
                    HiddenApiBypass.invoke(VibrationEffect.Composition.class,composition,"addEffect",effect);
                }
                return composition.compose();
            } catch(ReflectiveOperationException|RuntimeException unsupported){return null;}
        }
        var pattern=HomeRecentsHapticPattern.create(profile.type(),profile.strength(),count,vibrator.hasAmplitudeControl());
        return VibrationEffect.createWaveform(pattern.timings(),pattern.amplitudes(),-1);
    }

    /** Enumerate the HAL's actual effect inventory; public constants supplement unknown inventories. */
    public record EffectInventory(java.util.SortedMap<Integer,Integer> ids,boolean complete) { }
    public static EffectInventory effectIds(Context context) {
        var result=new java.util.TreeMap<Integer,Integer>();
        boolean complete=false;
        var device=context.getSystemService(Vibrator.class);
        if(device==null||!device.hasVibrator())return new EffectInventory(result,false);
        try {
            Object info=HiddenApiBypass.invoke(Vibrator.class,device,"getInfo");
            Object ids=HiddenApiBypass.invoke(Class.forName("android.os.VibratorInfo"),info,"getSupportedEffects");
            complete=ids instanceof int[]||ids instanceof android.util.SparseBooleanArray;
            if(ids instanceof int[] values)for(int id:values)if(id>=0)result.put(id,Vibrator.VIBRATION_EFFECT_SUPPORT_YES);
            if(ids instanceof android.util.SparseBooleanArray values)for(int i=0;i<values.size();++i)
                if(values.keyAt(i)>=0)result.put(values.keyAt(i),values.valueAt(i)?Vibrator.VIBRATION_EFFECT_SUPPORT_YES:Vibrator.VIBRATION_EFFECT_SUPPORT_NO);
        } catch(ReflectiveOperationException|RuntimeException unavailable) { }
        for(var field:VibrationEffect.class.getFields())try {
            if(field.getName().startsWith("EFFECT_")&&field.getType()==int.class) {
                int id=field.getInt(null);
                if(id>=0)result.put(id,device.areEffectsSupported(id)[0]);
            }
        } catch(ReflectiveOperationException|RuntimeException ignored) { }
        return new EffectInventory(result,complete);
    }
}
