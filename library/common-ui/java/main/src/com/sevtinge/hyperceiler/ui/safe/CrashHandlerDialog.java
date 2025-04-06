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
package com.sevtinge.hyperceiler.ui.safe;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.hchen.hooktool.log.AndroidLog;
import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellInit;

import fan.appcompat.app.AlertDialog;

public class CrashHandlerDialog {
    private static final String TAG = "CrashHandlerDialog";

    CrashHandlerDialog(Context context, Intent intent) {
        String crashInfo = intent.getStringExtra("crashInfo");

        new AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.error))
            .setMessage(context.getString(R.string.app_error_desc) + crashInfo)
            .setPositiveButton(context.getString(R.string.app_exit), (d, w) -> {
                AndroidLog.logI(TAG, "kill myself!!");
                ((Activity) context).moveTaskToBack(true);
                ((Activity) context).finish();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    ShellInit.getShell().run("am force-stop com.sevtinge.hyperceiler").sync();
                }, 500);
            })
            .setCancelable(false)
            .setHapticFeedbackEnabled(true)
            .show();
    }

    public static class CrashHandlerBroadcastReceiver extends BroadcastReceiver {
        public static final String CRASH_HANDLER = "com.sevtinge.hyperceiler.CrashHandler";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (CRASH_HANDLER.equals(intent.getAction())) {
                new CrashHandlerDialog(context, intent);
            }
        }
    }
}
