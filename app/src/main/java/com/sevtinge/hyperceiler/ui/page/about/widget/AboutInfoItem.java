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
package com.sevtinge.hyperceiler.ui.page.about.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.sevtinge.hyperceiler.ui.R;

public class AboutInfoItem extends FrameLayout {

    private String mTitle;
    private String mValue;

    private TextView mTitleView;
    private TextView mValueView;
    private ImageView mArrowRightView;

    public AboutInfoItem(Context context) {
        this(context, null);
    }

    public AboutInfoItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AboutInfoItem, 0, 0);
        mTitle = a.getString(R.styleable.AboutInfoItem_android_title);
        mValue = a.getString(R.styleable.AboutInfoItem_android_value);
        a.recycle();
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.item_about_info, this, true);
        mTitleView = findViewById(R.id.title);
        mValueView = findViewById(R.id.summary);
        mArrowRightView = findViewById(R.id.arrow_right);
        setTitle(mTitle);
        setValue(mValue);
        setArrowRightVisibility(false);
        refreshInfo();
    }

    public void refreshInfo() {}

    public String getTitle() {
        return mTitle;
    }

    public String getValue() {
        return mValue;
    }

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

    public void setArrowRightVisibility(boolean visible) {
        if (mArrowRightView != null) mArrowRightView.setVisibility(visible ? VISIBLE : GONE);
    }

    public String getString(@StringRes int resId) {
        return getContext().getString(resId);
    }
}
