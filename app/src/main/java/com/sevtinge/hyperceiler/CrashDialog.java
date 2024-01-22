package com.sevtinge.hyperceiler;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.utils.ShellUtils;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import moralnorm.appcompat.app.AlertDialog;
import moralnorm.appcompat.app.AppCompatActivity;


public class CrashDialog extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_crash_dialog);
        Intent intent = getIntent();
        String pkg = intent.getStringExtra("key_pkg");
        new AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("警告")
            .setMessage("此应用进入安全模式:\n" + pkg + "\n点击确定取消")
            .setHapticFeedbackEnabled(true)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ShellUtils.OpenShellExecWindow open = new ShellUtils.OpenShellExecWindow(
                        "setprop persist.hyperceiler.crash.report \"[]\"", true, true) {
                        @Override
                        public void readOutput(String out, String type) {
                            AndroidLogUtils.LogI(ITAG.TAG, "D O: " + out + " T: " + type);
                        }
                    };
                    open.append("settings put system hyperceiler_crash_report \"[]\"");
                    open.getResult();
                    open.close();
                    finish();
                }
            })
            .show();
    }
}
