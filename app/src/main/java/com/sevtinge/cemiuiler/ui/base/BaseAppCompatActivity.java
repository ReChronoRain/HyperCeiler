package com.sevtinge.cemiuiler.ui.base;

import static com.sevtinge.cemiuiler.utils.KotlinXposedHelperKt.exec;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.GlobalActions;
import com.sevtinge.cemiuiler.ui.main.base.BaseMainActivity;
import com.sevtinge.cemiuiler.utils.Helpers;

import moralnorm.appcompat.app.AlertDialog;
import moralnorm.appcompat.app.AppCompatActivity;
import moralnorm.appcompat.internal.view.SearchActionMode;
import moralnorm.internal.utils.ViewUtils;

public abstract class BaseAppCompatActivity extends AppCompatActivity {

    ImageView mRestartView;
    OnRestartListener mOnRestartListener;

    ViewGroup mSearchView;
    TextWatcher mTextWatcher;
    TextView mSearchInputView;
    SearchActionMode mSearchActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(ViewUtils.isNightMode(this) ? R.style.AppTheme_Dark : R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setActionBarEndViewEnable(false);
        if (initFragment() != null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, initFragment())
                .commit();
        }
        showXposedDialog();
        initView();
    }

    private void initView() {
        mSearchView = findViewById(R.id.search_view);
        mSearchInputView = findViewById(android.R.id.input);

        mSearchInputView.setHint(R.string.search);
        mSearchView.setVisibility(this instanceof BaseMainActivity ? View.VISIBLE : View.GONE);
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

    public void showRestartSystemDialog() {
        new AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(R.string.soft_reboot)
            .setMessage(R.string.soft_reboot_desc)
            .setHapticFeedbackEnabled(true)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> exec("reboot"))
            .setNegativeButton(android.R.string.cancel, null)
            .show();
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
            .setTitle(getResources().getString(R.string.soft_reboot) + appLabel)
            .setMessage(getResources().getString(R.string.restart_app_desc1) + appLabel + getResources().getString(R.string.restart_app_desc2))
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

    public void startActivity(AppCompatActivity activity, Class<?> cls) {
        startActivity(new Intent(activity, cls));
    }

    public void showXposedDialog() {
        if (!Helpers.isModuleActive) {
            new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.tip)
                .setMessage(R.string.hook_failed)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .show();
        }
    }

    public void setOnRestartListener(OnRestartListener listener) {
        mOnRestartListener = listener;
    }

    public interface OnRestartListener {
        void onRestart();
    }

    private void startSearchMode(SearchActionMode.Callback callback) {
        SearchActionMode startActionMode = (SearchActionMode) startActionMode(callback);
        if (startActionMode != null) {
            mSearchActionMode = startActionMode;
            return;
        }
        throw new NullPointerException("null cannot be cast to non-null type SearchActionMode");
    }

    private void exitSearchMode() {
        if (mSearchActionMode != null) {
            mSearchActionMode = null;
        }
    }
}
