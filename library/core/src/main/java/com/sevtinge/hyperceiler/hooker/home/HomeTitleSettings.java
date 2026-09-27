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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hooker.home;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.sub.SubPickerActivity.INPUT_MODE;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.sub.SubPickerActivity;

public class HomeTitleSettings extends DashboardFragment {

    SwitchPreference mDisableMonoChrome;
    SwitchPreference mDisableMonetColor;
    SwitchPreference mDisableHideTheme;
    Preference mIconTitleCustomization;
    RecommendPreference mRecommend;

    @Override
    public int getPreferenceScreenResId() {
        if (isMoreHyperOSVersion(3f)) return R.xml.home_title_new;
        return R.xml.home_title;
    }

    @Override
    public void initPrefs() {
        mDisableMonoChrome = findPreference("prefs_key_home_other_icon_mono_chrome");

        mDisableMonoChrome.setOnPreferenceChangeListener((preference, o) -> true);
        mDisableMonetColor = findPreference("prefs_key_home_other_icon_monet_color");
        mDisableMonetColor.setOnPreferenceChangeListener((preference, o) -> true);
        mDisableHideTheme = findPreference("prefs_key_home_title_disable_hide_theme");

        mIconTitleCustomization = findPreference("prefs_key_home_title_title_icontitlecustomization");
        mIconTitleCustomization.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("mode", INPUT_MODE);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });

        setPreVisible(mDisableHideTheme, isPad());

        // HyperOS 4 起图标大小改由「布局」页的 home_layout_icon_scale 接管：旧的
        // home_title_icon_size 只在 os2/os3 的 hook 里被消费（见 Home/os3/HomePhone），
        // 在 OS4 上它已经不起作用，留着只会给出一个无效入口。
        if (isMoreHyperOSVersion(4f)) {
            moveIconSizeToLayout();
        }

        Bundle args1 = new Bundle();
        Bundle args2 = new Bundle();
        mRecommend = new RecommendPreference(requireContext());
        getPreferenceScreen().addPreference(mRecommend);

        args2.putString(":settings:fragment_args_key", "prefs_key_home_other_all_hide_app_activity");
        mRecommend.addRecommendView(getString(R.string.home_other_app_icon_hide),
            null,
            HomeOtherSettings.class,
            args2,
            R.string.home_other
        );

        if (!isMoreHyperOSVersion(3f)) {
            args1.putString(":settings:fragment_args_key", "prefs_key_home_other_shortcut_background_blur");
            mRecommend.addRecommendView(getString(R.string.home_other_shortcut_background_blur),
                null,
                HomeOtherSettings.class,
                args1,
                R.string.home_other
            );
        }

    }

    /**
     * HyperOS 4 起图标大小由「布局」页的 {@code home_layout_icon_scale} 接管。
     *
     * <p>旧的 {@code home_title_icon_size} 只在 os2/os3 的 hook 里被消费，OS4 上这个入口
     * 已经不起作用。选择置灰而不是隐藏 —— 用户记得它原本在这里，直接标出去处比让它凭空消失
     * 更好找。依赖它的滑块由 {@link Preference#setEnabled(boolean)} 连带禁用。
     */
    private void moveIconSizeToLayout() {
        Preference enable = findPreference("prefs_key_home_title_icon_size_enable");
        Preference size = findPreference("prefs_key_home_title_icon_size");

        // 先落成关闭：置灰而不改值的话，UI 仍显示为「开」，旧值也还会被 hook 读到。
        if (enable instanceof SwitchPreference switchPreference && switchPreference.isChecked()) {
            switchPreference.setChecked(false);
        }

        for (Preference preference : new Preference[]{enable, size}) {
            if (preference == null) continue;
            preference.setEnabled(false);
            preference.setSummary(R.string.home_title_icon_size_moved);
        }
    }
}
