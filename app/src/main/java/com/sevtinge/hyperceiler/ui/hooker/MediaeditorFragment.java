package com.sevtinge.hyperceiler.ui.hooker;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.DropDownPreference;

public class MediaeditorFragment extends DashboardFragment
        implements Preference.OnPreferenceChangeListener {

    DropDownPreference mMediaHook;
    PreferenceCategory mAuthoringV1;
    PreferenceCategory mAuthoringV2;
    PreferenceCategory mCustomPhoto;
    PreferenceCategory mCustomDisney;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.mediaeditor;
    }

    @Override
    public void initPrefs() {
        int mMediaHookMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_mediaeditor_hook_type", "0"));
        mMediaHook = findPreference("prefs_key_mediaeditor_hook_type");
        mAuthoringV1 = findPreference("prefs_key_mediaeditor_custom_photo_frames_v1");
        mAuthoringV2 = findPreference("prefs_key_mediaeditor_custom_photo_frames_v2");
        mCustomPhoto = findPreference("prefs_key_mediaeditor_custom_photo_frames_v2_photo");
        mCustomDisney = findPreference("prefs_key_mediaeditor_custom_photo_frames_v2_disney");

        setMediaHookMode(mMediaHookMode);
        mMediaHook.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mMediaHook) {
            setMediaHookMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setMediaHookMode(int mode) {
        if (mode == 0) {
            mAuthoringV1.setVisible(false);
            mAuthoringV2.setVisible(false);
            mCustomPhoto.setVisible(false);
            mCustomDisney.setVisible(false);
        } else if (mode == 1) {
            mAuthoringV1.setVisible(true);
            mAuthoringV2.setVisible(false);
            mCustomPhoto.setVisible(false);
            mCustomDisney.setVisible(false);
        } else if (mode == 2) {
            mAuthoringV1.setVisible(false);
            mAuthoringV2.setVisible(true);
            mCustomPhoto.setVisible(PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_mediaeditor_unlock_custom_photo_frames_v2", false));
            mCustomDisney.setVisible(PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_mediaeditor_unlock_disney_some_func_v2", false));
        }
    }
}
