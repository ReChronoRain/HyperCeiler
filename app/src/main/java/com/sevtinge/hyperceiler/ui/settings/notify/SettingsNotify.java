package com.sevtinge.hyperceiler.ui.settings.notify;

import android.app.Activity;
import android.content.Intent;

public class SettingsNotify {

    int notifyId;
    int shownResId;
    Intent targetIntent;

    public void setNotifyId(int notifyId) {
        this.notifyId = notifyId;
    }

    public void setShownResId(int shownResId) {
        this.shownResId = shownResId;
    }

    public void setTargetIntent(Intent intent) {
        this.targetIntent = intent;
    }

    public void goToTarget(Activity activity) {
        if (activity.getPackageManager().resolveActivity(targetIntent, 0) != null) {
            activity.startActivity(targetIntent);
        }
    }
}