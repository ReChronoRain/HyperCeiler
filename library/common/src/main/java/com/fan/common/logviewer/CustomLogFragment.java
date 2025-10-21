package com.fan.common.logviewer;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fan.common.R;
import com.fan.common.widget.SearchEditText;
import com.fan.common.widget.SpinnerItemView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fan.androidbase.widget.ClearableEditText;
import fan.animation.Folme;
import fan.animation.ITouchStyle;
import fan.animation.base.AnimConfig;
import fan.appcompat.widget.Spinner;
import fan.recyclerview.card.CardDefaultItemAnimator;
import fan.recyclerview.card.CardItemDecoration;
import fan.recyclerview.widget.RecyclerView;

public class CustomLogFragment extends Fragment implements LogAdapter.OnFilterChangeListener {

    private static final String TAG = "CustomLogFragment";

    private RecyclerView mRecyclerView;
    private LogAdapter mLogAdapter;
    private LogManager mLogManager;

    // 过滤UI组件
    private SearchEditText mSearchEditText;
    private SpinnerItemView mLevelSpinner;
    private SpinnerItemView mModuleSpinner;
    private TextView mFilterStatsTextView;

    // 数据列表
    private List<String> mLevelList = new ArrayList<>();
    private List<String> mModuleList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLogManager = LogManager.getInstance(requireContext());

        setupViews(view);
    }

    private void setupViews(View rootView) {
        mRecyclerView = rootView.findViewById(R.id.recyclerView);
        mSearchEditText = rootView.findViewById(R.id.input);
        mLevelSpinner = rootView.findViewById(R.id.spinnerLevel);
        mModuleSpinner = rootView.findViewById(R.id.spinnerModule);
        mFilterStatsTextView = rootView.findViewById(R.id.textFilterStats);

        mSearchEditText.setHint("搜索日志");

        setupRecyclerView();
        setupSearchFilter();
        setupLevelFilter();
        setupModuleFilter();
    }

    private void setupRecyclerView() {
        List<LogEntry> logEntries = mLogManager != null ?
            mLogManager.getLogEntries() : new ArrayList<>();

        mLogAdapter = new LogAdapter(logEntries);
        mLogAdapter.setOnFilterChangeListener(this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mLogAdapter);
        mRecyclerView.addItemDecoration(new CardItemDecoration(requireContext()));
        mRecyclerView.setItemAnimator(new CardDefaultItemAnimator());

        // 更新过滤器选项
        updateList();
    }

    private void updateList() {
        if (mLogAdapter != null) {
            mLevelList.addAll(mLogAdapter.getLevelList());
            mModuleList.addAll(mLogAdapter.getModuleList());
        }
    }

    private void setupSearchFilter() {
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (mLogAdapter != null) {
                    mLogAdapter.setSearchKeyword(s.toString());
                }
            }
        });
        mSearchEditText.setOnSearchListener(() -> clearAllFilters());
    }

    private void setupLevelFilter() {
        // 初始数据
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
                requireContext(), fan.appcompat.R.layout.miuix_appcompat_simple_spinner_integrated_layout, 0x01020014, mLevelList);
        levelAdapter.setDropDownViewResource(fan.appcompat.R.layout.miuix_appcompat_simple_spinner_dropdown_item);
        mLevelSpinner.setAdapter(levelAdapter);
        mLevelSpinner.setSelection(0); // 默认选择全部

        mLevelSpinner.getSpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mLogAdapter == null) return;
                try {
                    // 安全地获取选中的级别
                    if (position >= 0 && position < mLevelList.size()) {
                        String selectedLevel = mLevelList.get(position);
                        mLogAdapter.setLevelFilter(selectedLevel);
                        Log.d(TAG, "Level filter set to: " + selectedLevel);
                    } else {
                        Log.w(TAG, "Invalid level position: " + position);
                        // 安全回退
                        mLogAdapter.setLevelFilter("ALL");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in level spinner selection", e);
                    // 安全回退
                    if (mLogAdapter != null) {
                        mLogAdapter.setLevelFilter("ALL");
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 安全处理
                if (mLogAdapter != null) {
                    mLogAdapter.setLevelFilter("ALL");
                }
            }
        });
    }

    private void setupModuleFilter() {
        // 初始数据
        ArrayAdapter<String> moduleAdapter = new ArrayAdapter<>(
                requireContext(), fan.appcompat.R.layout.miuix_appcompat_simple_spinner_integrated_layout, 0x01020014, mModuleList);
        moduleAdapter.setDropDownViewResource(fan.appcompat.R.layout.miuix_appcompat_simple_spinner_dropdown_item);
        mModuleSpinner.setAdapter(moduleAdapter);
        mModuleSpinner.setSelection(0); // 默认选择全部

        mModuleSpinner.getSpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mLogAdapter == null) return;

                try {
                    // 安全地获取选中的模块 - 这是修复的关键！
                    if (position >= 0 && position < mModuleList.size()) {
                        String selectedModule = mModuleList.get(position);
                        mLogAdapter.setModuleFilter(selectedModule);
                        Log.d(TAG, "Module filter set to: " + selectedModule);
                    } else {
                        Log.w(TAG, "Invalid module position: " + position + ", list size: " + mModuleList.size());
                        // 安全回退到ALL
                        mLogAdapter.setModuleFilter("ALL");
                        // 重置选择
                        if (mModuleSpinner != null) {
                            mModuleSpinner.setSelection(0);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in module spinner selection", e);
                    // 安全回退
                    if (mLogAdapter != null) {
                        mLogAdapter.setModuleFilter("ALL");
                    }
                    if (mModuleSpinner != null) {
                        mModuleSpinner.setSelection(0);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 安全处理
                if (mLogAdapter != null) {
                    mLogAdapter.setModuleFilter("ALL");
                }
            }
        });
    }

    private void clearAllFilters() {
        // 清除搜索
        if (mSearchEditText != null) {
            mSearchEditText.setText("");
        }

        // 重置Spinner选择
        if (mLevelSpinner != null) {
            mLevelSpinner.setSelection(0);
        }
        if (mModuleSpinner != null) {
            mModuleSpinner.setSelection(0);
        }

        // 应用清除
        if (mLogAdapter != null) {
            mLogAdapter.clearAllFilters();
        }
    }

    public void refreshLogs() {
        if (mLogAdapter != null && mLogManager != null) {
            mLogAdapter.updateData(mLogManager.getLogEntries());
            updateList();

            // 滚动到底部
            if (mRecyclerView != null && mLogAdapter.getItemCount() > 0) {
                mRecyclerView.scrollToPosition(mLogAdapter.getItemCount() - 1);
            }
        }
    }

    /*private void updateModuleFilterOptions() {
        if (mLogAdapter != null) {
            Set<String> availableModules = mLogAdapter.getAvailableModules();
            List<String> moduleList = new ArrayList<>(availableModules);

            ArrayAdapter<String> moduleAdapter = new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_item, moduleList);
            moduleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mModuleSpinner.setAdapter(moduleAdapter);
        }
    }*/

    @Override
    public void onFilterChanged(int filteredCount, int totalCount) {
        String stats = String.format("显示: %d/%d", filteredCount, totalCount);
        mFilterStatsTextView.setText(stats);
    }
}
