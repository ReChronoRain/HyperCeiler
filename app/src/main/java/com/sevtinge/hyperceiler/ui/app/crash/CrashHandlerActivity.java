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
package com.sevtinge.hyperceiler.ui.app.crash;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.hchen.hooktool.log.AndroidLog;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import fan.appcompat.app.AlertDialog;
import fan.appcompat.app.AppCompatActivity;

public class CrashHandlerActivity extends AppCompatActivity {
    private static final String TAG = "CrashHandlerActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String crashInfo = getIntent().getStringExtra("crashInfo");

        new AlertDialog.Builder(this)
            .setTitle("错误")
            .setMessage("模块发生致命崩溃事件，无法继续运行！请携带以下报错信息进行反馈！\n" + crashInfo)
            .setPositiveButton("结束进程", (d, w) -> {
                AndroidLog.logI(TAG, "kill myself!!");
                moveTaskToBack(true);
                finish();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    ShellInit.getShell().run("am force-stop com.sevtinge.hyperceiler").sync();
                }, 500);
            })
            .setCancelable(false)
            .setHapticFeedbackEnabled(true)
            .show();
    }
}
