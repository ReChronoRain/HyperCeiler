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
package com.sevtinge.hyperceiler.common.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

import fan.preference.TextPreference;

public class XmlPreference extends TextPreference {

    private final int mInflatedXml;

    public XmlPreference(@NonNull Context context) {
        this(context, null);
    }

    public XmlPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XmlPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Preference, defStyleAttr, 0);
        mInflatedXml = a.getResourceId(R.styleable.Preference_inflatedXml, 0);
        a.recycle();
        if (getFragment() == null) {
            setFragment(DashboardFragment.class.getName());
        }
    }

    public int getInflatedXml() {
        return mInflatedXml;
    }
}
