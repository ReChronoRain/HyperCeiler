/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.ui.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.prefs.XmlPreference;
import com.sevtinge.hyperceiler.utils.SettingLauncherHelper;

import fan.appcompat.app.AppCompatActivity;

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
        mActivity.setContentView(R.layout.settings_sub);
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
    public void onStartSettingsForArguments(Class<?> cls, Preference preference, boolean isAddPreferenceKey) {
        Bundle args = null;

        if (isAddPreferenceKey) {
            args = new Bundle();
            args.putString("key", preference.getKey());
        }

        if (preference instanceof XmlPreference xmlPreference) {
            if (args == null) args = new Bundle();
            args.putInt(":settings:fragment_resId", xmlPreference.getInflatedXml());
        } else {
            Intent intent = preference.getIntent();
            if (intent != null) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    String xmlPath = (String) bundle.get("inflatedXml");
                    if (!TextUtils.isEmpty(xmlPath)) {
                        if (args == null) args = new Bundle();
                        String[] split = xmlPath.split("\\/");

                        String[] split2 = split[2].split("\\.");
                        if (split.length == 3) {
                            args.putInt(":settings:fragment_resId", mActivity.getResources().getIdentifier(split2[0], split[1], mActivity.getPackageName()));
                        }
                    }
                }
            }
        }

        String mFragmentName = preference.getFragment();
        String mTitle = preference.getTitle().toString();
        SettingLauncherHelper.onStartSettingsForArguments(mActivity, cls, mFragmentName, args, mTitle);
    }
}
