package com.sevtinge.hyperceiler.ui.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.SettingLauncherHelper;

import moralnorm.appcompat.app.AppCompatActivity;
import moralnorm.preference.Preference;

public class SettingsProxy extends BaseSettingsProxy {

    public FragmentManager mFragmentManager;

    public SettingsProxy(AppCompatActivity activity) {
        mActivity = activity;
    }

    private void replaceFragment(Fragment fragment, String tag) {
        mFragmentManager.beginTransaction().replace(R.id.frame_content, fragment, tag).commit();
    }

    @Override
    public void setupContentView() {
        mActivity.setContentView(R.layout.settings_main);
    }

    @Override
    public void handleIntent(Intent intent) {

    }

    @Override
    public void initView(Bundle bundle) {
        mFragmentManager = mActivity.getSupportFragmentManager();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu) {

    }

    @Override
    public void onDestroyView() {

    }

    @Override
    public void onOptionsItemSelected(MenuItem menuItem) {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {

    }

    @Override
    public void onResume() {

    }

    @Override
    public String getInitialFragmentName(Intent intent) {
        return intent.getStringExtra(":settings:show_fragment");
    }

    @Override
    public Fragment getTargetFragment(Context context, String initialFragmentName, Bundle savedInstanceState) {
        try {
            return Fragment.instantiate(context, initialFragmentName, savedInstanceState);
        } catch (Exception e) {
            Log.e("Settings", "Unable to get target fragment", e);
            return null;
        }
    }

    @Override
    public Bundle getArguments(Intent intent) {
        Bundle args = intent.getBundleExtra(":settings:show_fragment_args");
        String showFragmentTitle = intent.getStringExtra(":settings:show_fragment_title");
        int showFragmentTitleResId = intent.getIntExtra(":settings:show_fragment_title_resid", 0);
        args.putString(":fragment:show_title", showFragmentTitle);
        args.putInt(":fragment:show_title_resid", showFragmentTitleResId);
        return args;
    }

    @Override
    public void onStartSettingsForArguments(Class<?> cls, Preference preference, boolean isEnableBundle) {
        Bundle args = null;
        if (isEnableBundle) {
            args = new Bundle();
            args.putString("key", preference.getKey());
        }
        String mFragmentName = preference.getFragment();
        String mTitle = preference.getTitle().toString();
        SettingLauncherHelper.onStartSettingsForArguments(mActivity, cls, mFragmentName, args, mTitle);
    }
}
