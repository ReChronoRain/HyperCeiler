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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.safemode.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.sevtinge.hyperceiler.ui.R;

public class CrashInfoItem extends LinearLayout {

    private TextView mTitleView;
    private TextView mValueView;

    private String mTitle;
    private String mValue;

    public CrashInfoItem(Context context) {
        this(context, null);
    }

    public CrashInfoItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CrashInfoCard);
        mTitle = a.getString(R.styleable.CrashInfoCard_android_title);
        mValue = a.getString(R.styleable.CrashInfoCard_android_value);
        a.recycle();
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.item_crash_info, this, true);
        mTitleView = findViewById(R.id.title);
        mValueView = findViewById(R.id.value);

        setTitle(mTitle);
        setValue(mValue);
        refreshInfo();
    }

    public void refreshInfo() {}

    public void setTitle(String title) {
        mTitle = title;
        if (mTitleView != null) mTitleView.setText(title);
    }

    public void setTitle(@StringRes int resid) {
        mTitle = getString(resid);
        if (mTitleView != null) mTitleView.setText(resid);
    }

    public void setValue(String value) {
        mValue = value;
        if (mValueView != null) mValueView.setText(value);
    }

    public void setValue(@StringRes int resid) {
        mValue = getString(resid);
        if (mValueView != null) mValueView.setText(resid);
    }

    public String getString(@StringRes int resId) {
        return getContext().getString(resId);
    }
}
