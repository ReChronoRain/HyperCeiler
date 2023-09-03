package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.screenrecorder.ForceSupportPlaybackCapture;
import com.sevtinge.cemiuiler.module.hook.screenrecorder.SaveToMovies;
import com.sevtinge.cemiuiler.module.hook.screenrecorder.ScreenRecorderConfig;
import com.sevtinge.cemiuiler.module.hook.screenrecorder.ScreenRecorderDexKit;
import com.sevtinge.cemiuiler.module.hook.screenrecorder.UnlockMoreVolumeFrom;

public class ScreenRecorder extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new ScreenRecorderDexKit());
        initHook(new ForceSupportPlaybackCapture(), mPrefsMap.getBoolean("screenrecorder_force_support_playback_capture"));
        initHook(new UnlockMoreVolumeFrom(), mPrefsMap.getBoolean("screenrecorder_more_volume"));
        initHook(ScreenRecorderConfig.INSTANCE, mPrefsMap.getBoolean("screenrecorder_config"));
        initHook(SaveToMovies.INSTANCE, mPrefsMap.getBoolean("screenrecorder_save_to_movies"));
    }
}

