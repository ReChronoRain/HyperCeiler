package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.base.CloseHostDir;
import com.sevtinge.cemiuiler.module.base.LoadHostDir;
import com.sevtinge.cemiuiler.module.hook.screenrecorder.ForceSupportPlaybackCapture;
import com.sevtinge.cemiuiler.module.hook.screenrecorder.SaveToMovies;
import com.sevtinge.cemiuiler.module.hook.screenrecorder.ScreenRecorderConfig;
import com.sevtinge.cemiuiler.module.hook.screenrecorder.UnlockMoreVolumeFrom;

public class ScreenRecorder extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(new ForceSupportPlaybackCapture(), mPrefsMap.getBoolean("screenrecorder_force_support_playback_capture"));
        initHook(new UnlockMoreVolumeFrom(), mPrefsMap.getBoolean("screenrecorder_more_volume"));
        initHook(ScreenRecorderConfig.INSTANCE, mPrefsMap.getBoolean("screenrecorder_config"));
        initHook(SaveToMovies.INSTANCE, mPrefsMap.getBoolean("screenrecorder_save_to_movies"));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}

