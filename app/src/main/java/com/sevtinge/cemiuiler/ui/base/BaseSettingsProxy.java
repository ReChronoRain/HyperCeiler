package com.sevtinge.cemiuiler.ui.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;

import moralnorm.appcompat.app.AppCompatActivity;
import moralnorm.preference.Preference;

public abstract class BaseSettingsProxy {

    AppCompatActivity mActivity;

    public abstract void setupContentView();

    public abstract void handleIntent(Intent intent);

    public abstract void initView(Bundle bundle);

    public abstract boolean onBackPressed();

    public abstract void onCreateOptionsMenu(Menu menu);

    public abstract void onDestroyView();

    public abstract void onOptionsItemSelected(MenuItem menuItem);

    public abstract void onPause();

    public abstract void onPrepareOptionsMenu(Menu menu);

    public abstract void onResume();

    public abstract String getInitialFragmentName(Intent intent);

    public abstract Fragment getTargetFragment(Context context, String initialFragmentName, Bundle savedInstanceState);

    public abstract Bundle getArguments(Intent intent);

    public abstract void onStartSettingsForArguments(Class<?> cls, Preference preference, boolean isEnableBundle);
}
