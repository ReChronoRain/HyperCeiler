package com.sevtinge.cemiuiler.data.adapter;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.sevtinge.cemiuiler.R;

import java.util.List;

import moralnorm.appcompat.widget.CheckedTextView;

public class MutipleChoiceAdapter extends BaseAdapter {

    // 填充数据的list
    private List<String> mList;
    // 用来控制CheckBox的选中状况
    private SparseBooleanArray mIsChecked;
    // 用来导入布局
    private LayoutInflater mInflater;

    public MutipleChoiceAdapter(Context context, List<String> list) {
        mList = list;
        mInflater = LayoutInflater.from(context);
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

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            // 获得ViewHolder对象
            holder = new ViewHolder();
            // 导入布局并赋值给convertview
            convertView = mInflater.inflate(R.layout.item_custom_mutiplechoice, null);
            holder.mCheckBoxTitle = (CheckedTextView) convertView.findViewById(android.R.id.text1);
            // 为view设置标签
            convertView.setTag(holder);
        } else {
            // 取出holder
            holder = (ViewHolder) convertView.getTag();
        }
        // 设置list中TextView的显示
        holder.mCheckBoxTitle.setText(mList.get(position));
        // 根据isSelected来设置checkbox的选中状况
        holder.mCheckBoxTitle.setChecked(getCheckedArray().get(position));
        return convertView;
    }

    public SparseBooleanArray getCheckedArray() {
        return mIsChecked;
    }

    public void setCheckedArray(SparseBooleanArray isChecked) {
        mIsChecked = isChecked;
    }

    public static class ViewHolder {

        public CheckedTextView mCheckBoxTitle;
    }
}
