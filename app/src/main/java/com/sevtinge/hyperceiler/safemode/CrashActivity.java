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
package com.sevtinge.hyperceiler.safemode;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashScope;
import com.sevtinge.hyperceiler.libhook.safecrash.SafeModeHandler;
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellInit;

import fan.appcompat.app.AppCompatActivity;

public class CrashActivity extends AppCompatActivity {

    private static final String KEY_PKG_ALIAS = "key_pkg";
    private static final String KEY_IS_NEED_SET_PROP = "key_is_need_set_prop";
    private static final String KEY_LONG_MSG = "key_longMsg";
    private static final String KEY_STACK_TRACE = "key_stackTrace";
    private static final String KEY_THROW_CLASS = "key_throwClassName";
    private static final String KEY_THROW_FILE = "key_throwFileName";
    private static final String KEY_THROW_LINE = "key_throwLineNumber";
    private static final String KEY_THROW_METHOD = "key_throwMethodName";

    private String longMsg;
    private String stackTrace;
    private String throwClassName;
    private String throwFileName;
    private int throwLineNumber;
    private String throwMethodName;

    @SuppressLint({"SetTextI18n", "StringFormatInvalid"})
    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        ShellInit.init();
        setContentView(R.layout.activity_crash_dialog);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        String alias = intent.getStringExtra(KEY_PKG_ALIAS);

        boolean isNeedSetProp = intent.getBooleanExtra(KEY_IS_NEED_SET_PROP, false);
        if (isNeedSetProp && alias != null) {
            SafeModeHandler.INSTANCE.updateCrashProp(alias);
        }

        longMsg = intent.getStringExtra(KEY_LONG_MSG);
        stackTrace = intent.getStringExtra(KEY_STACK_TRACE);
        throwClassName = intent.getStringExtra(KEY_THROW_CLASS);
        throwFileName = intent.getStringExtra(KEY_THROW_FILE);
        throwLineNumber = intent.getIntExtra(KEY_THROW_LINE, -1);
        throwMethodName = intent.getStringExtra(KEY_THROW_METHOD);

        String realPkgName = CrashScope.INSTANCE.getPackageName(alias);
        if (realPkgName == null) realPkgName = alias != null ? alias : "unknown";

        String appName = getAppName(realPkgName);
        String msg = formatSafeModeDesc(appName, realPkgName);

        View view = LayoutInflater.from(this).inflate(R.layout.crash_report_dialog, null);
        TextView mMessageTv = view.findViewById(R.id.tv_message);
        mMessageTv.setText(msg);

        TextView mCrashRecordTv = view.findViewById(R.id.tv_record);
        Paint paint = mCrashRecordTv.getPaint();
        paint.setFlags(Paint.UNDERLINE_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        paint.setAntiAlias(true);

        mCrashRecordTv.setOnClickListener(v -> DialogHelper.showCrashMsgDialog(
            this, throwClassName, throwFileName, throwLineNumber, throwMethodName, longMsg, stackTrace
        ));

        DialogHelper.showCrashReportDialog(this, view);
    }

    private String getAppName(String pkg) {
        if ("com.android.systemui".equals(pkg)) return getString(R.string.system_ui);
        if ("com.android.settings".equals(pkg)) return getString(R.string.system_settings);
        if ("com.miui.home".equals(pkg)) return getString(R.string.mihome);
        if ("com.hchen.demo".equals(pkg)) return getString(R.string.demo);
        if ("com.miui.securitycenter".equals(pkg)) return getString(R.string.security_center_hyperos);
        return pkg;
    }

    private String formatSafeModeDesc(String appName, String pkg) {
        String msg = getString(R.string.safe_mode_desc, " " + appName + " (" + pkg + ") ");
        return msg.replace("  ", " ")
            .replace("， ", "，")
            .replace("、 ", "、")
            .trim();
    }

    @Override
    public void onDestroy() {
        ShellInit.destroy();
        super.onDestroy();
    }
}
