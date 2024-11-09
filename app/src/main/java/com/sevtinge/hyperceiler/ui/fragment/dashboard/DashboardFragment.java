package com.sevtinge.hyperceiler.ui.fragment.dashboard;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;
import androidx.preference.PreferenceScreen;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.DialogHelper;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Field;

import fan.preference.PreferenceFragment;

public class DashboardFragment extends SettingsPreferenceFragment {

    private static final String TAG = "DashboardFragment";
    public static final String APP_NS = "http://schemas.android.com/apk/res-auto";

    private String mQuickRestartPackageName;

    @Override
    public int getPreferenceScreenResId() {
        return mPreferenceResId != 0 ? mPreferenceResId : 0;
    }

    @Override
    public void onCreatePreferencesAfter(Bundle bundle, String s) {
        super.onCreatePreferencesAfter(bundle, s);
        mQuickRestartPackageName = getQuickRestartPackageName(getContext(), getPreferenceScreenResId());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (!TextUtils.isEmpty(mQuickRestartPackageName)) {
            inflater.inflate(R.menu.navigation_immersion, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.quick_restart && !TextUtils.isEmpty(mQuickRestartPackageName)) {
            if (mQuickRestartPackageName.equals("system")) {
                DialogHelper.showRestartSystemDialog(getContext());
            } else {
                DialogHelper.showRestartDialog(getContext(), mQuickRestartPackageName);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private String getQuickRestartPackageName(Context context, @XmlRes int xmlResId) {
        Resources res = context.getResources();
        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xml.getName().equals(PreferenceScreen.class.getSimpleName())) {
                    return xml.getAttributeValue(APP_NS, "quick_restart");
                }
                eventType = xml.next();
            }
            return null;
        } catch (Throwable t) {
            AndroidLogUtils.logE(TAG, "Failed to access XML resource!", t);
            return null;
        }
    }

    protected void setOverlayMode() {
        try {
            Field declaredField = PreferenceFragment.class.getDeclaredField("mIsOverlayMode");
            declaredField.setAccessible(true);
            declaredField.set(this, Boolean.FALSE);
        } catch (Exception e) {
            Log.e("AboutFragment", "declaredField", e);
        }
    }
}
