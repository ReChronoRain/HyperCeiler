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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;
import androidx.core.view.MenuProvider;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.hook.utils.pkg.CheckModifyUtils;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Field;

import fan.preference.PreferenceFragment;

public class DashboardFragment extends SettingsPreferenceFragment {

    private static final String TAG = "DashboardFragment";
    private static final String APP_NS = "http://schemas.android.com/apk/res-auto";

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                if (!TextUtils.isEmpty(mQuickRestartPackageName)) {
                    menuInflater.inflate(R.menu.navigation_immersion, menu);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.quick_restart && !TextUtils.isEmpty(mQuickRestartPackageName)) {
                    if ("system".equals(mQuickRestartPackageName)) {
                        DialogHelper.showRestartSystemDialog(getContext());
                    } else {
                        DialogHelper.showRestartDialog(getContext(), mQuickRestartPackageName);
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    private String getQuickRestartPackageName(Context context, @XmlRes int xmlResId) {
        if (xmlResId == 0) return null;
        Resources res = context.getResources();
        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "PreferenceScreen".equals(xml.getName())) {
                    return xml.getAttributeValue(APP_NS, "quick_restart");
                }
                eventType = xml.next();
            }
        } catch (Exception e) {
            AndroidLogUtils.logE(TAG, "Failed to access XML resource!", e);
        }
        return null;
    }

    protected void setOverlayMode() {
        try {
            Field declaredField = PreferenceFragment.class.getDeclaredField("mIsOverlayMode");
            declaredField.setAccessible(true);
            declaredField.set(this, false);
        } catch (Exception e) {
            AndroidLogUtils.logE(TAG, "setOverlayMode error", e);
        }
    }

    public void setFuncHint(Preference p, int value) {
        cleanKey(p.getKey());
        p.setEnabled(false);
        switch (value) {
            case 1 -> p.setSummary(R.string.unsupported_system_func);
            case 2 -> p.setSummary(R.string.supported_system_func);
            case 3 -> p.setSummary(R.string.feature_doing_func);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        }
    }

    public void setHide(Preference p, boolean b) {
        if (!b) {
            cleanKey(p.getKey());
            p.setVisible(false);
        }
    }

    public void setAppModWarn(Preference p, String pkgName) {
        boolean check = CheckModifyUtils.INSTANCE.getCheckResult(getContext(), pkgName);
        boolean isDebugMode = getSharedPreferences().getBoolean("prefs_key_development_debug_mode", false);
        boolean isDebugVersion = getSharedPreferences().getInt("prefs_key_debug_choose_" + pkgName, 0) == 0;

        p.setVisible(check && !isDebugMode && isDebugVersion);
    }
}
