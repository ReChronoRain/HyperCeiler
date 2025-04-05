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
package com.sevtinge.hyperceiler.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.data.adapter.MutipleChoiceAdapter;

import java.util.List;

public class MultipleChoiceView extends LinearLayout implements MutipleChoiceAdapter.OnCurWillCheckAllChangedListener {

    private MutipleChoiceAdapter mAdapter;
    private List<String> mData;
    private RecyclerView mListView;
    private Button mAllSelectBtn;
    private Button mOkBtn;
    // 确定选择监听器
    private onCheckedListener mOnCheckedListener;
    // 当前点击按钮时是否将全选
    private boolean curWillCheckAll = true;

    public MultipleChoiceView(Context context) {
        super(context);
        initView(context);
    }

    public MultipleChoiceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }


    /* 实例化各个控件 */
    private void initView(Context context) {
        View view = inflate(context, R.layout.view_mutiplechoice, this);
        mListView = view.findViewById(android.R.id.list);
        mListView.setLayoutManager(new LinearLayoutManager(context));
        mListView.setHasFixedSize(true);
        mAllSelectBtn = view.findViewById(android.R.id.button2);
        mOkBtn = view.findViewById(android.R.id.button1);
        mAllSelectBtn.setText(curWillCheckAll ? getResources().getString(R.string.miuix_appcompat_action_mode_select_all) : getResources().getString(R.string.miuix_appcompat_action_mode_inverse));
        OnCustomMultipleChoiceCheckedListener onCheckedListener = new OnCustomMultipleChoiceCheckedListener();

        // 全选按钮的回调接口
        mAllSelectBtn.setOnClickListener(onCheckedListener);
        mOkBtn.setOnClickListener(onCheckedListener);
    }

    public void setData(List<String> data, boolean[] isSelected) {
        if (data != null) {
            mData = data;
            mAdapter = new MutipleChoiceAdapter(data);
            mAdapter.setOnCurWillCheckAllChangedListener(this);
            if (isSelected != null) {
                if (isSelected.length != data.size()) {
                    throw new IllegalArgumentException("data's length not equal the isSelected's length");
                } else {
                    for (int i = 0; i < isSelected.length; i++) {
                        mAdapter.getCheckedArray().put(i, isSelected[i]);
                    }
                }

            }
            // 绑定Adapter
            mListView.setAdapter(mAdapter);
        } else {
            throw new IllegalArgumentException("data is null");
        }
    }

    public void setOnCheckedListener(onCheckedListener listener) {
        mOnCheckedListener = listener;
    }

    @Override
    public void onCurWillCheckAllChanged(boolean curWillCheckAll) {
        this.curWillCheckAll = curWillCheckAll;
        mAllSelectBtn.setText(curWillCheckAll ? getResources().getString(R.string.miuix_appcompat_action_mode_select_all) : getResources().getString(R.string.miuix_appcompat_action_mode_inverse));
    }

    public interface onCheckedListener {
        void onChecked(SparseBooleanArray sparseBooleanArray);
    }

    /**
     * 全选
     */
    public void selectAll() {
        if (mData != null) {
            for (int i = 0; i < mData.size(); i++) {
                mAdapter.getCheckedArray().put(i, true);
            }
            // 刷新listview和TextView的显示
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 全不选
     */
    public void deselectAll() {
        if (mData != null) {
            for (int i = 0; i < mData.size(); i++) {
                mAdapter.getCheckedArray().put(i, false);
            }
            // 刷新listview和TextView的显示
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 反选
     */
    public void reverseSelect() {
        if (mData != null) {
            for (int i = 0; i < mData.size(); i++) {
                mAdapter.getCheckedArray().put(i, !mAdapter.getCheckedArray().get(i));
            }
            // 刷新listview和TextView的显示
            mAdapter.notifyDataSetChanged();
        }
    }

    private class OnCustomMultipleChoiceCheckedListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                // 确定选择按钮
                case android.R.id.button1 -> {
                    if (mOnCheckedListener != null && mAdapter != null) {
                        mOnCheckedListener.onChecked(mAdapter.getCheckedArray());
                    }
                }
                // 全选/反选按钮
                case android.R.id.button2 -> {
                    if (mData != null) {
                        if (curWillCheckAll) {
                            selectAll();
                        } else {
                            reverseSelect();
                        }
                        ((Button) v).setText(curWillCheckAll ? getResources().getString(R.string.miuix_appcompat_action_mode_inverse) : getResources().getString(R.string.miuix_appcompat_action_mode_select_all));
                        curWillCheckAll = !curWillCheckAll;
                    }
                }
            }
        }
    }
}
