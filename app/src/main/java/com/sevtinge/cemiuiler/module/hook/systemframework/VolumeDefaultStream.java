package com.sevtinge.cemiuiler.module.hook.systemframework;

import android.content.Context;
import android.os.Handler;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import de.robv.android.xposed.XposedHelpers;

public class VolumeDefaultStream extends BaseHook {

    Class<?> mAudioService;

    @Override
    public void init() {
        mAudioService = findClassIfExists("com.android.server.audio.AudioService");

        findAndHookMethod(mAudioService, "getActiveStreamType", int.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {

                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                Handler mHandler = new Handler(mContext.getMainLooper());
                new PrefsUtils.SharedPrefsObserver(mContext, mHandler, "prefs_key_system_framework_default_volume_stream", "0") {
                    @Override
                    public void onChange(String name, String defValue) {
                        mPrefsMap.put(name, PrefsUtils.getSharedStringPrefs(mContext, name, defValue));
                    }
                };

                int mDefaultVolumeStream = mPrefsMap.getStringAsInt("system_framework_default_volume_stream", 0);

                if (mDefaultVolumeStream > 0) {
                    param.setResult(mDefaultVolumeStream);
                }

            }
        });
    }
}
