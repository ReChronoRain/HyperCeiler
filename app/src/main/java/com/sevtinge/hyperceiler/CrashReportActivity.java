package com.sevtinge.hyperceiler;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.utils.DialogHelper;
import moralnorm.appcompat.app.AppCompatActivity;

public class CrashReportActivity extends AppCompatActivity {

    TextView mMessageTv;
    TextView mCrashRecordTv;

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_crash_dialog);
        Intent intent = getIntent();
        String pkg = intent.getStringExtra("key_pkg");
        View view = LayoutInflater.from(this).inflate(R.layout.crash_report_dialog, null);
        mMessageTv = view.findViewById(R.id.tv_message);
        mMessageTv.setText("此应用进入安全模式:\n" + pkg + "\n点击确定取消");
        mCrashRecordTv = view.findViewById(R.id.tv_record);
        mCrashRecordTv.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);//下划线并加清晰
        mCrashRecordTv.getPaint().setAntiAlias(true);//抗锯齿
        DialogHelper.showCrashReportDialog(this, pkg, view);
    }
}
