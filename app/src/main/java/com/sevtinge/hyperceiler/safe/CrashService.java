package com.sevtinge.hyperceiler.safe;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

public class CrashService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ShellInit.init();
        AndroidLogUtils.logI(ITAG.TAG, "service create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AndroidLogUtils.logI(ITAG.TAG, "service onStartCommand");
        String report = intent.getStringExtra("key_report");
        String stackTrace = intent.getStringExtra("key_stackTrace");
        String longMsg = intent.getStringExtra("key_longMsg");
        ShellInit.getShell().run("setprop persist.hyperceiler.crash.report " + "\"" + report + "\"").sync();
        Intent intent1 = new Intent(this, CrashReportActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent1.putExtra("key_report", report);
        intent1.putExtra("key_stackTrace", stackTrace);
        intent1.putExtra("key_longMsg", longMsg);
        startActivity(intent1);
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        AndroidLogUtils.logI(ITAG.TAG, "service onDestroy");
        ShellInit.destroy();
        super.onDestroy();
    }
}
