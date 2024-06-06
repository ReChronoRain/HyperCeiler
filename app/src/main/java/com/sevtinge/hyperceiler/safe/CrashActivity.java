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
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.DialogHelper;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import moralnorm.appcompat.app.AppCompatActivity;

public class CrashActivity extends AppCompatActivity {

    TextView mMessageTv;
    TextView mCrashRecordTv;
    private String longMsg;
    private String stackTrace;
    private String throwClassName;
    private String throwFileName;
    private int throwLineNumber;
    private String throwMethodName;

    private static HashMap<String, String> swappedMap = CrashData.swappedData();

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        ShellInit.init();
        if (swappedMap.isEmpty()) swappedMap = CrashData.swappedData();
        setContentView(R.layout.activity_crash_dialog);
        Intent intent = getIntent();
        String code = intent.getStringExtra("key_pkg");
        longMsg = intent.getStringExtra("key_longMsg");
        stackTrace = intent.getStringExtra("key_stackTrace");
        throwClassName = intent.getStringExtra("key_throwClassName");
        throwFileName = intent.getStringExtra("key_throwFileName");
        throwLineNumber = intent.getIntExtra("key_throwLineNumber", -1);
        throwMethodName = intent.getStringExtra("key_throwMethodName");
        String pkg = getReportCrashPkg(code);
        View view = LayoutInflater.from(this).inflate(R.layout.crash_report_dialog, null);
        mMessageTv = view.findViewById(R.id.tv_message);
        mMessageTv.setText(pkg + " " + getString(R.string.safe_mode_desc));
        mCrashRecordTv = view.findViewById(R.id.tv_record);
        mCrashRecordTv.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);// 下划线并加清晰
        mCrashRecordTv.getPaint().setAntiAlias(true);
        mCrashRecordTv.setOnClickListener(v -> DialogHelper.showCrashMsgDialog(this, throwClassName,
                throwFileName, throwLineNumber, throwMethodName, longMsg, stackTrace));
        DialogHelper.showCrashReportDialog(this, view);
    }

    private String getReportCrashPkg(String data) {
        if (data == null) return null;
        String[] sp = data.split(",");
        ArrayList<String> report = new ArrayList<>(Arrays.asList(sp));
        StringBuilder string = null;
        for (String s : report) {
            String mPkg = swappedMap.get(s);
            if (mPkg != null) {
                if (string == null) string = new StringBuilder(mPkg);
                else
                    string.append("\n").append(mPkg);
            }
        }
        if (string == null) return null;
        return string.toString();
    }

    @Override
    protected void onDestroy() {
        ShellInit.destroy();
        super.onDestroy();
    }
}
