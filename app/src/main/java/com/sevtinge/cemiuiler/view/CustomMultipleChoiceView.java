package com.sevtinge.cemiuiler.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.Nullable;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.data.adapter.MutipleChoiceAdapter;

import java.util.List;

public class CustomMultipleChoiceView extends LinearLayout {

    private MutipleChoiceAdapter mAdapter;
    private List<String> mData;
    private ListView mListView;
    private onCheckedListener mOnCheckedListener;//确定选择监听器
    private boolean curWillCheckAll = true;//当前点击按钮时是否将全选

    public CustomMultipleChoiceView(Context context) {
        super(context);
        initView();
    }

    public CustomMultipleChoiceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    @SuppressLint("InflateParams")
    private void initView(){
        /* 实例化各个控件 */
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.view_custom_mutiplechoice, null);
        mListView = view.findViewById(android.R.id.list);
        Button bt_SelectAll = view.findViewById(android.R.id.button2);
        Button bt_ok = view.findViewById(android.R.id.button1);
        bt_SelectAll.setText(curWillCheckAll ? "全选" : "反选");
        OnCustomMultipleChoiceCheckedListener onCheckedListener = new OnCustomMultipleChoiceCheckedListener();

        // 全选按钮的回调接口
        bt_SelectAll.setOnClickListener(onCheckedListener);
        bt_ok.setOnClickListener(onCheckedListener);

        // 绑定listView的监听器
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                // 取得ViewHolder对象，这样就省去了通过层层的findViewById去实例化我们需要的cb实例的步骤
                MutipleChoiceAdapter.ViewHolder holder = (MutipleChoiceAdapter.ViewHolder) arg1.getTag();
                // 改变CheckBox的状态
                holder.mCheckBoxTitle.toggle();
                // 将CheckBox的选中状况记录下来
                mAdapter.getCheckedArray().put(position, holder.mCheckBoxTitle.isChecked());
            }
        });
//        positiveBtn.setOnClickListener(l);
        addView(view);
    }

    public void setData(List<String> data, boolean[] isSelected) {
        if (data != null) {
            mData = data;
            mAdapter = new MutipleChoiceAdapter(getContext(), data);
            if (isSelected != null){
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

    public void setOnCheckedListener(onCheckedListener listener){
        mOnCheckedListener = listener;
    }

    public interface onCheckedListener{
        void onChecked(SparseBooleanArray sparseBooleanArray);
    }

    /**
     * 全选
     */
    public void selectAll(){
        if(mData != null){
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
    public void deselectAll(){
        if(mData != null){
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
    public void reverseSelect(){
        if (mData != null) {
            for (int i = 0; i < mData.size(); i++) {
                mAdapter.getCheckedArray().put(i, !mAdapter.getCheckedArray().get(i));
            }
            // 刷新listview和TextView的显示
            mAdapter.notifyDataSetChanged();
        }
    }

    private class OnCustomMultipleChoiceCheckedListener implements OnClickListener{

        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                case android.R.id.button1:
                    //确定选择的按钮
                    if(mOnCheckedListener != null && mAdapter != null){
                        mOnCheckedListener.onChecked(mAdapter.getCheckedArray());
                    }
                    break;

                case android.R.id.button2:
                    //全选/反选按钮
                    if(mData != null){
                        if(curWillCheckAll) {
                            selectAll();
                        } else {
                            deselectAll();
                        }
                        ((Button)v).setText(curWillCheckAll ? "反选" : "全选");
                        curWillCheckAll = !curWillCheckAll;
                    }
                    break;
                default:
                    break;
            }

        }

    }
}
