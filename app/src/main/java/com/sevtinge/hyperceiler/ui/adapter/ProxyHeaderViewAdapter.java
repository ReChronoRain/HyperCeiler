package com.sevtinge.hyperceiler.ui.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

import fan.recyclerview.card.CardGroupAdapter;

public class ProxyHeaderViewAdapter extends CardGroupAdapter {

    // 头部视图的容器
    private LinearLayout mHeaderContainer;
    final RecyclerView.Adapter<?> mBaseAdapter;
    private HashMap<Integer, View> mHeaderViews = new HashMap();
    private boolean isRemovableViewExist = false;


    public ProxyHeaderViewAdapter(RecyclerView.Adapter<?> adapter) {
        mBaseAdapter = adapter;
        setHasStableIds(adapter.hasStableIds());
        setNeedItemPressEffect(false);
    }

    public Map getHeaderViews() {
        return mHeaderViews;
    }

    public HeaderAdapter getBaseAdapter() {
        return ((HeaderAdapter) mBaseAdapter);
    }

    public void start() {
        ((HeaderAdapter)mBaseAdapter).start();
    }

    public void resume() {
        ((HeaderAdapter)mBaseAdapter).resume();
        notifyDataSetChanged();
    }

    public void pause() {
        ((HeaderAdapter)mBaseAdapter).pause();
    }

    public void stop() {
        ((HeaderAdapter)mBaseAdapter).stop();
    }

    public void updateItem(int i) {
        notifyItemChanged(i + mHeaderViews.size());
    }

    public void updateHeaderViewInfo() {
        ((HeaderAdapter) mBaseAdapter).updateHeaderViewInfo();
    }

    @Override
    public void setHasStableIds() {
        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        return mHeaderViews.size() + mBaseAdapter.getItemCount();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 256 && mHeaderViews.get(256) != null) {
            return new FixedViewHolder(mHeaderViews.get(256));
        }
        if (viewType == 512 && mHeaderViews.get(512) != null) {
            return new FixedViewHolder(mHeaderViews.get(512));
        }
        return mBaseAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (holder instanceof FixedViewHolder) {
            ((FixedViewHolder) holder).onBind();
        } else {
            ((HeaderAdapter) mBaseAdapter).onBindViewHolder((HeaderAdapter.HeaderViewHolder) holder, position - mHeaderViews.size());
        }
        if (position == 0) {
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            if (params != null && params instanceof ViewGroup.MarginLayoutParams marginLayoutParams) {
                marginLayoutParams.topMargin = 0;
                marginLayoutParams.bottomMargin = 0;
                holder.itemView.setLayoutParams(marginLayoutParams);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        int size = mHeaderViews.size();
        if (position < size) {
            return (position == 0 && isRemovableViewExist) ? 256 : 512;
        }
        return this.mBaseAdapter.getItemViewType(position - size);
    }

    @Override
    public long getItemId(int position) {
        int size = position - this.mHeaderViews.size();
        if (size < 0 || size >= this.mBaseAdapter.getItemCount()) {
            return -1L;
        }
        return this.mBaseAdapter.getItemId(size);
    }

    public void addRemovableHintView(View view) {
        isRemovableViewExist = true;
        addHeaderView(0, 256, view);
    }

    public void removeRemovableHintView(View view) {
        isRemovableViewExist = false;
        removeHeaderView(0, 256, view);
    }

    public void addDeferedSetupView(View view) {
        addHeaderView(1, 512, view);
    }

    public void removeDeferedSetupView(View view) {
        removeHeaderView(1, 512, view);
    }

    public void addHeaderView(int i, int i2, View view) {
        if (mHeaderViews.containsKey(Integer.valueOf(i2))) return;
        mHeaderViews.put(Integer.valueOf(i2), view);
        notifyDataSetChanged();
    }

    public View getRemoveHintView() {
        return mHeaderViews.get(256);
    }

    public void removeHeaderView(int i, int i2, View view) {
        if (this.mHeaderViews.containsKey(Integer.valueOf(i2))) {
            this.mHeaderViews.remove(Integer.valueOf(i2));
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewGroup(int position) {
        int size = position - mHeaderViews.size();
        if (size >= 0 && size < mBaseAdapter.getItemCount()) {
            return ((HeaderAdapter) mBaseAdapter).getHeaders().get(size).groupId;
        }
        return Integer.MIN_VALUE;
    }

    public class FixedViewHolder extends RecyclerView.ViewHolder {

        public FixedViewHolder(@NonNull View itemView) {
            super(itemView);
            setIsRecyclable(false);
        }

        public void onBind() {}
    }

}
