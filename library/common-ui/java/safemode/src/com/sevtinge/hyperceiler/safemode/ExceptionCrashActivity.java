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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.ui.R;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import fan.appcompat.app.AppCompatActivity;

public class ExceptionCrashActivity extends AppCompatActivity implements View.OnLongClickListener {
    private String fullMsg;
    private String stackMsg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);
        getAppCompatActionBar().setTitle(getString(R.string.error));

        Intent intent = getIntent();
        Throwable throwable = intent.getSerializableExtra("crashInfo", Throwable.class);
        if (throwable == null) return;

        String message = throwable.getMessage();
        String exceptionType = throwable.getClass().getName();
        StackTraceElement element = throwable.getStackTrace()[0];
        String fileName = element.getFileName();
        String className = element.getClassName();
        String methodName = element.getMethodName();
        int lineNumber = element.getLineNumber();
        Date timestamp = new Date();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String fullDetail = sw.toString();

        TextView msgView = findViewById(R.id.message);
        fullMsg = getString(R.string.error_message) + ": "  + message +
            "\n\n" + getString(R.string.error_type) + ": "  + exceptionType +
            "\n\n" + getString(R.string.error_file_name) + ": "  + fileName +
            "\n\n" + getString(R.string.safe_mode_recorder_class) + ": "  + className +
            "\n\n" + getString(R.string.safe_mode_recorder_method) + ": "  + methodName +
            "\n\n" + getString(R.string.safe_mode_recorder_line) + ": "  + lineNumber +
            "\n\n" + getString(R.string.error_time) + ": " + timestamp;
        msgView.setText(fullMsg);

        TextView stackView = findViewById(R.id.stack);
        stackMsg = fullDetail;
        stackView.setText(stackMsg);

        msgView.setOnLongClickListener(this);
        stackView.setOnLongClickListener(this);
    }

    @Override
    public boolean onLongClick(View v) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("text", fullMsg + "\n---------------------------------------\n" + stackMsg);
        cm.setPrimaryClip(clip);
        Toast.makeText(v.getContext(), getString(R.string.copy_ok), Toast.LENGTH_SHORT).show();

        return true;
    }
}
