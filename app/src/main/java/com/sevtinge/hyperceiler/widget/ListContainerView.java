package com.sevtinge.hyperceiler.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.PullViewHelper;

import fan.nestedheader.widget.NestedHeaderLayout;
import fan.springback.view.SpringBackLayout;

public class ListContainerView extends FrameLayout {

    private ViewGroup mContentView;
    private View mHeaderView;
    private NestedHeaderLayout mNestedHeader;
    private ViewGroup mPrefsContainer;
    private NestedScrollView mNestedScrollView;
    private ViewGroup mContainerView;
    private RecyclerView mRecyclerView;
    private SpringBackLayout mSpringBackLayout;
    private PullViewHelper mViewHelper;

    public ListContainerView(@NonNull Context context) {
        this(context, null);
    }

    public ListContainerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ListContainerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initListView();
    }

    private void initListView() {
        View.inflate(getContext(), R.layout.view_list_container, this);
        mContentView = findViewById(R.id.content);
        mNestedHeader = findViewById(R.id.nested_header_layout);
        mSpringBackLayout = findViewById(R.id.scrollable_view_group);
        mNestedScrollView = findViewById(R.id.scrollview);
        mContainerView = findViewById(R.id.container);
        mPrefsContainer = findViewById(R.id.prefs_container);
        mRecyclerView = findViewById(android.R.id.list);
        mHeaderView = findViewById(R.id.search_bar);

        mViewHelper = new PullViewHelper(getContext(), null);
        mViewHelper.attachSpringBackLayout(mSpringBackLayout);
        mHeaderView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.GONE);
    }

    public ViewGroup getContentView() {
        return mContentView;
    }

    public ViewGroup getContainerView() {
        return mContainerView;
    }

    public NestedHeaderLayout getNestedHeader() {
        return mNestedHeader;
    }

    public View getHeaderView() {
        return mHeaderView;
    }

    public SpringBackLayout getSpringBackLayout() {
        return mSpringBackLayout;
    }

    public NestedScrollView getNestedScrollView() {
        return mNestedScrollView;
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public void addContentView(View view) {
        mContentView.addView(view);
    }

    public void addContainerView(View view) {
        mContainerView.addView(view);
    }

    public void addPrefsContainer(View view) {
        mPrefsContainer.addView(view);
    }

    public void setOnPullToRefreshListener(PullToRefreshListener pullToRefreshListener) {
        if (mViewHelper != null) {
            mViewHelper.setPullListener(pullToRefreshListener);
        }
    }

    public void setAdapter(RecyclerView.Adapter<?> adapter) {
        mRecyclerView.setAdapter(adapter);
    }

    public void setRefreshEnable(boolean enable) {
        mViewHelper.setEnablePullRefresh(enable);
    }

    public void setLoadEnable(boolean enable) {
        mViewHelper.setEnableLoadMore(enable);
    }

    public void setEnablePrivate(boolean enable) {
        mViewHelper.setEnablePrivate(enable);
    }

    public void onLoadMoreComplete() {
        mViewHelper.onLoadMoreComplete();
    }

    public void onRefreshComplete() {
        mViewHelper.onPullRefreshComplete();
    }

    public void showHeaderView() {
        mHeaderView.setVisibility(View.VISIBLE);
    }

    public void showLoading() {
        mNestedScrollView.setVisibility(View.VISIBLE);
        //mSpringBackLayout.setTarget(mNestedScrollView);
        mRecyclerView.setVisibility(View.GONE);
    }

    public void showPrefsContainer() {
        mNestedScrollView.setVisibility(View.VISIBLE);
        mSpringBackLayout.setTarget(mNestedScrollView);
        mRecyclerView.setVisibility(View.GONE);
    }

    public void showRecyclerView() {
        mNestedScrollView.setVisibility(View.GONE);
        mSpringBackLayout.setTarget(mRecyclerView);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    public void resetUI() {
        mRecyclerView.removeAllViews();
    }
}
