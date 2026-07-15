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
package com.sevtinge.hyperceiler.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.model.adapter.MultipleChoiceAdapter;

import java.util.List;

public class MultipleChoiceView extends LinearLayout implements MultipleChoiceAdapter.OnSelectionChangedListener {

    private MultipleChoiceAdapter mAdapter;
    private RecyclerView mListView;
    private Button mSelectionButton;
    private Button mConfirmButton;
    private OnCheckedListener mOnCheckedListener;
    private boolean mShouldSelectAll = true;

    public MultipleChoiceView(Context context) {
        super(context);
        initView(context);
    }

    public MultipleChoiceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.view_mutiplechoice, this);
        mListView = view.findViewById(android.R.id.list);
        mListView.setLayoutManager(new LinearLayoutManager(context));
        mListView.setHasFixedSize(true);

        mSelectionButton = view.findViewById(android.R.id.button2);
        mConfirmButton = view.findViewById(android.R.id.button1);
        mSelectionButton.setOnClickListener(ignored -> updateAllSelections());
        mConfirmButton.setOnClickListener(ignored -> notifyChecked());
        updateSelectionButton();
    }

    public void setData(@NonNull List<String> data, @Nullable boolean[] selectedItems) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (selectedItems != null && selectedItems.length != data.size()) {
            throw new IllegalArgumentException("data and selectedItems must have the same length");
        }

        mAdapter = new MultipleChoiceAdapter(data);
        mAdapter.setOnSelectionChangedListener(this);
        if (selectedItems != null) {
            for (int i = 0; i < selectedItems.length; i++) {
                mAdapter.setChecked(i, selectedItems[i]);
            }
        }

        mListView.setAdapter(mAdapter);
        onSelectionChanged(mAdapter.shouldSelectAll());
    }

    public void setOnCheckedListener(OnCheckedListener listener) {
        mOnCheckedListener = listener;
    }

    @Override
    public void onSelectionChanged(boolean shouldSelectAll) {
        mShouldSelectAll = shouldSelectAll;
        updateSelectionButton();
    }

    public void selectAll() {
        if (mAdapter != null) {
            mAdapter.setAllChecked(true);
        }
    }

    public void deselectAll() {
        if (mAdapter != null) {
            mAdapter.setAllChecked(false);
        }
    }

    public void reverseSelect() {
        if (mAdapter != null) {
            mAdapter.invertSelection();
        }
    }

    private void updateAllSelections() {
        if (mShouldSelectAll) {
            selectAll();
        } else {
            reverseSelect();
        }
    }

    private void updateSelectionButton() {
        mSelectionButton.setText(mShouldSelectAll
            ? R.string.miuix_appcompat_action_mode_select_all
            : R.string.miuix_appcompat_action_mode_inverse);
    }

    private void notifyChecked() {
        if (mOnCheckedListener != null && mAdapter != null) {
            mOnCheckedListener.onChecked(mAdapter.getCheckedArray());
        }
    }

    public interface OnCheckedListener {
        void onChecked(@NonNull SparseBooleanArray checkedItems);
    }
}
