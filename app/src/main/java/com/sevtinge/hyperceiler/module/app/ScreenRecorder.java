package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.screenrecorder.ForceSupportPlaybackCapture;
import com.sevtinge.hyperceiler.module.hook.screenrecorder.SaveToMovies;
import com.sevtinge.hyperceiler.module.hook.screenrecorder.ScreenRecorderConfig;
import com.sevtinge.hyperceiler.module.hook.screenrecorder.UnlockMoreVolumeFromNew;

public class ScreenRecorder extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new ForceSupportPlaybackCapture(), mPrefsMap.getBoolean("screenrecorder_force_support_playback_capture"));
        initHook(UnlockMoreVolumeFromNew.INSTANCE, mPrefsMap.getBoolean("screenrecorder_more_volume"));
        initHook(ScreenRecorderConfig.INSTANCE, mPrefsMap.getBoolean("screenrecorder_config"));
        initHook(SaveToMovies.INSTANCE, mPrefsMap.getBoolean("screenrecorder_save_to_movies"));
    }
}

