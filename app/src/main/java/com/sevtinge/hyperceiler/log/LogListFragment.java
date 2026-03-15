package com.sevtinge.hyperceiler.log;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.log.db.LogEntry;
import com.sevtinge.hyperceiler.log.db.LogRepository;
import com.sevtinge.hyperceiler.widget.PullToRefreshListener;
import com.sevtinge.hyperceiler.widget.PullViewHelper;

import java.util.List;

import fan.recyclerview.card.CardItemDecoration;
import fan.springback.view.SpringBackLayout;

public class LogListFragment extends Fragment {

    // 状态常量
    private static final int STATE_LOADING = 0;
    private static final int STATE_CONTENT = 1;
    private static final int STATE_EMPTY = 2;

    private int mModule;
    private LogAdapter mAdapter;
    private boolean isLoaded = false;

    private View mLoadingLayout, mEmptyLayout;
    private SpringBackLayout mSpringBackLayout;
    private RecyclerView mRecyclerView;
    private TextView mEmptyText;
    private ImageView mEmptyIcon;

    private String mCurrentKeyword = "";
    private String mCurrentLevel = "ALL";
    private String mCurrentTag = "全部标签";

    private PullViewHelper mPullViewHelper;

    public static LogListFragment newInstance(int module) {
        LogListFragment fragment = new LogListFragment();
        Bundle args = new Bundle();
        args.putInt("module", module);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_log_list, container, false);
        mModule = getArguments().getInt("module", 0);

        mSpringBackLayout = v.findViewById(R.id.spring_back);

        mRecyclerView = v.findViewById(R.id.recyclerView);
        mLoadingLayout = v.findViewById(R.id.loading_layout);
        mEmptyLayout = v.findViewById(R.id.empty_layout);
        mEmptyText = v.findViewById(R.id.empty_text);
        mEmptyIcon = v.findViewById(R.id.empty_icon);

        mAdapter = new LogAdapter(getContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.addItemDecoration(new CardItemDecoration(requireContext()));
        mRecyclerView.setAdapter(mAdapter);


        mPullViewHelper = new PullViewHelper(requireContext(), new PullToRefreshListener() {
            @Override
            public void onRefresh() {
                onlyRefreshData();
                mPullViewHelper.onPullRefreshComplete();
            }

            @Override
            public void onLoadMore() {

            }

            @Override
            public void onEnterPrivate() {

            }
        });
        mPullViewHelper.attachSpringBackLayout(mSpringBackLayout);
        mPullViewHelper.setEnablePullRefresh(true);

        return v;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        // 懒加载逻辑：可见且未加载过时触发
        if (isVisibleToUser && getView() != null && !isLoaded) {
            refreshData();
            isLoaded = true;
        }
    }

    /**
     * Activity 调用的即时响应接口
     * @param keyword 搜索词
     * @param level 日志等级 (D, I, W, E, ALL)
     * @param tag 标签名
     */
    public void applyFilter(String keyword, String level, String tag) {
        if (mCurrentKeyword.equals(keyword) && mCurrentLevel.equals(level) && mCurrentTag.equals(tag)) return;
        mCurrentKeyword = keyword;
        mCurrentLevel = level;
        mCurrentTag = tag;
        if (getView() != null) {
            refreshData();
        }
    }

    private void refreshData() {
        showState(STATE_LOADING);
        new Thread(() -> {
            // 从 Room 获取过滤后的数据
            List<LogEntry> logs = LogRepository.getInstance().getDao()
                .queryLogs(mModule == 0 ? "App" : "Xposed", mCurrentLevel, mCurrentTag, mCurrentKeyword);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    mAdapter.updateData(logs, mCurrentKeyword);
                    showState(logs.isEmpty() ? 2 : 1); // 无数据展示空态，有数据展示列表
                });
            }
        }).start();
    }

    private void onlyRefreshData() {
        new Thread(() -> {
            // 从 Room 获取过滤后的数据
            List<LogEntry> logs = LogRepository.getInstance().getDao()
                .queryLogs(mModule == 0 ? "App" : "Xposed", mCurrentLevel, mCurrentTag, mCurrentKeyword);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    mAdapter.updateData(logs, mCurrentKeyword);
                });
            }
        }).start();
    }

    /**
     * 三态切换控制
     * @param state 0:Loading, 1:Content, 2:Empty
     */
    private void showState(int state) {
        mLoadingLayout.setVisibility(state == STATE_LOADING ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(state == STATE_CONTENT ? View.VISIBLE : View.GONE);
        mEmptyLayout.setVisibility(state == STATE_EMPTY ? View.VISIBLE : View.GONE);

        if (state == STATE_EMPTY) {
            // 判断是搜不到结果还是没日志
            boolean isFiltering = !mCurrentKeyword.isEmpty() || !mCurrentLevel.equals("ALL") ||
                !mCurrentTag.equals("全部标签");

            if (isFiltering) {
                mEmptyText.setText("未找到匹配的日志");
                mEmptyIcon.setImageResource(R.drawable.search_list_empty); // 搜索无结果图标
            } else {
                mEmptyText.setText("暂无日志");
                mEmptyIcon.setImageResource(R.drawable.ic_empty);    // 纯空数据图标
            }
        }
    }
}
