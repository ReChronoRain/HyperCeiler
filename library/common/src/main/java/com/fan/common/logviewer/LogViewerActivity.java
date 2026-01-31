/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.fan.common.logviewer;

import static com.fan.common.logviewer.XposedLogLoader.loadLogsSync;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fan.common.base.BaseActivity;
import com.fan.common.widget.SearchEditText;
import com.fan.common.widget.SpinnerItemView;
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fan.nestedheader.widget.NestedHeaderLayout;
import fan.recyclerview.card.CardDefaultItemAnimator;
import fan.recyclerview.card.CardItemDecoration;

public class LogViewerActivity extends BaseActivity
    implements LogAdapter.OnFilterChangeListener, LogAdapter.OnLogItemClickListener {

    private static final String TAG = "LogViewerActivity";
    private static final String FILE_PROVIDER_AUTHORITY = ProjectApi.mAppModulePkg + ".fileprovider";

    public interface XposedLogLoader {
        void loadLogs(Context context, Runnable onComplete);
    }

    private static XposedLogLoader sXposedLogLoader;

    public static void setXposedLogLoader(XposedLogLoader loader) {
        sXposedLogLoader = loader;
    }

    private NestedHeaderLayout mNestedHeaderLayout;
    private RecyclerView mRecyclerView;
    private LogAdapter mLogAdapter;
    private LogManager mLogManager;

    private SearchEditText mSearchEditText;
    private SpinnerItemView mLogTypeSpinner;
    private SpinnerItemView mLevelSpinner;
    private SpinnerItemView mModuleSpinner;
    private TextView mFilterStatsTextView;

    private final List<String> mLogTypeList = new ArrayList<>();
    private final List<String> mLevelList = new ArrayList<>();
    private final List<String> mModuleList = new ArrayList<>();

    private int mCurrentLogType = 0;

    private static final int sExportRequestCode = 1001;
    private static final int sShareRequestCode = 1002;
    private final Handler mSearchHandler = new Handler(Looper.getMainLooper());
    private Runnable mSearchRunnable;
    private static final long SEARCH_DEBOUNCE_MS = 200;

    @Override
    protected int getContentLayoutId() {
        return com.fan.common.R.layout.fragment_log;
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        mLogManager = LogManager.getInstance();
        loadXposedLogsAndInit();
    }

    private void loadXposedLogsAndInit() {
        if (sXposedLogLoader != null) {
            sXposedLogLoader.loadLogs(this, () -> {
                new Handler(Looper.getMainLooper()).post(this::initViews);
            });
        } else {
            initViews();
        }
    }

    private void initViews() {
        mNestedHeaderLayout = findViewById(com.fan.common.R.id.nested_header_layout);
        mRecyclerView = findViewById(com.fan.common.R.id.recyclerView);
        mSearchEditText = findViewById(com.fan.common.R.id.input);
        mLogTypeSpinner = findViewById(com.fan.common.R.id.spinnerLogType);
        mLevelSpinner = findViewById(com.fan.common.R.id.spinnerLevel);
        mModuleSpinner = findViewById(com.fan.common.R.id.spinnerModule);
        mFilterStatsTextView = findViewById(com.fan.common.R.id.textFilterStats);

        mNestedHeaderLayout.setEnableBlur(false);
        registerCoordinateScrollView(mNestedHeaderLayout);

        mSearchEditText.setHint(com.sevtinge.hyperceiler.core.R.string.log_search_hint);

        // 先设置空适配器，快速显示界面
        setupEmptyRecyclerView();
        setupLogTypeFilter();
        setupSearchFilter();

        // 延迟加载数据
        mRecyclerView.post(this::loadDataAsync);
    }

    private void setupEmptyRecyclerView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new CardItemDecoration(this));
        mRecyclerView.setItemAnimator(new CardDefaultItemAnimator());

        mLogAdapter = new LogAdapter(this, new ArrayList<>());
        mLogAdapter.setOnFilterChangeListener(this);
        mLogAdapter.setOnLogItemClickListener(this);
        mLogAdapter.setOnDataUpdateListener(this::onAdapterDataUpdated);

        mRecyclerView.setAdapter(mLogAdapter);
    }

    private void onAdapterDataUpdated() {
        updateList();
        refreshFilterSpinners();
        setupSpinnerListeners();
    }

    private void loadDataAsync() {
        new Thread(() -> {
            List<LogEntry> logEntries = mLogManager != null ? mLogManager.getLogEntries() : new ArrayList<>();

            runOnUiThread(() -> {
                if (mLogAdapter != null) {
                    mLogAdapter.updateData(logEntries);
                }
            });
        }).start();
    }

    private void setupSpinnerListeners() {
        mLevelSpinner.getSpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mLogAdapter == null) return;
                try {
                    if (position >= 0 && position < mLevelList.size()) {
                        mLogAdapter.setLevelFilter(mLevelList.get(position));
                    } else {
                        mLogAdapter.setLevelFilter("ALL");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in level spinner selection", e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (mLogAdapter != null) mLogAdapter.setLevelFilter("ALL");
            }
        });

        mModuleSpinner.getSpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mLogAdapter == null) return;
                try {
                    if (position >= 0 && position < mModuleList.size()) {
                        mLogAdapter.setModuleFilter(mModuleList.get(position));
                    } else {
                        mLogAdapter.setModuleFilter("ALL");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in module spinner selection", e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (mLogAdapter != null) mLogAdapter.setModuleFilter("ALL");
            }
        });
    }

    private void updateList() {
        mLevelList.clear();
        mModuleList.clear();
        if (mLogAdapter != null) {
            mLevelList.addAll(mLogAdapter.getLevelList());
            mModuleList.addAll(mLogAdapter.getModuleList());
        }
    }

    private void setupLogTypeFilter() {
        mLogTypeList.clear();
        mLogTypeList.add(getString(com.sevtinge.hyperceiler.core.R.string.log_type_app));
        mLogTypeList.add(getString(com.sevtinge.hyperceiler.core.R.string.log_type_xposed));

        ArrayAdapter<String> logTypeAdapter = new ArrayAdapter<>(
            this, fan.appcompat.R.layout.miuix_appcompat_simple_spinner_integrated_layout, 0x01020014, mLogTypeList);
        logTypeAdapter.setDropDownViewResource(fan.appcompat.R.layout.miuix_appcompat_simple_spinner_dropdown_item);
        mLogTypeSpinner.setAdapter(logTypeAdapter);
        mLogTypeSpinner.setSelection(0);

        mLogTypeSpinner.getSpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != mCurrentLogType) {
                    mCurrentLogType = position;
                    switchLogType(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void switchLogType(int logType) {
        List<LogEntry> logEntries;
        if (logType == 0) {
            logEntries = mLogManager != null ? mLogManager.getLogEntries() : new ArrayList<>();
        } else {
            logEntries = mLogManager != null ? mLogManager.getXposedLogEntries() : new ArrayList<>();
        }

        if (mLogAdapter != null) {
            mLogAdapter.updateData(logEntries);
            // 回调会处理后续操作
        }

        if (mRecyclerView != null && mLogAdapter != null && mLogAdapter.getItemCount() > 0) {
            mRecyclerView.scrollToPosition(0);
        }
    }

    private void refreshFilterSpinners() {
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
            this, fan.appcompat.R.layout.miuix_appcompat_simple_spinner_integrated_layout,
            0x01020014, mLevelList);
        levelAdapter.setDropDownViewResource(fan.appcompat.R.layout.miuix_appcompat_simple_spinner_dropdown_item);
        mLevelSpinner.setAdapter(levelAdapter);
        mLevelSpinner.setSelection(0);

        ArrayAdapter<String> moduleAdapter = new ArrayAdapter<>(
            this, fan.appcompat.R.layout.miuix_appcompat_simple_spinner_integrated_layout,
            0x01020014, mModuleList);
        moduleAdapter.setDropDownViewResource(fan.appcompat.R.layout.miuix_appcompat_simple_spinner_dropdown_item);
        mModuleSpinner.setAdapter(moduleAdapter);
        mModuleSpinner.setSelection(0);
    }

    private void setupSearchFilter() {
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (mSearchRunnable != null) {
                    mSearchHandler.removeCallbacks(mSearchRunnable);
                }

                final String keyword = s.toString();
                mSearchRunnable = () -> {
                    if (mLogAdapter != null) {
                        mLogAdapter.setSearchKeyword(keyword);
                    }};

                mSearchHandler.postDelayed(mSearchRunnable, SEARCH_DEBOUNCE_MS);
            }
        });

        mSearchEditText.setOnSearchListener(this::clearAllFilters);
    }

    private void clearAllFilters() {
        if (mSearchEditText != null) {
            mSearchEditText.setText("");
        }
        if (mLevelSpinner != null) {
            mLevelSpinner.setSelection(0);
        }
        if (mModuleSpinner != null) {
            mModuleSpinner.setSelection(0);
        }
        if (mLogAdapter != null) {
            mLogAdapter.clearAllFilters();
        }
    }

    private void setupLevelFilter() {
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
            this, fan.appcompat.R.layout.miuix_appcompat_simple_spinner_integrated_layout, 0x01020014, mLevelList);
        levelAdapter.setDropDownViewResource(fan.appcompat.R.layout.miuix_appcompat_simple_spinner_dropdown_item);
        mLevelSpinner.setAdapter(levelAdapter);
        mLevelSpinner.setSelection(0);

        mLevelSpinner.getSpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mLogAdapter == null) return;
                try {
                    if (position >= 0 && position < mLevelList.size()) {
                        String selectedLevel = mLevelList.get(position);
                        mLogAdapter.setLevelFilter(selectedLevel);
                    } else {
                        mLogAdapter.setLevelFilter("ALL");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in level spinner selection", e);
                    if (mLogAdapter != null) {
                        mLogAdapter.setLevelFilter("ALL");
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (mLogAdapter != null) {
                    mLogAdapter.setLevelFilter("ALL");
                }
            }
        });
    }

    private void setupModuleFilter() {
        ArrayAdapter<String> moduleAdapter = new ArrayAdapter<>(
            this, fan.appcompat.R.layout.miuix_appcompat_simple_spinner_integrated_layout, 0x01020014, mModuleList);
        moduleAdapter.setDropDownViewResource(fan.appcompat.R.layout.miuix_appcompat_simple_spinner_dropdown_item);
        mModuleSpinner.setAdapter(moduleAdapter);
        mModuleSpinner.setSelection(0);

        mModuleSpinner.getSpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mLogAdapter == null) return;
                try {
                    if (position >= 0 && position < mModuleList.size()) {
                        String selectedModule = mModuleList.get(position);
                        mLogAdapter.setModuleFilter(selectedModule);
                    } else {
                        mLogAdapter.setModuleFilter("ALL");
                        if (mModuleSpinner != null) {
                            mModuleSpinner.setSelection(0);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in module spinner selection", e);
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
                if (mLogAdapter != null) {
                    mLogAdapter.setModuleFilter("ALL");
                }
            }
        });
    }

    public void refreshLogs() {
        if (mLogAdapter != null && mLogManager != null) {
            List<LogEntry> logEntries;
            if (mCurrentLogType == 0) {
                logEntries = mLogManager.getLogEntries();
            } else {
                logEntries = mLogManager.getXposedLogEntries();
            }
            mLogAdapter.updateData(logEntries);
            updateList();
            refreshFilterSpinners();

            if (mRecyclerView != null && mLogAdapter.getItemCount() > 0) {
                mRecyclerView.scrollToPosition(0);
            }
        }
    }

    // ===== 导出日志压缩包 =====
    private void exportLogs() {
        showToast(getString(com.sevtinge.hyperceiler.core.R.string.log_export_preparing));
        new Thread(() -> {

            try {
                loadLogsSync();
            } catch (Exception e) {
                Log.w(TAG, "Failed to sync Xposed logs before export", e);
            }

            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                intent.putExtra(Intent.EXTRA_TITLE, LogManager.generateZipFileName());
                startActivityForResult(intent, sExportRequestCode);
            });
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == sExportRequestCode && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                new Thread(() -> {
                    boolean success = mLogManager.exportLogsZipToUri(uri);
                    runOnUiThread(() -> {
                        if (success) {
                            showToast(getString(com.sevtinge.hyperceiler.core.R.string.log_export_success));
                        } else {
                            showToast(getString(com.sevtinge.hyperceiler.core.R.string.log_export_failed));
                        }
                    });
                }).start();
            }
        } else if (requestCode == sShareRequestCode) {
            cleanShareCache();
        }
    }

    // ===== 分享日志压缩包 =====
    private void shareLogs() {
        showToast(getString(com.sevtinge.hyperceiler.core.R.string.log_share_preparing));

        new Thread(() -> {
            try {
                try {
                    loadLogsSync();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to sync Xposed logs before share", e);
                }

                File zipFile = mLogManager.createLogZipFile();

                if (zipFile == null || !zipFile.exists()) {
                    runOnUiThread(() -> showToast(getString(com.sevtinge.hyperceiler.core.R.string.log_share_failed)));
                    return;
                }

                runOnUiThread(() -> {
                    try {
                        Uri contentUri = FileProvider.getUriForFile(
                            LogViewerActivity.this,
                            FILE_PROVIDER_AUTHORITY,
                            zipFile
                        );

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("application/zip");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "HyperCeiler Logs");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(com.sevtinge.hyperceiler.core.R.string.log_share_text));
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        startActivityForResult(
                            Intent.createChooser(shareIntent, getString(com.sevtinge.hyperceiler.core.R.string.log_share_title)),
                            sShareRequestCode
                        );
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "FileProvider configuration error", e);
                        cleanShareCache();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to share logs", e);
                        showToast(getString(com.sevtinge.hyperceiler.core.R.string.log_share_failed));
                        cleanShareCache();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to create log zip for sharing", e);
                runOnUiThread(() -> showToast(getString(com.sevtinge.hyperceiler.core.R.string.log_share_failed)));
            }
        }).start();
    }

    private void cleanShareCache() {
        new Thread(() -> {
            try {
                File cacheDir = new File(getCacheDir(), "log_export");
                if (cacheDir.exists()) {
                    File[] files = cacheDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (!file.delete()) {
                                Log.w(TAG, "Failed to delete cache file: " + file.getAbsolutePath());
                            }
                        }
                    }
                }
                Log.d(TAG, "Share cache cleaned");
            } catch (Exception e) {
                Log.w(TAG, "Failed to clean share cache", e);
            }
        }).start();
    }


    // ===== 清空日志 =====
    private void clearCurrentLogs() {
        new fan.appcompat.app.AlertDialog.Builder(this)
            .setTitle(com.sevtinge.hyperceiler.core.R.string.log_clear_title)
            .setMessage(com.sevtinge.hyperceiler.core.R.string.log_clear_all_message)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                showToast(getString(com.sevtinge.hyperceiler.core.R.string.log_clear_in_progress));

                new Thread(() -> {
                    try {
                        mLogManager.clearAllLogs();
                        runOnUiThread(() -> {
                            refreshLogs();
                            showToast(getString(com.sevtinge.hyperceiler.core.R.string.log_clear_success));
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to clear logs", e);
                        runOnUiThread(() -> showToast(getString(com.sevtinge.hyperceiler.core.R.string.log_clear_failed)));
                    }
                }).start();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    // ===== 复制日志到剪贴板 =====
    private void copyLogToClipboard(LogEntry logEntry) {
        String logText = LogManager.formatLogEntryForCopy(logEntry);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("HyperCeiler Log", logText);
        clipboard.setPrimaryClip(clip);
        showToast(getString(com.sevtinge.hyperceiler.core.R.string.log_copy_success));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.fan.common.R.menu.menu_log_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == com.fan.common.R.id.menu_clear) {
            clearCurrentLogs();
            return true;
        } else if (itemId == com.fan.common.R.id.menu_export) {
            exportLogs();
            return true;
        } else if (itemId == com.fan.common.R.id.menu_share) {
            shareLogs();
            return true;
        }
        return super.onMenuItemClick(item);
    }

    @Override
    public void onFilterChanged(int filteredCount, int totalCount) {
        String stats = getString(com.sevtinge.hyperceiler.core.R.string.log_filter_stats, filteredCount, totalCount);
        mFilterStatsTextView.setText(stats);
    }

    @Override
    public void onLogItemClick(LogEntry logEntry) {
        showLogDetailDialog(logEntry);
    }

    private void showLogDetailDialog(LogEntry logEntry) {
        String title = getString(com.sevtinge.hyperceiler.core.R.string.log_detail_title);
        String message = "[" + getString(com.sevtinge.hyperceiler.core.R.string.log_detail_time) + "]: " + logEntry.getFormattedTime() +"\n[" + getString(com.sevtinge.hyperceiler.core.R.string.log_detail_level) + "]: " + logEntry.getLevel() +
            "\n[" + getString(com.sevtinge.hyperceiler.core.R.string.log_detail_tag) + "]: " + logEntry.getTag() +
            "\n[" + getString(com.sevtinge.hyperceiler.core.R.string.log_detail_message) + "]:\n" + logEntry.getMessage();

        new fan.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
            .setNeutralButton(com.sevtinge.hyperceiler.core.R.string.log_copy, (dialog, which) -> copyLogToClipboard(logEntry))
            .show();
    }
}
