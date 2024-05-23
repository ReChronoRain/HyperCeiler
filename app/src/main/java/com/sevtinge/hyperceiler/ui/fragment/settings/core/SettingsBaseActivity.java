package com.sevtinge.hyperceiler.ui.fragment.settings.core;

import android.app.ActivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;

import fan.appcompat.app.AppCompatActivity;

/** Base activity for Settings pages */
public class SettingsBaseActivity extends AppCompatActivity {

    /**
     * The metrics category constant for logging source when a setting fragment is opened.
     */
    public static final String EXTRA_SOURCE_METRICS_CATEGORY = ":settings:source_metrics";

    /**
     * What type of page transition should be apply.
     */
    public static final String EXTRA_PAGE_TRANSITION_TYPE = "page_transition_type";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }
        super.setContentView(R.layout.settings_base_layout);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        final ViewGroup parent = findViewById(R.id.content_frame);
        if (parent != null) {
            parent.removeAllViews();
        }
        LayoutInflater.from(this).inflate(layoutResID, parent);
    }

    @Override
    public void setContentView(View view) {
        ((ViewGroup) findViewById(R.id.content_frame)).addView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        ((ViewGroup) findViewById(R.id.content_frame)).addView(view, params);
    }

    private boolean isSettingsRunOnTop() {
        final ActivityManager activityManager =
                getApplicationContext().getSystemService(ActivityManager.class);
        final String taskPkgName = activityManager.getRunningTasks(1 /* maxNum */)
                .get(0 /* index */).baseActivity.getPackageName();
        return TextUtils.equals(getPackageName(), taskPkgName);
    }
}
