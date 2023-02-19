package com.sevtinge.cemiuiler.module.systemframework;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class VolumeSteps extends BaseHook {

    Class<?> mAudioService;

    @Override
    public void init() {

       mAudioService = findClass("com.android.server.audio.AudioService");

        findAndHookMethod(mAudioService, "createStreamStates", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {

                int[] maxStreamVolume = (int[])XposedHelpers.getStaticObjectField(mAudioService, "MAX_STREAM_VOLUME");
                int mult = mPrefsMap.getInt("system_framework_volume_steps", 0);
                if (mult <= 0) return;
                for (int i = 0; i < maxStreamVolume.length; i++)
                    maxStreamVolume[i] = Math.round(maxStreamVolume[i] * mult / 100.0f);
                XposedHelpers.setStaticObjectField(mAudioService, "MAX_STREAM_VOLUME", maxStreamVolume);
            }
        });
    }
}
