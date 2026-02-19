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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class CrashReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String reportProp = intent.getStringExtra("key_all");
        String alias = intent.getStringExtra("key_pkg");

        /*// 如果有需要更新的属性，执行 Shell 命令
        if (reportProp != null && !reportProp.isEmpty()) {
            // 注意：在 Receiver 中执行耗时操作可能会 ANR，建议在 Service 或 Handler 中处理
            // 但为了保持原有逻辑，这里保留同步执行
            try {
                ShellInit.getShell().run("setprop persist.service.hyperceiler.crash.report " + "\"" + reportProp + "\"").sync();
            } catch (Throwable ignored) {
            }
        }*/

        if (alias != null) {
            Toast.makeText(context, "Crash detected: " + alias, Toast.LENGTH_LONG).show();
        }

        // 启动 Activity
        Intent activityIntent = createCrashActivityIntent(context, intent, alias);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);
    }

    @NonNull
    private Intent createCrashActivityIntent(Context context, Intent sourceIntent, String alias) {
        Intent intent = new Intent(context, CrashActivity.class);
        intent.setAction("android.intent.action.Crash");

        intent.putExtra("key_longMsg", sourceIntent.getStringExtra("key_longMsg"));
        intent.putExtra("key_stackTrace", sourceIntent.getStringExtra("key_stackTrace"));
        intent.putExtra("key_throwClassName", sourceIntent.getStringExtra("key_throwClassName"));
        intent.putExtra("key_throwFileName", sourceIntent.getStringExtra("key_throwFileName"));
        intent.putExtra("key_throwLineNumber", sourceIntent.getIntExtra("key_throwLineNumber", -1));
        intent.putExtra("key_throwMethodName", sourceIntent.getStringExtra("key_throwMethodName"));
        intent.putExtra("key_pkg", alias);

        return intent;
    }
}
