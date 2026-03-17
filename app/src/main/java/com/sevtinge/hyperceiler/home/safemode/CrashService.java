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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.home.safemode;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.utils.CrashIntentContract;
import com.sevtinge.hyperceiler.common.utils.shell.ShellInit;

public class CrashService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ShellInit.init();
    }

    @SuppressLint("UnsafeIntentLaunch")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return super.onStartCommand(null, flags, startId);
        }

        String alias = intent.getStringExtra(CrashIntentContract.KEY_PKG_ALIAS);
        CrashRecordStore.CrashRecord record = HookCrashHandler.persist(this, intent);
        HookCrashHandler.ensureSafeModeProp(record, alias);
        HookCrashHandler.showCrashToast(this, record, alias);

        Intent activityIntent = HookCrashHandler.createCrashActivityIntent(this, intent, record);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        HookCrashHandler.launchCrashActivity(this, activityIntent);

        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        ShellInit.destroy();
        super.onDestroy();
    }
}
