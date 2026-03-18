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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sevtinge.hyperceiler.common.utils.CrashIntentContract;

public class CrashReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String alias = intent.getStringExtra(CrashIntentContract.KEY_PKG_ALIAS);
        CrashRecordStore.CrashRecord record = HookCrashHandler.persist(context, intent);
        HookCrashHandler.ensureSafeModeProp(record, alias);
        HookCrashHandler.showCrashToast(context, record, alias);

        Intent activityIntent = HookCrashHandler.createCrashActivityIntent(context, intent, record);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        HookCrashHandler.launchCrashActivity(context, activityIntent);
    }
}
