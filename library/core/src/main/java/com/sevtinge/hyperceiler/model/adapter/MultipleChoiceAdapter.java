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
package com.sevtinge.hyperceiler.model.adapter;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.core.R;

import java.util.List;

import fan.androidbase.widget.CheckedTextView;

public class MultipleChoiceAdapter extends RecyclerView.Adapter<MultipleChoiceAdapter.ViewHolder> {

    private final List<String> mItems;
    private final SparseBooleanArray mCheckedItems = new SparseBooleanArray();

    private OnSelectionChangedListener mOnSelectionChangedListener;

    public MultipleChoiceAdapter(@NonNull List<String> items) {
        mItems = items;
        for (int i = 0; i < items.size(); i++) {
            mCheckedItems.put(i, false);
        }
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        mOnSelectionChangedListener = listener;
    }

    public SparseBooleanArray getCheckedArray() {
        return mCheckedItems;
    }

    public void setChecked(int position, boolean checked) {
        if (position < 0 || position >= mItems.size()) {
            return;
        }
        mCheckedItems.put(position, checked);
    }

    public void setAllChecked(boolean checked) {
        for (int i = 0; i < mItems.size(); i++) {
            mCheckedItems.put(i, checked);
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public void invertSelection() {
        for (int i = 0; i < mItems.size(); i++) {
            mCheckedItems.put(i, !mCheckedItems.get(i));
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public boolean shouldSelectAll() {
        for (int i = 0; i < mItems.size(); i++) {
            if (mCheckedItems.get(i)) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_mutiplechoice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.mTitle.setText(mItems.get(position));
        holder.mTitle.setChecked(mCheckedItems.get(position));
        holder.itemView.setOnClickListener(view -> toggleSelection(holder));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    private void toggleSelection(@NonNull ViewHolder holder) {
        int position = holder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        boolean checked = !mCheckedItems.get(position);
        mCheckedItems.put(position, checked);
        holder.mTitle.setChecked(checked);
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (mOnSelectionChangedListener != null) {
            mOnSelectionChangedListener.onSelectionChanged(shouldSelectAll());
        }
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(boolean shouldSelectAll);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final CheckedTextView mTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(android.R.id.text1);
        }
    }
}
