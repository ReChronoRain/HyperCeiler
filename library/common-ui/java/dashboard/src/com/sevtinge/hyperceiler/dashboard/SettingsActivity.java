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

import static com.sevtinge.hyperceiler.safemode.CrashHandlerReceiver.CRASH_HANDLER;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sevtinge.hyperceiler.dashboard.base.activity.BaseSettingsActivity;

import com.sevtinge.hyperceiler.safemode.CrashHandlerDialog;
import com.sevtinge.hyperceiler.safemode.CrashHandlerReceiver;
import com.sevtinge.hyperceiler.ui.hooker.framework.OtherSettings;
import com.sevtinge.hyperceiler.ui.hooker.home.HomeDockSettings;
import com.sevtinge.hyperceiler.ui.hooker.home.HomeFolderSettings;
import com.sevtinge.hyperceiler.ui.hooker.home.HomeGestureSettings;
import com.sevtinge.hyperceiler.ui.hooker.various.AlertDialogSettings;
import com.sevtinge.hyperceiler.ui.sub.MultiActionSettings;

import fan.preference.PreferenceFragment;

public abstract class SettingsActivity extends BaseSettingsActivity implements PreferenceFragment.OnPreferenceStartFragmentCallback {
    private CrashHandlerReceiver mCrashHandlerBroadcastReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCrashHandlerBroadcastReceiver = new CrashHandlerReceiver();
        IntentFilter intentFilter = new IntentFilter(CRASH_HANDLER);
        registerReceiver(mCrashHandlerBroadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);

        initCreate();
    }

    public void initCreate() {
    }

    public void onStartSettingsForArguments(Preference preference, boolean isAddPreferenceKey) {
        mProxy.onStartSettingsForArguments(SubSettings.class, preference, isAddPreferenceKey);
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        boolean isAddPreferenceKey = caller instanceof OtherSettings ||
            caller instanceof HomeDockSettings ||
            caller instanceof HomeFolderSettings ||
            caller instanceof AlertDialogSettings ||
            caller instanceof HomeGestureSettings ||
            caller instanceof MultiActionSettings;

        onStartSettingsForArguments(pref, isAddPreferenceKey);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mCrashHandlerBroadcastReceiver);
    }
}
