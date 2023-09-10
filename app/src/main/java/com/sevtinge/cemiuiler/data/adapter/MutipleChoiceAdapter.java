package com.sevtinge.cemiuiler.data.adapter;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.cemiuiler.R;

import java.util.List;

import moralnorm.appcompat.widget.CheckedTextView;

public class MutipleChoiceAdapter extends RecyclerView.Adapter<MutipleChoiceAdapter.ViewHolder> {

    // 填充数据的list
    private List<String> mList;
    // 用来控制CheckBox的选中状况
    private SparseBooleanArray mIsChecked;

    private OnCurWillCheckAllChangedListener mListener;
    private boolean curWillCheckAll = true;

    public MutipleChoiceAdapter(List<String> list) {
        mList = list;
        mIsChecked = new SparseBooleanArray();
        // 初始化数据
        initData();
    }

    // 初始化isSelected的数据
    private void initData() {
        for (int i = 0; i < mList.size(); i++) {
            getCheckedArray().put(i, false);
        }
    }

    public interface OnCurWillCheckAllChangedListener {
        void onCurWillCheckAllChanged(boolean curWillCheckAll);
    }

    public void setOnCurWillCheckAllChangedListener(OnCurWillCheckAllChangedListener listener) {
        this.mListener = listener;
    }

    public SparseBooleanArray getCheckedArray() {
        return mIsChecked;
    }

    public void setCheckedArray(SparseBooleanArray isChecked) {
        mIsChecked = isChecked;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mutiplechoice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CheckedTextView mCheckBoxTitle = holder.mCheckBoxTitle;
        // 设置list中TextView的显示
        mCheckBoxTitle.setText(mList.get(position));
        // 根据isSelected来设置checkbox的选中状况
        mCheckBoxTitle.setChecked(getCheckedArray().get(position));
        holder.itemView.setOnClickListener(v -> {
            // 改变CheckBox的状态
            mCheckBoxTitle.toggle();
            // 将CheckBox的选中状况记录下来
            getCheckedArray().put(position, mCheckBoxTitle.isChecked());
            for (int i = 0; i < mIsChecked.size(); i++) {
                if (mIsChecked.valueAt(i)) {
                    curWillCheckAll = false;
                    break;
                } else {
                    curWillCheckAll = true;
                }
            }
            if (mListener != null) {
                mListener.onCurWillCheckAllChanged(curWillCheckAll);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public CheckedTextView mCheckBoxTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            mCheckBoxTitle = itemView.findViewById(android.R.id.text1);
        }
    }
}
