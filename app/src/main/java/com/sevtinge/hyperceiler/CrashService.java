package com.sevtinge.hyperceiler;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.utils.ShellUtils;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

public class CrashService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidLogUtils.LogI(ITAG.TAG, "service create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AndroidLogUtils.LogI(ITAG.TAG, "service onStartCommand");
        String cover = intent.getStringExtra("key_cover");
        boolean equal = intent.getBooleanExtra("key_equal", true);
        ShellUtils.OpenShellExecWindow open = new ShellUtils.OpenShellExecWindow(
            "setprop persist.hyperceiler.crash.report " + "\"" + cover + "\"",
            true, true) {
            @Override
            public void readOutput(String out, String type) {
                AndroidLogUtils.LogE(ITAG.TAG, "S O: " + out + " T: " + type, null);
            }
        };
        int re = open.getResult();
        open.close();
        AndroidLogUtils.LogI(ITAG.TAG, "R: " + re + " C: " + cover + " E: " + equal);
        if (equal) {
            Intent intent1 = new Intent(this, CrashReportActivity.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent1.putExtra("key_pkg", cover);
            startActivity(intent1);
        }
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        AndroidLogUtils.LogI(ITAG.TAG, "service onDestroy");
        super.onDestroy();
    }
}
