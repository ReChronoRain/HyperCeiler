/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.dashboard;

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
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.ui.R;

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
        mQuickRestartPackageName = getQuickRestartPackageName(requireContext(), getPreferenceScreenResId());
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

    public void setFuncHint(Preference p, int value) {
        p.setEnabled(false);
        switch (value) {
            case 1 -> p.setSummary(R.string.unsupported_system_func);
            case 2 -> p.setSummary(R.string.supported_system_func);
            case 3 -> p.setSummary(R.string.feature_doing_func);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }
}
