/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.main.page.settings.development;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DevelopmentDebugInfoFragment extends SettingsPreferenceFragment {

    private Preference mDebugInfo;
    private ExecutorService mExecutor;
    private Future<?> mFuture;

    private volatile String mCachedDebugInfo;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_development_debug_info;
    }

    @Override
    public void initPrefs() {
        mDebugInfo = findPreference("prefs_key_debug_info");
        if (mDebugInfo != null) {
            mDebugInfo.setTitle("Loading...");
            loadDebugInfoAsync();
        }
    }

    private void loadDebugInfoAsync() {
        if (mCachedDebugInfo != null) {
            if (mDebugInfo != null) {
                mDebugInfo.setTitle(mCachedDebugInfo);
            }
            return;
        }

        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "HyperCeiler-DebugInfo-Worker");
                t.setDaemon(true);
                return t;
            });
        }

        if (mFuture != null && !mFuture.isDone()) {
            mFuture.cancel(true);
        }

        mFuture = mExecutor.submit(() -> {
            try {
                final String info = com.sevtinge.hyperceiler.utils.DeviceInfoBuilder.build(requireContext());
                mCachedDebugInfo = info;
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (mDebugInfo != null) {
                            mDebugInfo.setTitle(info);
                        }
                    });
                }
            } catch (Exception e) {
                String fallback = "Failed to load debug info";
                mCachedDebugInfo = fallback;
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (mDebugInfo != null) {
                            mDebugInfo.setTitle(fallback);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mFuture != null && !mFuture.isDone()) {
                mFuture.cancel(true);
            }
            if (mExecutor != null && !mExecutor.isShutdown()) {
                mExecutor.shutdownNow();
            }
        } catch (Exception ignored) {
        }
    }
}
