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
package com.sevtinge.hyperceiler.hooker.systemui;

import static com.sevtinge.hyperceiler.sub.SubPickerActivity.ALL_APPS_MODE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.common.utils.PrefsDataStore;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.sub.SubPickerActivity;

import fan.appcompat.app.AlertDialog;
import fan.preference.SeekBarPreferenceCompat;

public class ControlCenterSettings extends DashboardFragment {

    /**
     * 运营商档位偏好的完整键。
     * 注意：这里必须带 prefs_key_ 前缀。SharedPreferences 变更回调给出的是 XML 里的完整键，
     * 而 PrefsBridge 读取时会自动补前缀，两者只在"读取"时等价，比较时不等价。
     */
    private static final String OPERATOR_MODE_KEY =
        "prefs_key_system_ui_control_center_hide_operator";
    private static final String CARRIER_NAME_SLOT1_KEY =
        "prefs_key_system_ui_control_center_real_network_name_slot1";
    private static final String CARRIER_NAME_SLOT2_KEY =
        "prefs_key_system_ui_control_center_real_network_name_slot2";
    /** 与 CustomCarrierText.MODE_REAL_NETWORK_NAME 对应：只有该档位才用得到自定义名称。 */
    private static final int MODE_REAL_NETWORK_NAME = 5;

    Preference mExpandNotification;
    Preference mFocusNotificationList;
    Preference mCarrierNameSlot1;
    Preference mCarrierNameSlot2;
    RecommendPreference mRecommend;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_control_center;
    }

    @Override
    public void initPrefs() {
        mExpandNotification = findPreference("prefs_key_system_ui_control_center_expand_notification");

        mCarrierNameSlot1 = findPreference(CARRIER_NAME_SLOT1_KEY);
        mCarrierNameSlot2 = findPreference(CARRIER_NAME_SLOT2_KEY);
        setupCustomCarrierName(mCarrierNameSlot1,
            R.string.system_ui_control_center_real_network_name_slot1_desc);
        setupCustomCarrierName(mCarrierNameSlot2,
            R.string.system_ui_control_center_real_network_name_slot2_desc);
        installOperatorModeWatcher();
        updateCarrierNameVisibility();

        mExpandNotification.setOnPreferenceClickListener(
                preference -> {
                    Intent intent = new Intent(getActivity(), SubPickerActivity.class);
                    intent.putExtra("mode", ALL_APPS_MODE);
                    intent.putExtra("key", preference.getKey());
                    startActivity(intent);
                    return true;
                }
        );

        mFocusNotificationList = findPreference("prefs_key_system_ui_focus_notification_list");

        mFocusNotificationList.setOnPreferenceClickListener(
            preference -> {
                Intent intent = new Intent(getActivity(), SubPickerActivity.class);
                intent.putExtra("mode", ALL_APPS_MODE);
                intent.putExtra("key", preference.getKey());
                startActivity(intent);
                return true;
            }
        );

        ((SeekBarPreferenceCompat) findPreference("prefs_key_system_control_center_old_qs_grid_columns")).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (progress < 3) progress = 5;
                try {
                    Settings.Secure.putInt(requireActivity().getContentResolver(), "sysui_qqs_count", progress);
                } catch (Throwable t) {
                    AndroidLog.w("SeekBarPreferenceCompat", "onProgressChanged -> system_control_center_old_qs_grid_columns", t);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Bundle args1 = new Bundle();
        mRecommend = new RecommendPreference(requireContext());
        getPreferenceScreen().addPreference(mRecommend);

        args1.putString(":settings:fragment_args_key", "prefs_key_new_clock_status");
        mRecommend.addRecommendView(getString(R.string.system_ui_statusbar_clock_title),
                null,
                StatusBarSettings.class,
                args1,
                R.string.system_ui_statusbar_title
        );

    }

    /**
     * 只在下拉选中「使用真实注册网络名称」时显示两个自定义名称输入框。
     *
     * 不能用 setOnPreferenceChangeListener：fan 的 DropDownPreference 在 setValue 里
     * 只做 persistString + notifyChanged，不会回调 callChangeListener。
     * 也不能用 registerOnSharedPreferenceChangeListener：本页由 PrefsConfigurator 装的是
     * PrefsDataStore，偏好读写全部转给 PrefsBridge，根本不经过 SharedPreferences，
     * 监听器永远不触发——这正是"必须退出重进才生效"的原因。
     * 所以给该偏好单独挂一个 PrefsDataStore 子类：其余读写全部沿用仓库现成实现，
     * 只在写入后补一次界面刷新。
     */
    private void installOperatorModeWatcher() {
        Preference mode = findPreference(OPERATOR_MODE_KEY);
        if (mode == null) return;
        mode.setPreferenceDataStore(new OperatorModeDataStore(this::updateCarrierNameVisibility));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCarrierNameVisibility();
    }

    private static boolean isOperatorModeKey(String key) {
        return OPERATOR_MODE_KEY.equals(key)
            || OPERATOR_MODE_KEY.equals("prefs_key_" + key);
    }

    private void updateCarrierNameVisibility() {
        boolean show;
        try {
            show = PrefsBridge.getStringAsInt(OPERATOR_MODE_KEY, 0) == MODE_REAL_NETWORK_NAME;
        } catch (Throwable t) {
            // 偏好值类型异常时不能连累设置页，按"未选中真实网络名称"处理。
            AndroidLog.w("ControlCenterSettings", "read operator mode failed", t);
            show = false;
        }
        if (mCarrierNameSlot1 != null) mCarrierNameSlot1.setVisible(show);
        if (mCarrierNameSlot2 != null) mCarrierNameSlot2.setVisible(show);
    }

    /**
     * 自定义运营商名称入口。
     *
     * 这里刻意不用 EditTextPreference：本页偏好由 fan.preference 渲染，
     * androidx 的 EditTextPreference 对话框在该主题下点开即崩，改用与仓库其它
     * 输入弹窗一致的 AlertDialog + EditText 写法。
     */
    private void setupCustomCarrierName(Preference preference, int emptyHintRes) {
        if (preference == null) return;
        updateCustomCarrierNameSummary(preference, emptyHintRes);
        preference.setOnPreferenceClickListener(p -> {
            showCustomCarrierNameDialog(p, emptyHintRes);
            return true;
        });
    }

    private void showCustomCarrierNameDialog(Preference preference, int emptyHintRes) {
        Activity activity = getActivity();
        if (activity == null) return;

        // 与仓库其它输入弹窗保持一致：直接复用 edit_dialog 布局。
        View view = LayoutInflater.from(activity).inflate(R.layout.edit_dialog, null);
        EditText input = view.findViewById(R.id.title);
        String current = PrefsBridge.getString(preference.getKey(), "");
        input.setText(current == null ? "" : current);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(activity)
            .setTitle(preference.getTitle())
            .setView(view)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                // 留空表示"不自定义"，由 hook 回退到真实注册网络名称。
                PrefsBridge.putString(preference.getKey(), input.getText().toString().trim());
                updateCustomCarrierNameSummary(preference, emptyHintRes);
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void updateCustomCarrierNameSummary(Preference preference, int emptyHintRes) {
        String value = PrefsBridge.getString(preference.getKey(), "");
        if (value == null || value.trim().isEmpty()) {
            preference.setSummary(emptyHintRes);
        } else {
            preference.setSummary(value.trim());
        }
    }

    /**
     * 仓库现成的 [PrefsDataStore] 已实现全部读写转发（最终落到 PrefsBridge），
     * 这里只补一个写入后的界面刷新，其余行为与原存储完全一致。
     */
    private static final class OperatorModeDataStore extends PrefsDataStore {

        private final Runnable mOnWrite;

        OperatorModeDataStore(Runnable onWrite) {
            mOnWrite = onWrite;
        }

        @Override
        public void putString(@NonNull String key, @Nullable String value) {
            super.putString(key, value);
            if (isOperatorModeKey(key)) mOnWrite.run();
        }
    }
}
