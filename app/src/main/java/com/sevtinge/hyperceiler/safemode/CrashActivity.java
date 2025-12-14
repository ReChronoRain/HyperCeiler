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
import com.sevtinge.hyperceiler.hook.safe.CrashData;
import com.sevtinge.hyperceiler.hook.safe.SafeMode;
import com.sevtinge.hyperceiler.hook.utils.PropUtils;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellInit;

import java.util.HashMap;
import java.util.Map;

import fan.appcompat.app.AppCompatActivity;

public class CrashActivity extends AppCompatActivity {

    TextView mMessageTv;
    TextView mCrashRecordTv;
    private String longMsg;
    private String stackTrace;
    private String throwClassName;
    private String throwFileName;
    private int throwLineNumber;
    private String throwMethodName;

    private static Map<String, String> swappedMap = CrashData.swappedData();

    @SuppressLint({"SetTextI18n", "StringFormatInvalid"})
    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        ShellInit.init();
        if (swappedMap == null || swappedMap.isEmpty()) {
            swappedMap = CrashData.swappedData();
        }
        setContentView(R.layout.activity_crash_dialog);

        Intent intent = getIntent();
        String code = intent.getStringExtra("key_pkg");
        boolean isNeedSetProp = intent.getBooleanExtra("key_is_need_set_prop", false);
        if (isNeedSetProp) {
            PropUtils.setProp(SafeMode.PROP_REPORT_PACKAGE, code);
        }

        longMsg = intent.getStringExtra("key_longMsg");
        stackTrace = intent.getStringExtra("key_stackTrace");
        throwClassName = intent.getStringExtra("key_throwClassName");
        throwFileName = intent.getStringExtra("key_throwFileName");
        throwLineNumber = intent.getIntExtra("key_throwLineNumber", -1);
        throwMethodName = intent.getStringExtra("key_throwMethodName");

        Map<String, String> appNameMap = getAppNameMap();
        String pkg = getReportCrashPkg(code);
        String appName = appNameMap.getOrDefault(pkg, pkg != null ? pkg : "unknown");
        String msg = formatSafeModeDesc(appName, pkg);

        View view = LayoutInflater.from(this).inflate(R.layout.crash_report_dialog, null);
        mMessageTv = view.findViewById(R.id.tv_message);
        mMessageTv.setText(msg);

        mCrashRecordTv = view.findViewById(R.id.tv_record);
        Paint paint = mCrashRecordTv.getPaint();
        paint.setFlags(Paint.UNDERLINE_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        paint.setAntiAlias(true);

        mCrashRecordTv.setOnClickListener(v -> DialogHelper.showCrashMsgDialog(
                this, throwClassName, throwFileName, throwLineNumber, throwMethodName, longMsg, stackTrace
        ));

        DialogHelper.showCrashReportDialog(this, view);
    }

    private Map<String, String> getAppNameMap() {
        Map<String, String> map = new HashMap<>();
        map.put("com.android.systemui", getString(R.string.system_ui));
        map.put("com.android.settings", getString(R.string.system_settings));
        map.put("com.miui.home", getString(R.string.mihome));
        map.put("com.hchen.demo", getString(R.string.demo));
        map.put("com.miui.securitycenter", getString(R.string.security_center_hyperos));
        return map;
    }

    private String formatSafeModeDesc(String appName, String pkg) {
        String msg = getString(R.string.safe_mode_desc, " " + appName + " (" + pkg + ") ");
        return msg.replace("  ", " ")
                  .replace("， ", "，")
                  .replace("、 ", "、")
                  .replaceAll("^\\s+|\\s+$", "");
    }

    private String getReportCrashPkg(String data) {
        if (data == null || swappedMap == null) return null;
        String[] sp = data.split(",");
        StringBuilder result = new StringBuilder();
        for (String s : sp) {
            String mPkg = swappedMap.get(s);
            if (mPkg != null) {
                if (!result.isEmpty()) result.append("\n");
                result.append(mPkg);
            }
        }
        return result.isEmpty() ? null : result.toString();
    }

    @Override
    public void onDestroy() {
        ShellInit.destroy();
        super.onDestroy();
    }
}
