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
package com.sevtinge.hyperceiler.oldui.safemode;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellInit;

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

        String reportProp = intent.getStringExtra("key_all");
        String alias = intent.getStringExtra("key_pkg");

        /*// 执行 Shell 命令设置属性
        if (reportProp != null && !reportProp.isEmpty()) {
            try {
                ShellInit.getShell().run("setprop persist.service.hyperceiler.crash.report " + "\"" + reportProp + "\"").sync();
            } catch (Throwable ignored) {
            }
        }*/

        if (alias != null) {
            Toast.makeText(getBaseContext(), "Crash detected: " + alias, Toast.LENGTH_LONG).show();
        }

        // 启动 Activity
        Intent activityIntent = new Intent(this, CrashActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // 转发参数
        activityIntent.putExtra("key_longMsg", intent.getStringExtra("key_longMsg"));
        activityIntent.putExtra("key_stackTrace", intent.getStringExtra("key_stackTrace"));
        activityIntent.putExtra("key_throwClassName", intent.getStringExtra("key_throwClassName"));
        activityIntent.putExtra("key_throwFileName", intent.getStringExtra("key_throwFileName"));
        activityIntent.putExtra("key_throwLineNumber", intent.getIntExtra("key_throwLineNumber", -1));
        activityIntent.putExtra("key_throwMethodName", intent.getStringExtra("key_throwMethodName"));
        activityIntent.putExtra("key_pkg", alias);

        try {
            startActivity(activityIntent);
        } catch (Exception e) {
            AndroidLog.e("CrashService", "Failed to start CrashActivity: " + e.getMessage());
        }

        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        ShellInit.destroy();
        super.onDestroy();
    }
}
