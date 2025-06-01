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
package com.sevtinge.hyperceiler.provision.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.ui.R;

public class PermissionItemView extends FrameLayout {

    boolean mIsSelected = false;

    TextView mItemTitle;
    ImageView mItemIcon;

    OnSelectedListener mOnSelectedListener;

    public PermissionItemView(@NonNull Context context) {
        this(context, null);
    }

    public PermissionItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.provision_permission_list_item_view, this, true);
        mItemTitle = findViewById(R.id.item_title);
        mItemIcon = findViewById(R.id.item_icon);

        mItemTitle.setTextColor(getResources().getColor(R.color.provision_list_item_text_unselected));
        mItemIcon.setVisibility(mIsSelected ? VISIBLE : INVISIBLE);
        setClickable(true);
    }

    @Override
    public boolean performClick() {
        setItemSelected(!mIsSelected);
        return super.performClick();
    }

    public void setItemTitle(int resId) {
        setItemTitle(getContext().getString(resId));
    }

    public void setItemTitle(String title) {
        mItemTitle.setText(title);
    }

    public void setItemSelected(boolean selected) {
        if (mIsSelected != selected) {
            mIsSelected = selected;
            if (mOnSelectedListener != null) {
                mOnSelectedListener.onSelected(mIsSelected);
            }
            updateItemState(mIsSelected);
        }
    }

    public boolean isItemSelected() {
        return mIsSelected;
    }

    public void setOnSelectedListener(OnSelectedListener l) {
        mOnSelectedListener = l;
    }

    public void updateItemState(boolean selected) {
        mItemIcon.setImageDrawable(getResources().getDrawable(R.drawable.provision_picker_btn_radio));
        mItemIcon.setVisibility(selected ? VISIBLE : INVISIBLE);
    }

    public interface OnSelectedListener {
        /**
         * Called when a view has been selected.
         *
         * @param selected The view that was selected.
         */
        void onSelected(boolean selected);
    }
}
