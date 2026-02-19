package com.sevtinge.hyperceiler.home;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class CustomOrderDiffCallback extends DiffUtil.Callback {
    public final List<Header> newList;
    public final List<Header> oldList;

    public CustomOrderDiffCallback(List<Header> list, List<Header> list2) {
        this.oldList = list;
        this.newList = list2;
    }

    public int getOldListSize() {
        return this.oldList.size();
    }

    public int getNewListSize() {
        return this.newList.size();
    }

    public boolean areItemsTheSame(int i, int i2) {
        return this.oldList.get(i).equals(this.newList.get(i2));
    }

    public boolean areContentsTheSame(int i, int i2) {
        return this.oldList.get(i).equals(this.newList.get(i2));
    }
}
