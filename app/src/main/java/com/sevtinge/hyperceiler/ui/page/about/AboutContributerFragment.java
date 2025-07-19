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
package com.sevtinge.hyperceiler.ui.page.about;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.sevtinge.hyperceiler.common.data.Contributors;
import com.sevtinge.hyperceiler.common.data.GitHubUser;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.ui.R;

import java.util.Collections;
import java.util.List;

public class AboutContributerFragment extends SettingsPreferenceFragment {
    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_about_contributor;
    }
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        PreferenceCategory category = findPreference("contributor_PreferenceCategory_base");
        if (category == null) return;

        Context context = requireContext();

        List<GitHubUser> contributors =  Contributors.INSTANCE.getLIST();

        for (GitHubUser contributor : contributors) {
            Preference pref = new Preference(context);
            pref.setTitle(contributor.getName());
            pref.setSummary("Github@" + contributor.getLogin());
            pref.setIcon(ContextCompat.getDrawable(context, contributor.getAvatar()));
            pref.setLayoutResource(R.layout.preference_round_layout); // 如果你使用了自定义布局
            pref.setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/" + contributor.getLogin())));

            category.addPreference(pref);
        }
    }
}
