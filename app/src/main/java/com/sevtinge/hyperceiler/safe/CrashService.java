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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.safe;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.utils.shell.ShellInit;

public class CrashService extends Service {
    private String longMsg;
    private String stackTrace;
    private String throwClassName;
    private String throwFileName;
    private int throwLineNumber;
    private String throwMethodName;

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
        String report = intent.getStringExtra("key_all");
        String abbr = intent.getStringExtra("key_pkg");
        longMsg = intent.getStringExtra("key_longMsg");
        stackTrace = intent.getStringExtra("key_stackTrace");
        throwClassName = intent.getStringExtra("key_throwClassName");
        throwFileName = intent.getStringExtra("key_throwFileName");
        throwLineNumber = intent.getIntExtra("key_throwLineNumber", -1);
        throwMethodName = intent.getStringExtra("key_throwMethodName");
        ShellInit.getShell().run("setprop persist.hyperceiler.crash.report " + "\"" + report + "\"").sync();
        Intent intent1 = getIntent(abbr);
        startActivity(intent1);
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @NonNull
    private Intent getIntent(String abbr) {
        Intent intent1 = new Intent(this, CrashActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent1.putExtra("key_longMsg", longMsg);
        intent1.putExtra("key_stackTrace", stackTrace);
        intent1.putExtra("key_throwClassName", throwClassName);
        intent1.putExtra("key_throwFileName", throwFileName);
        intent1.putExtra("key_throwLineNumber", throwLineNumber);
        intent1.putExtra("key_throwMethodName", throwMethodName);
        intent1.putExtra("key_pkg", abbr);
        return intent1;
    }

    @Override
    public void onDestroy() {
        ShellInit.destroy();
        super.onDestroy();
    }
}
