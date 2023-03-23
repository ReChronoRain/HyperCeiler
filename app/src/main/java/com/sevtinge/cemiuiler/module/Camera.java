package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.camera.*;

public class Camera extends BaseModule {

    @Override
    public void handleLoadPackage() {
        //功能
        initHook(new UnlockAiWatermark(), mPrefsMap.getBoolean("camera_features_unlock_aiwatermark"));
        initHook(new UnlockAmbilight(), mPrefsMap.getBoolean("camera_features_unlock_ambilight"));
        initHook(new UnlockClone(), mPrefsMap.getBoolean("camera_features_unlock_clone"));
        initHook(new UnlockCosmeticMirror(), mPrefsMap.getBoolean("camera_features_unlock_cosmetic_mirror"));
        initHook(new UnlockDoc(), mPrefsMap.getBoolean("camera_features_unlock_doc"));
        initHook(new UnlockDualcam(), mPrefsMap.getBoolean("camera_features_unlock_dualcam"));
        initHook(new UnlockDuration(), mPrefsMap.getBoolean("camera_features_unlock_duration"));
        initHook(new UnlockIdcard(), mPrefsMap.getBoolean("camera_features_unlock_idcard"));
        initHook(new UnlockMilive(), mPrefsMap.getBoolean("camera_features_unlock_milive"));
        initHook(new UnlockMoon(), mPrefsMap.getBoolean("camera_features_unlock_moon"));
        initHook(new UnlockPano(), mPrefsMap.getBoolean("camera_features_unlock_pano"));
        initHook(new UnlockPixel(), mPrefsMap.getBoolean("camera_features_unlock_pixel"));
        initHook(new UnlockPortrait(), mPrefsMap.getBoolean("camera_features_unlock_portrait"));
        initHook(new UnlockSlow(), mPrefsMap.getBoolean("camera_features_unlock_slow"));
        initHook(new UnlockVideosky(), mPrefsMap.getBoolean("camera_features_unlock_videosky"));
        initHook(new UnlockVlog(), mPrefsMap.getBoolean("camera_features_unlock_vlog"));
        initHook(new UnlockVlogPro(), mPrefsMap.getBoolean("camera_features_unlock_vlog_pro"));
        initHook(new UnlockFilm(), mPrefsMap.getBoolean("camera_features_unlock_film"));
        initHook(new UnlockFilmDelay(), mPrefsMap.getBoolean("camera_features_unlock_film_delay"));
        initHook(new UnlockFilmDollyZoom(), mPrefsMap.getBoolean("camera_features_unlock_film_dollyzoom"));
        initHook(new UnlockFilmDream(), mPrefsMap.getBoolean("camera_features_unlock_film_dream"));
        initHook(new UnlockFilmSlowShutter(), mPrefsMap.getBoolean("camera_features_unlock_film_slowshutter"));
        initHook(new UnlockFilmTimeBackflow(), mPrefsMap.getBoolean("camera_features_unlock_film_timebackflow"));
        initHook(new UnlockFilmTimeFreeze(), mPrefsMap.getBoolean("camera_features_unlock_film_timefreeze"));

        //录像
        initHook(new Unlock60Fps(), mPrefsMap.getBoolean("camera_record_60fps"));
        initHook(new Unlock120Fps(), mPrefsMap.getBoolean("camera_record_120fps"));
    }
}
