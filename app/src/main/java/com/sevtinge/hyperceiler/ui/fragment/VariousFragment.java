package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;

import android.os.Handler;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.PrefsUtils;
import com.sevtinge.hyperceiler.utils.ShellUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SwitchPreference;

public class VariousFragment extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    DropDownPreference mSuperModePreference;
    PreferenceCategory mDefault;
    SwitchPreference mClipboard;
    Preference mMipad; // 平板相关功能

    Handler handler;

    @Override
    public int getContentResId() {
        return R.xml.various;
    }

    @Override
    public void initPrefs() {
        int mode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_various_super_clipboard_e", "0"));
        mSuperModePreference = findPreference("prefs_key_various_super_clipboard_e");
        mDefault = findPreference("prefs_key_various_super_clipboard_key");
        mMipad = findPreference("prefs_key_various_mipad");
        mClipboard = findPreference("prefs_key_sogou_xiaomi_clipboard");
        mMipad.setVisible(isPad());
        handler = new Handler();

        mClipboard.setOnPreferenceChangeListener((preference, o) -> {
            initKill();
            return true;
        });
        setSuperMode(mode);
        mSuperModePreference.setOnPreferenceChangeListener(this);
    }

    private void initKill() {
        new Thread(() -> handler.post(() ->
            ShellUtils.execCommand("killall -s 9 com.sohu.inputmethod.sogou.xiaomi ;" +
                " killall -s 9 com.sohu.inputmethod.sogou", true, false))).start();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mSuperModePreference) {
            setSuperMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setSuperMode(int mode) {
        mDefault.setVisible(mode == 1);
    }
}
