package com.sevtinge.hyperceiler.ui.fragment.settings.adapter;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

public class ProxyHeaderViewAdapter extends RecyclerView.Adapter {

    final RecyclerView.Adapter mBaseAdapter;
    private HashMap<Integer, View> mHeaderViews = new HashMap<>();
    private boolean isRemovableViewExist = false;

    public ProxyHeaderViewAdapter(RecyclerView.Adapter adapter) {
        mBaseAdapter = adapter;
        setHasStableIds(adapter.hasStableIds());
    }

    public Map<Integer, View> getHeaderViews() {
        return mHeaderViews;
    }

    public HeaderAdapter getBaseAdapter() {
        return (HeaderAdapter) mBaseAdapter;
    }

    public void resume() {
        ((HeaderAdapter) mBaseAdapter).resume();
        notifyDataSetChanged();
    }

    public void updateItem(int position) {
        notifyItemChanged(position + mHeaderViews.size());
    }

    public void updateHeaderViewInfo() {
        ((HeaderAdapter) mBaseAdapter).updateHeaderViewInfo();
    }

    public void pause() {
        ((HeaderAdapter) mBaseAdapter).pause();
    }

    public void stop() {
        ((HeaderAdapter) mBaseAdapter).stop();
    }

    public void start() {
        ((HeaderAdapter) mBaseAdapter).start();
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
        if (holder instanceof FixedViewHolder) {
            ((FixedViewHolder) holder).onBind();
        } else {
            mBaseAdapter.onBindViewHolder(holder, position - this.mHeaderViews.size());
        }
    }

    @Override
    public int getItemCount() {
        return mHeaderViews.size() + mBaseAdapter.getItemCount();
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
        int size = position - mHeaderViews.size();
        if (size < 0 || size >= mBaseAdapter.getItemCount()) {
            return -1L;
        }
        return mBaseAdapter.getItemId(size);
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
        if (!mHeaderViews.containsValue(view)) {
            mHeaderViews.put(Integer.valueOf(i2), view);
            notifyDataSetChanged();
        }
    }

    public View getRemoveHintView() {
        return mHeaderViews.get(256);
    }

    public void removeHeaderView(int i, int i2, View view) {
        if (mHeaderViews.containsValue(view)) {
            mHeaderViews.remove(Integer.valueOf(i2));
            notifyDataSetChanged();
        }
    }

    public class FixedViewHolder extends RecyclerView.ViewHolder {

        public FixedViewHolder(@NonNull View itemView) {
            super(itemView);
            setIsRecyclable(false);
        }

        public void onBind() {}
    }
}
