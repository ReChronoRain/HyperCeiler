package com.sevtinge.cemiuiler.ui.base;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.MainActivity;
import com.sevtinge.cemiuiler.utils.Helpers;

import java.util.ArrayList;
import java.util.List;

import moralnorm.appcompat.app.AlertDialog;
import moralnorm.appcompat.app.AppCompatActivity;

public class BaseSettingsActivity extends AppCompatActivity {

    private String initialFragmentName;
    public BaseSettingsProxy mProxy;
    public static List<BaseSettingsActivity> mActivityList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        mProxy = new SettingsProxy(this);
        initialFragmentName = mProxy.getInitialFragmentName(intent);
        if (TextUtils.isEmpty(initialFragmentName)) {
            initialFragmentName = intent.getStringExtra(":android:show_fragment");
        }
        super.onCreate(savedInstanceState);
        createUiFromIntent(savedInstanceState, intent);
    }

    protected void createUiFromIntent(Bundle savedInstanceState, Intent intent) {
        mProxy.setupContentView();
        initActionBar();
        mActivityList.add(this);
        Fragment targetFragment = mProxy.getTargetFragment(this, initialFragmentName, savedInstanceState);
        if (targetFragment != null) {
            targetFragment.setArguments(mProxy.getArguments(intent));
            setFragment(targetFragment);
        }
        if (!(this instanceof MainActivity)) {
            findViewById(R.id.search_view).setVisibility(View.GONE);
        }
        showXposedActiveDialog();
    }

    private void initActionBar() {
        setDisplayHomeAsUpEnabled();
    }

    private void setDisplayHomeAsUpEnabled() {
        if (this instanceof MainActivity) {
            getAppCompatActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    public void showXposedActiveDialog() {
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

    public void setFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.frame_content, fragment)
            .commit();
    }
}
