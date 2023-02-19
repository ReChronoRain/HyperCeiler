package com.sevtinge.cemiuiler.module.systemframework;

import android.media.AudioManager;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class VolumeFirstPress extends BaseHook {

    Class<?> mVolumeController;

    @Override
    public void init() {
        mVolumeController = findClassIfExists("com.android.server.audio.AudioService$VolumeController");

        findAndHookMethod(mVolumeController, "suppressAdjustment", int.class, int.class, boolean.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                int streamType = (int)param.args[0];
                if (streamType != AudioManager.STREAM_MUSIC) return;
                boolean isMuteAdjust = (boolean)param.args[2];
                if (isMuteAdjust) return;
                Object mController = XposedHelpers.getObjectField(param.thisObject, "mController");
                if (mController == null) return;
                param.setResult(false);
            }
        });
    }
}
