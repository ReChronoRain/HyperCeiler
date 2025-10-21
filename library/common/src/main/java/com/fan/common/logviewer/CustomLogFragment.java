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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private EditText mSearchEditText;
    private Spinner mLevelSpinner;
    private Spinner mModuleSpinner;
    private TextView mFilterStatsTextView;
    private View mClearFiltersButton;

    // 数据列表
    private List<String> mLevelList;
    private List<String> mModuleList;

    // 标记是否正在初始化，避免不必要的回调
    private boolean mIsInitializing = false;

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

        // 初始化数据列表
        mLevelList = new ArrayList<>();
        mModuleList = new ArrayList<>();

        setupFilterViews(view);
        setupRecyclerView();
    }

    private void setupFilterViews(View rootView) {
        mRecyclerView = rootView.findViewById(R.id.recyclerView);
        mSearchEditText = rootView.findViewById(R.id.editSearch);
        mLevelSpinner = rootView.findViewById(R.id.spinnerLevel);
        mModuleSpinner = rootView.findViewById(R.id.spinnerModule);
        mFilterStatsTextView = rootView.findViewById(R.id.textFilterStats);
        mClearFiltersButton = rootView.findViewById(R.id.buttonClearFilters);

        setupSearchFilter();
        setupLevelFilter();
        setupModuleFilter();
        setupClearFilters();

        // 初始更新过滤器选项
        updateFilterOptions();
    }

    private void setupSearchFilter() {
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (mLogAdapter != null && !mIsInitializing) {
                    mLogAdapter.setSearchKeyword(s.toString());
                }
            }
        });
    }

    private void setupLevelFilter() {
        // 初始数据
        // 初始化级别列表 - 使用固定列表避免动态变化
        mLevelList.clear();
        mLevelList.add("ALL");
        mLevelList.add("V");
        mLevelList.add("D");
        mLevelList.add("I");
        mLevelList.add("W");
        mLevelList.add("E");

        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
                requireContext(), fan.appcompat.R.layout.miuix_appcompat_simple_spinner_integrated_layout, 0x01020014, mLevelList);
        levelAdapter.setDropDownViewResource(fan.appcompat.R.layout.miuix_appcompat_simple_spinner_dropdown_item);
        mLevelSpinner.setAdapter(levelAdapter);

        mIsInitializing = true;
        mLevelSpinner.setSelection(0); // 默认选择"ALL"
        mIsInitializing = false;

        setSpinnerDisplayLocation((ViewGroup) mLevelSpinner.getParent(), mLevelSpinner);
        mLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mIsInitializing || mLogAdapter == null) {
                    return;
                }

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

    @SuppressLint("ClickableViewAccessibility")
    public void setSpinnerDisplayLocation(ViewGroup parent, Spinner spinner) {
        if (parent != null && spinner != null) {
            spinner.setClickable(false);
            spinner.setLongClickable(false);
            spinner.setContextClickable(false);
            spinner.setOnSpinnerDismissListener(() ->
                Folme.useAt(parent).touch().touchUp(new AnimConfig[0]));
            parent.setOnTouchListener((v, event) -> {
                if (spinner.isEnabled()) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN -> Folme.useAt(v).touch().setScale(1.0f, new ITouchStyle.TouchType[0]).touchDown(new AnimConfig[0]);
                        case MotionEvent.ACTION_UP -> spinner.performClick(event.getX(), event.getY());
                        case MotionEvent.ACTION_CANCEL -> Folme.useAt(v).touch().touchUp(new AnimConfig[0]);
                    }
                    return true;
                } else {
                    return false;
                }
            });
        }
    }

    private void setupModuleFilter() {
        // 初始化模块列表
        mModuleList.clear();
        mModuleList.add("ALL"); // 始终包含ALL

        ArrayAdapter<String> moduleAdapter = new ArrayAdapter<>(
                requireContext(), fan.appcompat.R.layout.miuix_appcompat_simple_spinner_integrated_layout, 0x01020014, mModuleList);
        moduleAdapter.setDropDownViewResource(fan.appcompat.R.layout.miuix_appcompat_simple_spinner_dropdown_item);
        mModuleSpinner.setAdapter(moduleAdapter);

        mIsInitializing = true;
        mModuleSpinner.setSelection(0); // 默认选择"ALL"
        mIsInitializing = false;

        setSpinnerDisplayLocation((ViewGroup) mModuleSpinner.getParent(), mModuleSpinner);
        mModuleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mIsInitializing || mLogAdapter == null) {
                    return;
                }

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

    private void setupClearFilters() {
        mClearFiltersButton.setOnClickListener(v -> {
            clearAllFilters();
        });
    }

    private void clearAllFilters() {
        mIsInitializing = true;

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

        mIsInitializing = false;

        // 应用清除
        if (mLogAdapter != null) {
            mLogAdapter.clearAllFilters();
        }
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
        updateFilterOptions();
    }

    private void updateFilterOptions() {
        if (mLogAdapter == null) {
            return;
        }

        // 更新模块列表
        updateModuleList();
    }

    private void updateModuleList() {
        if (mLogAdapter == null || mModuleSpinner == null) {
            return;
        }

        try {
            mIsInitializing = true;

            // 获取当前选中的模块（在更新前保存）
            String currentSelection = "ALL";
            int currentPosition = mModuleSpinner.getSelectedItemPosition();
            if (currentPosition >= 0 && currentPosition < mModuleList.size()) {
                currentSelection = mModuleList.get(currentPosition);
            }

            // 获取可用的模块
            List<String> availableModules = mLogAdapter.getModuleList();
            mModuleList.clear();
            mModuleList.addAll(availableModules);

            // 通知适配器更新
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) mModuleSpinner.getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            // 尝试恢复之前的选择，如果不存在则选择ALL
            int newPosition = mModuleList.indexOf(currentSelection);
            if (newPosition == -1) {
                newPosition = 0; // ALL
            }
            mModuleSpinner.setSelection(newPosition);

            mIsInitializing = false;

        } catch (Exception e) {
            Log.e(TAG, "Error updating module list", e);
            mIsInitializing = false;

            // 安全回退
            if (mModuleSpinner != null) {
                mModuleSpinner.setSelection(0);
            }
        }
    }

    public void refreshLogs() {
        if (mLogAdapter != null && mLogManager != null) {
            mLogAdapter.updateData(mLogManager.getLogEntries());
            updateFilterOptions();

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

    @Override
    public void onDestroyView() {
        // 清理资源
        mIsInitializing = true;
        super.onDestroyView();
    }
}
