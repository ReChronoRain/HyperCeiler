/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.safemode;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.safemode.widget.CrashInfoItem;
import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.BuildConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import fan.appcompat.app.AppCompatActivity;

public class ExceptionCrashActivity extends AppCompatActivity implements View.OnLongClickListener {
    private String buildMsg;
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

        buildMsg = getString(R.string.error_version_name) + ": "  + BuildConfig.VERSION_NAME +
            "\n" + getString(R.string.error_git_hash) + ": "  + BuildConfig.GIT_HASH +
            "\n" + getString(R.string.error_build_time) + ": "  + BuildConfig.BUILD_TIME;

        fullMsg = getString(R.string.error_message) + ": "  + message +
            "\n" + getString(R.string.error_type) + ": "  + exceptionType +
            "\n" + getString(R.string.error_file_name) + ": "  + fileName +
            "\n" + getString(R.string.safe_mode_recorder_class) + ": "  + className +
            "\n" + getString(R.string.safe_mode_recorder_method) + ": "  + methodName +
            "\n" + getString(R.string.safe_mode_recorder_line) + ": "  + lineNumber +
            "\n" + getString(R.string.error_time) + ": " + timestamp;

        CrashInfoItem versionNameView = findViewById(R.id.version_name);
        versionNameView.setValue(BuildConfig.VERSION_NAME);
        CrashInfoItem gitHashView = findViewById(R.id.git_hash);
        gitHashView.setValue(BuildConfig.GIT_HASH);
        CrashInfoItem buildTimeView = findViewById(R.id.build_time);
        buildTimeView.setValue(BuildConfig.BUILD_TIME);

        CrashInfoItem msgView = findViewById(R.id.message);
        msgView.setValue(message);

        CrashInfoItem typeView = findViewById(R.id.e_type);
        typeView.setValue(exceptionType);
        CrashInfoItem filenameView = findViewById(R.id.filename);
        filenameView.setValue(fileName);
        CrashInfoItem classnameView = findViewById(R.id.classname);
        classnameView.setValue(className);
        CrashInfoItem methodnameView = findViewById(R.id.methodname);
        methodnameView.setValue(methodName);
        CrashInfoItem lineView = findViewById(R.id.e_line);
        lineView.setValue(String.valueOf(lineNumber));
        CrashInfoItem timeView = findViewById(R.id.e_time);
        timeView.setValue(timestamp.toString());

        TextView stackView = findViewById(R.id.stack);
        stackMsg = fullDetail;
        stackView.setText(stackMsg);

        LinearLayout buildViews = findViewById(R.id.build_views);
        LinearLayout messageViews = findViewById(R.id.message_views);

        buildViews.setOnLongClickListener(this);
        messageViews.setOnLongClickListener(this);
        stackView.setOnLongClickListener(this);
    }

    @Override
    public boolean onLongClick(View v) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("text", buildMsg + "\n----------\n" + fullMsg + "\n----------\n" + stackMsg);
        cm.setPrimaryClip(clip);
        Toast.makeText(v.getContext(), getString(R.string.copy_ok), Toast.LENGTH_SHORT).show();

        return true;
    }
}
