package com.sevtinge.hyperceiler.home.adapter;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

import fan.recyclerview.card.CardGroupAdapter;

public class ProxyHeaderViewAdapter extends CardGroupAdapter {

    private static final int VIEW_TYPE_REMOVABLE_HEADER = 256;
    private static final int VIEW_TYPE_DEFERRED_HEADER = 512;
    private static final int VIEW_TYPE_FOOTER_HINT = 768;

    final RecyclerView.Adapter<?> mBaseAdapter;
    private final HashMap<Integer, View> mHeaderViews = new HashMap<>();
    private View mFooterHintView;
    private boolean isRemovableViewExist = false;


    public ProxyHeaderViewAdapter(RecyclerView.Adapter<?> adapter) {
        mBaseAdapter = adapter;
        setHasStableIds(adapter.hasStableIds());
        setNeedItemPressEffect(false);
    }

    public Map<Integer, View> getHeaderViews() {
        return mHeaderViews;
    }

    public HeaderAdapter getBaseAdapter() {
        return ((HeaderAdapter) mBaseAdapter);
    }

    public void start() {
        ((HeaderAdapter)mBaseAdapter).start();
    }

    public void resume() {
        ((HeaderAdapter) mBaseAdapter).resume();
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
        return mHeaderViews.size() + mBaseAdapter.getItemCount() + (mFooterHintView != null ? 1 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_REMOVABLE_HEADER && mHeaderViews.get(VIEW_TYPE_REMOVABLE_HEADER) != null) {
            return new FixedViewHolder(mHeaderViews.get(VIEW_TYPE_REMOVABLE_HEADER));
        }
        if (viewType == VIEW_TYPE_DEFERRED_HEADER && mHeaderViews.get(VIEW_TYPE_DEFERRED_HEADER) != null) {
            return new FixedViewHolder(mHeaderViews.get(VIEW_TYPE_DEFERRED_HEADER));
        }
        if (viewType == VIEW_TYPE_FOOTER_HINT && mFooterHintView != null) {
            return new FixedViewHolder(mFooterHintView);
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
            return (position == 0 && isRemovableViewExist) ? VIEW_TYPE_REMOVABLE_HEADER : VIEW_TYPE_DEFERRED_HEADER;
        }
        int footerPosition = size + mBaseAdapter.getItemCount();
        if (mFooterHintView != null && position == footerPosition) {
            return VIEW_TYPE_FOOTER_HINT;
        }
        return mBaseAdapter.getItemViewType(position - size);
    }

    @Override
    public long getItemId(int position) {
        int basePosition = position - mHeaderViews.size();
        if (basePosition < 0 || basePosition >= mBaseAdapter.getItemCount()) {
            return -1L;
        }
        return mBaseAdapter.getItemId(basePosition);
    }

    public void addRemovableHintView(View view) {
        isRemovableViewExist = true;
        addHeaderView(0, VIEW_TYPE_REMOVABLE_HEADER, view);
    }

    public void removeRemovableHintView(View view) {
        isRemovableViewExist = false;
        removeHeaderView(0, VIEW_TYPE_REMOVABLE_HEADER, view);
    }

    public void addDeferedSetupView(View view) {
        addHeaderView(1, VIEW_TYPE_DEFERRED_HEADER, view);
    }

    public void removeDeferedSetupView(View view) {
        removeHeaderView(1, VIEW_TYPE_DEFERRED_HEADER, view);
    }

    public void addFooterHintView(View view) {
        if (mFooterHintView == view) {
            return;
        }
        mFooterHintView = view;
        notifyDataSetChanged();
    }

    public void removeFooterHintView(View view) {
        if (mFooterHintView == view) {
            mFooterHintView = null;
            notifyDataSetChanged();
        }
    }

    public View getFooterHintView() {
        return mFooterHintView;
    }

    public void addHeaderView(int i, int i2, View view) {
        Integer key = Integer.valueOf(i2);
        View oldView = mHeaderViews.get(key);
        if (oldView == view) {
            return;
        }
        mHeaderViews.put(key, view);
        notifyDataSetChanged();
    }

    public View getRemoveHintView() {
        return mHeaderViews.get(VIEW_TYPE_REMOVABLE_HEADER);
    }

    public void removeHeaderView(int i, int i2, View view) {
        if (this.mHeaderViews.containsKey(Integer.valueOf(i2))) {
            this.mHeaderViews.remove(Integer.valueOf(i2));
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewGroup(int position) {
        int basePosition = position - mHeaderViews.size();
        if (basePosition >= 0 && basePosition < mBaseAdapter.getItemCount()) {
            return ((HeaderAdapter) mBaseAdapter).getHeaders().get(basePosition).groupId;
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
