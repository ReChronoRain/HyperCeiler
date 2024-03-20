package com.sevtinge.hyperceiler.ui.fragment.helper;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.PackagesUtils;
import com.sevtinge.hyperceiler.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.utils.ToastHelper;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class HomepageEntrance extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private boolean isInit = false;
    private final String TAG = "HomepageEntrance";
    private static EntranceState entranceState = null;

    @Override
    public int getContentResId() {
        return R.xml.prefs_set_homepage_entrance;
    }

    public static void setEntranceStateListen(EntranceState entranceState) {
        HomepageEntrance.entranceState = entranceState;
    }

    @Override
    public void initPrefs() {
        super.initPrefs();
        if (isInit) return;
        Resources resources = getResources();
        ThreadPoolManager.getInstance().submit(() -> {
            try (XmlResourceParser xml = resources.getXml(R.xml.prefs_set_homepage_entrance)) {
                try {
                    int event = xml.getEventType();
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG) {
                            if (xml.getName().equals("SwitchPreference")) {
                                String key = xml.getAttributeValue(ANDROID_NS, "key");
                                SwitchPreference switchPreference = findPreference(key);
                                if (switchPreference != null) {
                                    String summary = (String) switchPreference.getSummary();
                                    if (summary != null && !summary.equals("android")) {
                                        if (PackagesUtils.checkAppStatus(getContext(), summary)) {
                                            switchPreference.setVisible(false);
                                        }
                                    }
                                    switchPreference.setOnPreferenceChangeListener(HomepageEntrance.this);
                                }
                            }
                        }
                        event = xml.next();
                    }
                    isInit = true;
                } catch (XmlPullParserException | IOException e) {
                    AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
                }
            }
        });
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (!isInit) {
            ToastHelper.makeText(getContext(), "尚未加载完毕，请稍后");
            return false;
        }
        entranceState.onEntranceStateChange(preference.getKey(), (boolean) o);
        return true;
    }

    public interface EntranceState {
        void onEntranceStateChange(String key, boolean state);
    }
}
