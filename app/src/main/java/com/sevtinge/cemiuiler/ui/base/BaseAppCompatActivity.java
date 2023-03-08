package com.sevtinge.cemiuiler.ui.base;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.GlobalActions;
import com.sevtinge.cemiuiler.utils.Helpers;

import moralnorm.appcompat.app.AlertDialog;
import moralnorm.appcompat.app.AppCompatActivity;
import moralnorm.internal.utils.ViewUtils;

public abstract class BaseAppCompatActivity extends AppCompatActivity {

    ImageView mRestartView;
    OnRestartListener mOnRestartListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(ViewUtils.isNightMode(this) ? R.style.AppTheme_Dark : R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setActionBarEndViewEnable(false);
        if (initFragment() != null ) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, initFragment())
                    .commit();
        }
        showXposedDialog();

    }

    public abstract Fragment initFragment();

    public void setActionBarEndViewEnable(boolean isEnable) {
        if (isEnable) {
            mRestartView = new ImageView(this);
            mRestartView.setImageResource(R.drawable.ic_reboot_small);
            mRestartView.setOnClickListener(v -> mOnRestartListener.onRestart());
            getAppCompatActionBar().setEndView(mRestartView);
        }
    }


    public void showRestartSystemUIDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.restart_systemui)
                .setMessage(R.string.restart_systemui_desc)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> restartSystemUI())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void restartSystemUI() {
        sendBroadcast(new Intent(GlobalActions.ACTION_PREFIX + "RestartSystemUI"));
    }

    public void showRestartAppsDialog(String appLabel, String packagename) {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.restart_app + appLabel)
                .setMessage(R.string.restart_app_desc1 + appLabel + R.string.restart_app_desc2)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> setRestartApps(packagename))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void setRestartApps(String packagename) {
        Intent intent = new Intent(GlobalActions.ACTION_PREFIX + "RestartApps");
        intent.putExtra("packageName", packagename);
        sendBroadcast(intent);
    }

    public void startActivity(AppCompatActivity activity,Class<?> cls) {
        startActivity(new Intent(activity,cls));
    }

    public void showXposedDialog() {
        if (!Helpers.isModuleActive) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.tip)
                    .setMessage(R.string.hook_failed)
                    .setHapticFeedbackEnabled(true)
                    .setPositiveButton(android.R.string.ok ,(dialog, which) -> finish())
                    .show();
        }
    }

    public void setOnRestartListener(OnRestartListener listener) {
        mOnRestartListener = listener;
    }

    public interface OnRestartListener {
        void onRestart();
    }
}
