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
package com.sevtinge.hyperceiler.logviewer;

import static com.sevtinge.hyperceiler.logviewer.XposedLogLoader.loadLogsSync;

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
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.base.BaseActivity;
import com.sevtinge.hyperceiler.common.widget.SearchEditText;
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.logviewer.widget.LogoPreviewDetailBottomSheet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fan.appcompat.app.ActionBar;
import fan.appcompat.internal.view.menu.MenuBuilder;
import fan.appcompat.widget.HyperPopupMenu;
import fan.miuixbase.widget.FilterSortTabView;
import fan.miuixbase.widget.FilterSortView2;
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
    private TextView mFilterStatsTextView;

    private final List<String> mLevelList = new ArrayList<>();
    private final List<String> mModuleList = new ArrayList<>();

    private final Map<Integer, Boolean> mSortPopoMenuSaveMap = new HashMap<>();
    private final Map<Integer, Boolean[]> mSortPopoMenuSaveSecondMap = new HashMap<>();

    private int mCurrentLogType = 0;

    private static final int sExportRequestCode = 1001;
    private static final int sShareRequestCode = 1002;
    private final Handler mSearchHandler = new Handler(Looper.getMainLooper());
    private Runnable mSearchRunnable;
    private static final long SEARCH_DEBOUNCE_MS = 200;

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_log;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mLogManager = LogManager.getInstance();
        initViews();
        initActionBar();
        loadXposedInBackground();
    }

    private void loadXposedInBackground() {
        if (sXposedLogLoader != null) {
            sXposedLogLoader.loadLogs(this, () -> runOnUiThread(() -> {
                if (mCurrentLogType == 1) {
                    loadDataForCurrentType();
                }
            }));
        }
    }

    private void switchLogType() {
        mFilterStatsTextView.setText(getString(R.string.log_loading));
        loadDataForCurrentType();
    }

    private void loadDataForCurrentType() {
        // 显示加载中
        mFilterStatsTextView.setText(getString(R.string.log_loading));

        if (mLogAdapter == null || mLogManager == null) return;
        if (mCurrentLogType == 1 && !mLogManager.isXposedLogsLoaded()) {
            mLogAdapter.updateData(new ArrayList<>());
            return;
        }
        List<LogEntry> entries;
        if (mCurrentLogType == 0) {
            entries = mLogManager.getLogEntries();
        } else {
            entries = mLogManager.getXposedLogEntries();
        }
        mLogAdapter.updateData(entries);
    }

    private void loadDataAsync() {
        loadDataForCurrentType();
    }

    private void initViews() {
        mNestedHeaderLayout = findViewById(R.id.nested_header_layout);
        mRecyclerView = findViewById(R.id.recyclerView);

        mSearchEditText = findViewById(android.R.id.input);
        mFilterStatsTextView = findViewById(R.id.textFilterStats);

        mNestedHeaderLayout.setEnableBlur(false);
        registerCoordinateScrollView(mNestedHeaderLayout);

        mSearchEditText.setHint(R.string.log_search_hint);

        // 先设置空适配器，快速显示界面
        setupEmptyRecyclerView();
        setupLogTypeFilter();
        setupSearchFilter();

        // 延迟加载数据
        mRecyclerView.post(this::loadDataAsync);
    }

    private void initActionBar() {
        ImageView endView = new ImageView(this);
        endView.setImageResource(R.drawable.ic_function_setting);
        endView.setOnClickListener(v -> showFilterMenu(v));

        ActionBar actionBar = getAppCompatActionBar();
        if (actionBar != null) {
            actionBar.setEndView(endView);
        }
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
        FilterSortView2 mLogType = findViewById(R.id.log_type);

        FilterSortTabView mLogTypeApp = findViewById(R.id.log_type_app);
        FilterSortTabView mLogTypeXposed = findViewById(R.id.log_type_xposed);

        mLogType.setFilteredTab(mCurrentLogType);
        mLogTypeApp.setOnClickListener(v -> {
            mCurrentLogType = 0;
            switchLogType();
        });

        mLogTypeXposed.setOnClickListener(v -> {
            mCurrentLogType = 2;
            switchLogType();
        });
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
        if (mLogAdapter != null) {
            mLogAdapter.clearAllFilters();
        }
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

            if (mRecyclerView != null && mLogAdapter.getItemCount() > 0) {
                mRecyclerView.scrollToPosition(0);
            }
        }
    }

    // ===== 导出日志压缩包 =====
    private void exportLogs() {
        showToast(getString(R.string.log_export_preparing));
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
                            showToast(getString(R.string.log_export_success));
                        } else {
                            showToast(getString(R.string.log_export_failed));
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
        showToast(getString(R.string.log_share_preparing));

        new Thread(() -> {
            try {
                try {
                    loadLogsSync();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to sync Xposed logs before share", e);
                }

                File zipFile = mLogManager.createLogZipFile();

                if (zipFile == null || !zipFile.exists()) {
                    runOnUiThread(() -> showToast(getString(R.string.log_share_failed)));
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
                        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.log_share_text));
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        startActivityForResult(
                            Intent.createChooser(shareIntent, getString(R.string.log_share_title)),
                            sShareRequestCode
                        );
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "FileProvider configuration error", e);
                        cleanShareCache();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to share logs", e);
                        showToast(getString(R.string.log_share_failed));
                        cleanShareCache();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to create log zip for sharing", e);
                runOnUiThread(() -> showToast(getString(R.string.log_share_failed)));
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
            .setTitle(R.string.log_clear_title)
            .setMessage(R.string.log_clear_all_message)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                showToast(getString(R.string.log_clear_in_progress));

                new Thread(() -> {
                    try {
                        mLogManager.clearAllLogs();
                        runOnUiThread(() -> {
                            refreshLogs();
                            showToast(getString(R.string.log_clear_success));
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to clear logs", e);
                        runOnUiThread(() -> showToast(getString(R.string.log_clear_failed)));
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
        showToast(getString(R.string.log_copy_success));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_log_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_clear) {
            clearCurrentLogs();
            return true;
        } else if (itemId == R.id.menu_export) {
            exportLogs();
            return true;
        } else if (itemId == R.id.menu_share) {
            shareLogs();
            return true;
        }
        return super.onMenuItemClick(item);
    }

    @Override
    public void onFilterChanged(int filteredCount, int totalCount) {
        if (mCurrentLogType == 1 && mLogManager != null && !mLogManager.isXposedLogsLoaded()) {
            mFilterStatsTextView.setText(getString(R.string.log_loading));
            return;
        }
        String stats = getString(R.string.log_filter_stats, filteredCount, totalCount);
        mFilterStatsTextView.setText(stats);
    }

    @Override
    public void onLogItemClick(LogEntry logEntry) {
        showLogDetailBottomSheet(logEntry);
    }

    private void showLogDetailBottomSheet(LogEntry logEntry) {
        LogoPreviewDetailBottomSheet bottomSheet = new LogoPreviewDetailBottomSheet(this);
        bottomSheet.initView(this, logEntry, v -> copyLogToClipboard(logEntry));
        bottomSheet.show();
    }

    public final void showFilterMenu(View view) {
        HyperPopupMenu mPopupMenu = new HyperPopupMenu(this, view);
        mPopupMenu.inflate(R.menu.log_sort_menu);

        injectDynamicSubMenu(mPopupMenu, R.id.log_level, mLevelList);
        injectDynamicSubMenu(mPopupMenu, R.id.log_tag, mModuleList);

        mPopupMenu.preCheckPrimaryItem(mSortPopoMenuSaveMap);
        mPopupMenu.preCheckSecondaryItem(mSortPopoMenuSaveSecondMap);

        mPopupMenu.notifyDataChanged();

        mPopupMenu.setOnMenuItemClickListener(item -> {
            int groupId = item.getGroupId();
            int index = item.getItemId();

            if (groupId == R.id.log_level || groupId == R.id.log_tag) {
                if (groupId == R.id.log_level) {
                    if (mLogAdapter != null) {
                        mLogAdapter.setLevelFilter(index < mLevelList.size() ? mLevelList.get(index) : "ALL");
                    }
                } else {
                    if (mLogAdapter != null) {
                        mLogAdapter.setModuleFilter(index < mModuleList.size() ? mModuleList.get(index) : "ALL");
                    }
                }
                updateSecondaryMapState(groupId, index);
            }
            mPopupMenu.savePrimaryCheckedMap(mSortPopoMenuSaveMap);
            mPopupMenu.saveSecondaryCheckedMap(mSortPopoMenuSaveSecondMap);
        });
        mPopupMenu.show();
    }

    /**
     * 向指定的子菜单（app_filter 或 app_order）动态添加单选列表
     *
     * @param popupMenu HyperPopupMenu 实例
     * @param parentId  父菜单项 ID (R.id.app_filter 或 R.id.app_order)
     * @param items     动态字符串列表
     */
    private void injectDynamicSubMenu(HyperPopupMenu popupMenu, int parentId, List<String> items) {
        Menu menu = popupMenu.getMenu();
        MenuItem parentItem = menu.findItem(parentId);

        if (parentItem == null) return;

        SubMenu subMenu = parentItem.getSubMenu();
        subMenu.clear();

        Boolean[] status = mSortPopoMenuSaveSecondMap.get(parentId);
        if (status == null || status.length != items.size()) {
            status = new Boolean[items.size()];
            for (int i = 0; i < items.size(); i++) {
                status[i] = (i == 0);
            }
            mSortPopoMenuSaveSecondMap.put(parentId, status);
        }

        for (int i = 0; i < items.size(); i++) {
            MenuItem subItem = subMenu.add(parentId, i, Menu.NONE, items.get(i));
            subItem.setCheckable(true);
            subItem.setChecked(status[i]);
        }
    }

    private void updateSecondaryMapState(int groupId, int index) {
        Boolean[] status = mSortPopoMenuSaveSecondMap.get(groupId);
        if (status != null && index < status.length) {
            Arrays.fill(status, Boolean.FALSE);
            status[index] = Boolean.TRUE;
        }
    }

}
