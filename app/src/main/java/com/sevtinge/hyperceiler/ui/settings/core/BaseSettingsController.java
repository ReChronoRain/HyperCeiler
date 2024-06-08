package com.sevtinge.hyperceiler.ui.settings.core;

import android.content.Context;
import android.widget.TextView;

public abstract class BaseSettingsController {

    protected Context mContext;
    protected TextView mStatusView;
    protected UpdateCallback mUpdateCallback = null;

    public abstract void pause();

    public abstract void resume();

    protected void start() {}

    protected void stop() {}

    protected abstract void updateStatus();

    public BaseSettingsController(Context context, TextView textView) {
        mContext = context;
        mStatusView = textView;
    }

    public void setStatusView(TextView textView) {
        if (textView != null && mStatusView != textView) {
            BaseSettingsController controller = (BaseSettingsController) textView.getTag();
            if (controller != null) {
                controller.setStatusView(null);
            }
        }
        mStatusView = textView;
        updateStatus();
    }

    public void setUpdateCallback(UpdateCallback updateCallback) {
        mUpdateCallback = updateCallback;
    }

    public class UpdateCallback {

        public void updateText(String text) {
            throw null;
        }
    }
}
