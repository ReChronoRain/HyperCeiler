package com.sevtinge.hyperceiler.safe;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.utils.DialogHelper;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import moralnorm.appcompat.app.AppCompatActivity;

public class CrashReportActivity extends AppCompatActivity {

    TextView mMessageTv;
    TextView mCrashRecordTv;

    private static HashMap<String, String> swappedMap = CrashData.swappedData();

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        ShellInit.init();
        if (swappedMap.isEmpty()) swappedMap = CrashData.swappedData();
        setContentView(com.sevtinge.hyperceiler.R.layout.activity_crash_dialog);
        Intent intent = getIntent();
        String code = intent.getStringExtra("key_report");
        String pkg = getReportCrashPkg(code);
        View view = LayoutInflater.from(this).inflate(com.sevtinge.hyperceiler.R.layout.crash_report_dialog, null);
        mMessageTv = view.findViewById(com.sevtinge.hyperceiler.R.id.tv_message);
        mMessageTv.setText("此应用进入安全模式:\n" + pkg + "\n点击确定取消");
        mCrashRecordTv = view.findViewById(com.sevtinge.hyperceiler.R.id.tv_record);
        mCrashRecordTv.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);// 下划线并加清晰
        mCrashRecordTv.getPaint().setAntiAlias(true);// 抗锯齿
        DialogHelper.showCrashReportDialog(this, pkg, view);
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
