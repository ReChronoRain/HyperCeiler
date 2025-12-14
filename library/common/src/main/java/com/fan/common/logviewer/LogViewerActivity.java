package com.fan.common.logviewer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fan.common.R;
import com.fan.common.base.BaseActivity;
import com.fan.common.widget.SearchEditText;
import com.fan.common.widget.SpinnerItemView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fan.nestedheader.widget.NestedHeaderLayout;
import fan.recyclerview.card.CardDefaultItemAnimator;
import fan.recyclerview.card.CardItemDecoration;

public class LogViewerActivity extends BaseActivity
    implements LogAdapter.OnFilterChangeListener {

    private static final String TAG = "LogViewerActivity";

    private AppLogger mAppLogger;
    private ModuleLogger mNetworkLogger;
    private ModuleLogger mDatabaseLogger;

    private NestedHeaderLayout mNestedHeaderLayout;
    private RecyclerView mRecyclerView;
    private LogAdapter mLogAdapter;
    private LogManager mLogManager;

    // 过滤UI组件
    private SearchEditText mSearchEditText;
    private SpinnerItemView mLevelSpinner;
    private SpinnerItemView mModuleSpinner;
    private TextView mFilterStatsTextView;

    // 数据列表
    private final List<String> mLevelList = new ArrayList<>();
    private final List<String> mModuleList = new ArrayList<>();

    private static final int sExportRequestCode = 1001;

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_log;
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        mLogManager = LogManager.getInstance(this);

        initViews();
        setupLoggers();
        // 添加测试日志
        addTestLogs();
    }

    private void initViews() {
        mNestedHeaderLayout = findViewById(R.id.nested_header_layout);
        mRecyclerView = findViewById(R.id.recyclerView);
        mSearchEditText = findViewById(R.id.input);
        mLevelSpinner = findViewById(R.id.spinnerLevel);
        mModuleSpinner = findViewById(R.id.spinnerModule);
        mFilterStatsTextView = findViewById(R.id.textFilterStats);

        mNestedHeaderLayout.setEnableBlur(false);
        registerCoordinateScrollView(mNestedHeaderLayout);

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

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mLogAdapter);
        mRecyclerView.addItemDecoration(new CardItemDecoration(this));
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
        mSearchEditText.setOnSearchListener(this::clearAllFilters);
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

    private void setupLevelFilter() {
        // 初始数据
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
            this, fan.appcompat.R.layout.miuix_appcompat_simple_spinner_integrated_layout, 0x01020014, mLevelList);
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
            this, fan.appcompat.R.layout.miuix_appcompat_simple_spinner_integrated_layout, 0x01020014, mModuleList);
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

    private void setupLoggers() {
        // 从Application获取或直接使用静态方法
        mAppLogger = AppLogger.getInstance();
        mNetworkLogger = new ModuleLogger("Network");
        mDatabaseLogger = new ModuleLogger("Database");
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

    private void exportLogs() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, generateDefaultFileName());
        startActivityForResult(intent, sExportRequestCode);
    }

    private static String generateDefaultFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return "app_" + sdf.format(new Date()) + ".log";
    }

    private void addTestLog() {
        mAppLogger.debug("Test log entry at " + System.currentTimeMillis());
        refreshLogs();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == sExportRequestCode && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                // 这里需要处理文件写入权限，简化处理
                LogManager.getInstance().exportLogs("exported_logs.txt", true);
            }
        }
    }

    @Override
    protected int getMenuResId() {
        return R.menu.menu_log_actions;
    }

    @Override
    protected boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_clear_custom) {
            LogManager.getInstance().clearCustomLogs();
            refreshLogs();
            return true;
        } else if (itemId == R.id.menu_clear_system) {
            LogManager.getInstance().clearSystemLogs();
            refreshLogs();
            return true;
        } else if (itemId == R.id.menu_export) {
            exportLogs();
            return true;
        } else if (itemId == R.id.menu_refresh) {
            refreshLogs();
            return true;
        } else if (itemId == R.id.menu_add_test_log) {
            addTestLog();
            return true;
        }

        return super.onMenuItemClick(item);
    }


    private void addTestLogs() {
        AppLogger appLogger = AppLogger.getInstance();
        ModuleLogger networkLogger = new ModuleLogger("Network");
        ModuleLogger databaseLogger = new ModuleLogger("Database");

        // 添加各种类型的测试日志
        appLogger.verbose("This is a verbose message with detailed information");
        appLogger.debug("Debug information for development");
        appLogger.info("Application initialized successfully");
        appLogger.warn("Warning: Low memory detected");
        appLogger.error("Error: Network connection failed");

        networkLogger.info("Network request started");
        networkLogger.debug("Sending request to: https://api.example.com");
        networkLogger.error("Network timeout after 30 seconds");

        databaseLogger.info("Database connection established");
        databaseLogger.debug("Executing query: SELECT * FROM users");
        databaseLogger.warn("Slow query detected: took 2.5 seconds");
    }

    @Override
    public void onFilterChanged(int filteredCount, int totalCount) {
        String stats = String.format("显示: %d/%d", filteredCount, totalCount);
        mFilterStatsTextView.setText(stats);
    }
}
