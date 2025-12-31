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
package com.sevtinge.hyperceiler.main.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;

import java.lang.reflect.Field;

import fan.preference.PreferenceFragment;
import fan.springback.view.SpringBackLayout;

public abstract class PagePreferenceFragment extends SettingsPreferenceFragment {

    private static final String TAG = "PagePreferenceFragment";

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View onCreateView = super.onCreateView(inflater, container, savedInstanceState);
        setOverlayMode();
        return onCreateView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView listView = getListView();
        View parent = (View) listView.getParent();
        if (parent instanceof SpringBackLayout) {
            parent.setEnabled(false);
            listView.setPaddingRelative(listView.getPaddingStart(), 0, listView.getPaddingEnd(), 0);
        }
    }

    private void setOverlayMode() {
        try {
            Field declaredField = PreferenceFragment.class.getDeclaredField("mIsOverlayMode");
            declaredField.setAccessible(true);
            declaredField.set(this, false);
        } catch (Exception e) {
            AndroidLogUtils.logE(TAG, "setOverlayMode error", e);
        }
    }
}
