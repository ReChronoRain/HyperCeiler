/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.log;

import android.content.Context;
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
import com.sevtinge.hyperceiler.common.utils.ShellUtils;
import com.sevtinge.hyperceiler.log.db.LogDao;
import com.sevtinge.hyperceiler.log.db.LogEntry;
import com.sevtinge.hyperceiler.log.db.LogRepository;
import com.sevtinge.hyperceiler.widget.PullToRefreshListener;
import com.sevtinge.hyperceiler.widget.PullViewHelper;

import java.util.List;

import fan.recyclerview.card.CardItemDecoration;
import fan.springback.view.SpringBackLayout;

public class LogListFragment extends Fragment {

    private static final String ALL_TAG_VALUE = "";
    private static final int STATE_LOADING = 0;
    private static final int STATE_CONTENT = 1;
    private static final int STATE_EMPTY = 2;

    private int mModule;
    private LogAdapter mAdapter;
    private boolean isLoaded = false;

    private View mLoadingLayout;
    private View mEmptyLayout;
    private SpringBackLayout mSpringBackLayout;
    private RecyclerView mRecyclerView;
    private TextView mEmptyText;
    private ImageView mEmptyIcon;

    private String mCurrentKeyword = "";
    private String mCurrentLevel = LogLevelFilter.ALL.getValue();
    private String mCurrentTag = ALL_TAG_VALUE;

    private PullViewHelper mPullViewHelper;
    private boolean mHasRootPermission = false;
    private boolean mHasAnyModuleLogs = false;

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
        View view = inflater.inflate(R.layout.fragment_log_list, container, false);
        mModule = getArguments().getInt("module", 0);

        mSpringBackLayout = view.findViewById(R.id.spring_back);
        mRecyclerView = view.findViewById(R.id.recyclerView);
        mLoadingLayout = view.findViewById(R.id.loading_layout);
        mEmptyLayout = view.findViewById(R.id.empty_layout);
        mEmptyText = view.findViewById(R.id.empty_text);
        mEmptyIcon = view.findViewById(R.id.empty_icon);

        mAdapter = new LogAdapter(requireContext(), this::showLogDetail);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.addItemDecoration(new CardItemDecoration(requireContext()));
        mRecyclerView.setAdapter(mAdapter);

        mPullViewHelper = new PullViewHelper(requireContext(), new PullToRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData(false, true, true);
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

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        maybeLoadInitialData();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            maybeLoadInitialData();
        }
    }

    public void applyFilter(String keyword, String level, String tag) {
        if (mCurrentKeyword.equals(keyword) && mCurrentLevel.equals(level) && mCurrentTag.equals(tag)) {
            return;
        }
        mCurrentKeyword = keyword;
        mCurrentLevel = level;
        mCurrentTag = tag;
        if (getView() != null) {
            refreshData(false, false, false);
        }
    }

    public void forceRefresh() {
        if (getView() != null) {
            refreshData(true, false, true);
        }
    }

    private void maybeLoadInitialData() {
        if (getView() == null || !getUserVisibleHint() || isLoaded) {
            return;
        }
        forceRefresh();
        isLoaded = true;
    }

    private void refreshData() {
        refreshData(true, false, false);
    }

    private void refreshData(boolean showLoading, boolean fromPullRefresh, boolean syncBeforeQuery) {
        if (showLoading) {
            showState(STATE_LOADING);
        }

        final Context context = getContext();
        final String module = mModule == 0 ? "App" : "Xposed";
        final String keyword = mCurrentKeyword;
        final String level = mCurrentLevel;
        final String tag = mCurrentTag;
        final boolean isFiltering = !keyword.isEmpty() || !LogLevelFilter.isAll(level) || !ALL_TAG_VALUE.equals(tag);

        new Thread(() -> {
            boolean hasRootPermission;
            if (mModule == 1) {
                hasRootPermission = ShellUtils.checkRootPermission() == 0;
                if (syncBeforeQuery && hasRootPermission && context != null) {
                    XposedLogLoader.syncLogsToDatabaseSync(context.getApplicationContext());
                }
            } else {
                hasRootPermission = false;
            }

            LogDao dao = LogRepository.getInstance().getDao();
            List<LogEntry> logs = dao.queryLogs(module, level, tag, keyword);
            boolean hasAnyModuleLogs = isFiltering
                ? !dao.queryLogs(module, LogLevelFilter.ALL.getValue(), ALL_TAG_VALUE, "").isEmpty()
                : !logs.isEmpty();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    mHasRootPermission = hasRootPermission;
                    mHasAnyModuleLogs = hasAnyModuleLogs;
                    mAdapter.updateData(logs, keyword);
                    showState(logs.isEmpty() ? STATE_EMPTY : STATE_CONTENT);
                    if (fromPullRefresh) {
                        mPullViewHelper.onPullRefreshComplete();
                    }
                });
            }
        }).start();
    }

    private void showLogDetail(LogEntry entry) {
        if (getActivity() == null) {
            return;
        }
        LogDetailBottomSheet detailBottomSheet = new LogDetailBottomSheet(requireActivity());
        detailBottomSheet.showRecord(entry);
    }

    private void showState(int state) {
        mLoadingLayout.setVisibility(state == STATE_LOADING ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(state == STATE_CONTENT ? View.VISIBLE : View.GONE);
        mEmptyLayout.setVisibility(state == STATE_EMPTY ? View.VISIBLE : View.GONE);

        if (state == STATE_EMPTY) {
            boolean isFiltering = !mCurrentKeyword.isEmpty() || !LogLevelFilter.isAll(mCurrentLevel) ||
                !ALL_TAG_VALUE.equals(mCurrentTag);

            if (mModule == 1 && !mHasRootPermission && !mHasAnyModuleLogs) {
                mEmptyText.setText(R.string.log_empty_root_required);
                mEmptyIcon.setImageResource(R.drawable.ic_empty);
            } else if (isFiltering) {
                mEmptyText.setText(R.string.log_empty_no_results);
                mEmptyIcon.setImageResource(R.drawable.search_list_empty);
            } else {
                mEmptyText.setText(R.string.log_empty_no_logs);
                mEmptyIcon.setImageResource(R.drawable.ic_empty);
            }
        }
    }
}
