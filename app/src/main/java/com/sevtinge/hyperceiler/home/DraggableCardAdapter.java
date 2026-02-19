package com.sevtinge.hyperceiler.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;

import java.util.ArrayList;
import java.util.List;

public class DraggableCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public Context mContext;
    public List<Header> mCustomOrderItems;
    public String peopleAndPetTitle;

    public DraggableCardAdapter(Context context, List<Header> headers) {
        mContext = context;
        this.mCustomOrderItems = new ArrayList<>();
        for (Header h : headers) {
            // 必须深拷贝！否则在弹窗里勾选会直接改变首页内存里的对象
            this.mCustomOrderItems.add(h);
        }
    }

    public void setData(List<Header> headers) {
        DiffUtil.DiffResult calculateDiff = DiffUtil.calculateDiff(new CustomOrderDiffCallback(this.mCustomOrderItems, headers));
        mCustomOrderItems.clear();
        mCustomOrderItems.addAll(headers);
        calculateDiff.dispatchUpdatesTo(this);
    }

    public List<Header> getData() {
        return mCustomOrderItems;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_custom_order_restore, parent, false);
            return new RestoreButtonViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_custom_order_draggable, parent, false);
        return new CustomOrderViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof RestoreButtonViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                // 1. 将内存副本中所有 Header 的显示状态重置为 true
                for (Header header : mCustomOrderItems) {
                    header.displayStatus = true;
                }
                // 2. 刷新弹窗里的列表显示，让所有勾选框都勾上
                notifyDataSetChanged();
            });
        } else {
            Header customOrderItem = mCustomOrderItems.get(position);
            CustomOrderViewHolder viewHolder = (CustomOrderViewHolder) holder;
            viewHolder.checkBox.setOnCheckedChangeListener(null);
            viewHolder.bind(customOrderItem);
            viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                    int currentPos = holder.getBindingAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION) {
                        // 更新内存中的对象状态
                        mCustomOrderItems.get(currentPos).displayStatus = isChecked;
                    }
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        // 如果位置到了最后，就是重置按钮
        if (position == mCustomOrderItems.size()) {
            return 1;
        }
        return 0;
    }

    @Override
    public int getItemCount() {
        return mCustomOrderItems.size() + 1;
    }
}
