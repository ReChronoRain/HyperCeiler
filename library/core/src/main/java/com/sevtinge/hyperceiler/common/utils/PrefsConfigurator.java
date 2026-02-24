package com.sevtinge.hyperceiler.common.utils;

import android.content.Context;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

public class PrefsConfigurator {
    public static void setup(PreferenceFragmentCompat fragment) {
        PreferenceManager pm = fragment.getPreferenceManager();
        pm.setPreferenceDataStore(new PrefsDataStore());
        /*PreferenceManager pm = fragment.getPreferenceManager();
        pm.setSharedPreferencesName(PrefsBridge.PREFS_NAME);
        pm.setSharedPreferencesMode(Context.MODE_PRIVATE);*/
    }

    public static void reset(PreferenceFragmentCompat fragment, int xmlResId) {
        Context context = fragment.requireContext();
        context.getSharedPreferences(PrefsBridge.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        PreferenceManager.setDefaultValues(context, PrefsBridge.PREFS_NAME, Context.MODE_PRIVATE, xmlResId, true);

        fragment.setPreferenceScreen(null);
        fragment.addPreferencesFromResource(xmlResId);

        // 通知远程进程（Hook 端）数据已重置
        //PrefsBridge.put("__reset__", System.currentTimeMillis());
    }

    /**
     * 重置：先清空，再根据当前 XML 恢复本页默认值，最后刷新 UI
     */
    public static void performReset(PreferenceFragmentCompat fragment, int currentXmlId) {
        Context context = fragment.requireContext();
        //PrefsBridge.clearAll();
        PreferenceManager.setDefaultValues(context, PrefsBridge.PREFS_NAME, Context.MODE_PRIVATE, currentXmlId, true);

        fragment.setPreferenceScreen(null);
        fragment.addPreferencesFromResource(currentXmlId);
    }
}
