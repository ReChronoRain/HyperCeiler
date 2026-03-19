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

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.base.BaseActivity;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.widget.SearchEditText;
import com.sevtinge.hyperceiler.log.db.LogRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fan.appcompat.widget.HyperPopupMenu;
import fan.miuixbase.widget.FilterSortTabView;
import fan.miuixbase.widget.FilterSortView2;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.viewpager.widget.ViewPager;

public class LogViewerActivity extends BaseActivity {

    private static final String TAG = "LogViewerActivity";
    private static final String ALL_TAG_VALUE = "";

    private ViewPager mViewPager;
    private LogPagerAdapter mPagerAdapter;

    private NestedHeaderLayout mNestedHeaderLayout;
    private SearchEditText mSearchEditText;
    private FilterSortView2 mLogType;
    private FilterSortTabView mLogTypeApp;
    private FilterSortTabView mLogTypeXposed;

    private int mCurrentType = 0;

    // 筛选状态记录
    private String mKeyword = "";
    private int mSelectedLevelPos = 0;

    private int mSelectedTagPos = 0; // 当前选中的 Tag 索引
    private List<String> mCurrentAvailableTags = new ArrayList<>(List.of(ALL_TAG_VALUE)); // 当前 Tab 可用的 Tag
    private final Map<Integer, Boolean[]> mPopupCheckedState = new HashMap<>();
    private final ActivityResultLauncher<Intent> mExportLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) {
                return;
            }
            Uri uri = result.getData().getData();
            new Thread(() -> {
                boolean success = LogManager.getInstance().exportLogsZipToUri(uri);
                runOnUiThread(() -> showToast(getString(success ? R.string.log_export_success : R.string.log_export_failed)));
            }).start();
        }
    );
    private final ActivityResultLauncher<Intent> mShareLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> cleanShareCache()
    );

    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_log_viewer;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        initViews();
        setupTab();
        setupViewPager();
        setupSearch();
    }

    @Override
    protected int getMenuResId() {
        return R.menu.menu_log_actions;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean created = super.onCreateOptionsMenu(menu);
        return created;
    }

    private void initViews() {
        mNestedHeaderLayout = findViewById(R.id.nested_header_layout);
        mViewPager = findViewById(R.id.view_pager);

        mSearchEditText = findViewById(android.R.id.input);

        mNestedHeaderLayout.setEnableBlur(false);
        registerCoordinateScrollView(mNestedHeaderLayout);

        mSearchEditText.setHint(R.string.log_search_hint);

        mLogType = findViewById(R.id.log_type);

        mLogTypeApp = findViewById(R.id.log_type_app);
        mLogTypeXposed = findViewById(R.id.log_type_xposed);

        mPagerAdapter = new LogPagerAdapter(this);
        mViewPager.setAdapter(mPagerAdapter);
        onTabChanged(mCurrentType);
    }

    private void setupTab() {

        mLogType.setFilteredTab(mCurrentType);

        mLogTypeApp.setOnClickListener(v -> handleTabClick(0));

        mLogTypeXposed.setOnClickListener(v -> handleTabClick(1));
    }

    private void setupViewPager() {
        mPagerAdapter = new LogPagerAdapter(this);
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                onTabChanged(position);
            }
        });
    }

    private void setupSearch() {
        mSearchEditText.addTextChangedListener((SimpleTextWatcher) s -> {
            mKeyword = s.toString();
            // 搜索框一动，立即同步给当前可见的 Fragment
            dispatchFilter();
        });
    }

    private void handleTabClick(int position) {
        if (mViewPager.getCurrentItem() == position) {
            boolean hadActiveFilters = hasActiveFilters();
            resetFilters();
            onTabChanged(position);
            if (!hadActiveFilters) {
                refreshCurrentFragment();
            }
            return;
        }
        mViewPager.setCurrentItem(position, mViewPager.isDraggable());
    }

    private boolean hasActiveFilters() {
        return !mKeyword.isEmpty() || mSelectedLevelPos != 0 || mSelectedTagPos != 0;
    }

    /**
     * 核心方法：Tab 切换后的逻辑中心
     */
    private void onTabChanged(int position) {
        mCurrentType = position;
        String module = (position == 0) ? "App" : "Xposed";
        mSelectedTagPos = 0;
        mLogType.setFilteredTab(position);
        updateTagsForModule(module);
        dispatchFilter();
    }

    private void updateTagsForModule(String module) {
        new Thread(() -> {
            // 1. 从数据库查询该模块所有去重后的 Tag
            List<String> tags = LogRepository.getInstance().getDao().getDistinctTags(module);

            List<String> temp = new ArrayList<>();
            temp.add(ALL_TAG_VALUE); // 默认 Pos 0
            if (tags != null) temp.addAll(tags);

            runOnUiThread(() -> {
                this.mCurrentAvailableTags = temp;
                // 2. 刷新菜单数据：如果当前选中的 Pos 超过了新列表长度，强制归零
                if (mSelectedTagPos >= mCurrentAvailableTags.size()) {
                    mSelectedTagPos = 0;
                }
                // 3. 立即下发过滤指令，确保 Fragment 响应
                dispatchFilter();
            });
        }).start();
    }

    /**
     * 核心分发器：将 Activity 维护的过滤状态（搜索词、等级、标签）同步给当前 Fragment
     */
    private void dispatchFilter() {
        // 1. 获取当前 ViewPager 正在显示的 Fragment 实例
        // 这里使用你适配器里提供的辅助方法
        LogListFragment currentFragment = mPagerAdapter.getCurrentFragment(mViewPager);

        if (currentFragment != null) {
            // 2. 提取当前选中的等级 String 值（从 Pos 转换）
            String levelValue = LogLevelFilter.fromPos(mSelectedLevelPos).getValue();

            // 3. 提取当前选中的标签 String 值
            // 注意：mCurrentAvailableTags 是 Activity 异步维护的
            String tagValue = ALL_TAG_VALUE;
            if (!mCurrentAvailableTags.isEmpty() && mSelectedTagPos < mCurrentAvailableTags.size()) {
                tagValue = mCurrentAvailableTags.get(mSelectedTagPos);
            }
            if (tagValue == null) {
                tagValue = ALL_TAG_VALUE;
            }

            // 4. 下发给 Fragment 执行真正的数据库查询
            // 这里会触发 Fragment 内部的“加载态 -> 成功/空态”切换
            currentFragment.applyFilter(mKeyword, levelValue, tagValue);
        }
    }

    private void refreshCurrentFragment() {
        LogListFragment currentFragment = mPagerAdapter.getCurrentFragment(mViewPager);
        if (currentFragment != null) {
            currentFragment.forceRefresh();
        }
    }

    private void resetFilters() {
        mKeyword = "";
        mSelectedLevelPos = 0;
        mSelectedTagPos = 0;
        mCurrentAvailableTags = new ArrayList<>(List.of(ALL_TAG_VALUE));
        mPopupCheckedState.clear();
        if (mSearchEditText != null) {
            mSearchEditText.setText("");
        }
    }

    // ===== 清空日志 =====
    private void clearCurrentLogs() {
        new fan.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.log_clear_title)
            .setMessage(R.string.log_clear_all_message)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                new Thread(() -> {
                    try {
                        LogManager.getInstance().clearAllLogs();
                        runOnUiThread(() -> {
                            resetFilters();
                            updateTagsForModule(mCurrentType == 0 ? "App" : "Xposed");
                            refreshCurrentFragment();
                            showToast(getString(R.string.log_clear_success));
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> showToast(getString(R.string.log_clear_failed)));
                    }
                }).start();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    // ===== 导出日志压缩包 =====
    private void exportLogs() {
        showToast(getString(R.string.log_export_preparing));
        new Thread(() -> {

            try {
                XposedLogLoader.loadLogsSync();
            } catch (Exception e) {
                AndroidLog.w(TAG, "Export logs: failed to sync Xposed logs before export", e);
            }

            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                intent.putExtra(Intent.EXTRA_TITLE, LogManager.generateZipFileName());
                mExportLauncher.launch(intent);
            });
        }).start();
    }

    // ===== 分享日志压缩包 =====
    private void shareLogs() {
        showToast(getString(R.string.log_share_preparing));
        new Thread(() -> {
            try {
                try {
                    XposedLogLoader.loadLogsSync();
                } catch (Exception e) {
                    AndroidLog.w(TAG, "Share logs: failed to sync Xposed logs before share", e);
                }

                File zipFile = LogManager.getInstance().createLogZipFile();
                if (zipFile == null || !zipFile.exists()) {
                    AndroidLog.w(TAG, "Share logs: zip file was not created");
                    runOnUiThread(() -> showToast(getString(R.string.log_share_failed)));
                    return;
                }

                runOnUiThread(() -> {
                    try {
                        Uri contentUri = FileProvider.getUriForFile(
                            LogViewerActivity.this,
                            getPackageName() + ".fileprovider",
                            zipFile
                        );

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("application/zip");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "HyperCeiler Logs");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.log_share_text));
                        shareIntent.setClipData(ClipData.newRawUri("hyperceiler_logs", contentUri));
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        Intent chooserIntent = Intent.createChooser(shareIntent, getString(R.string.log_share_title));
                        chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        grantShareUriPermission(contentUri, shareIntent);

                        mShareLauncher.launch(chooserIntent);
                    } catch (Exception e) {
                        AndroidLog.e(TAG, "Share logs: failed to launch chooser", e);
                        showToast(getString(R.string.log_share_failed));
                        cleanShareCache();
                    }
                });
            } catch (Exception e) {
                AndroidLog.e(TAG, "Share logs: failed to prepare zip", e);
                runOnUiThread(() -> showToast(getString(R.string.log_share_failed)));
            }
        }).start();
    }

    private void cleanShareCache() {
        new Thread(() -> {
            try {
                File cacheDir = new File(getCacheDir(), "log_export");
                deleteRecursively(cacheDir);
            } catch (Exception e) {
                AndroidLog.w(TAG, "Share logs: failed to clean cache", e);
            }
        }).start();
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            AndroidLog.w(TAG, "Share logs: failed to delete cache file " + file.getAbsolutePath());
        }
    }

    private void grantShareUriPermission(Uri contentUri, Intent shareIntent) {
        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(shareIntent, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName = resolveInfo.activityInfo != null ? resolveInfo.activityInfo.packageName : null;
            if (packageName == null || packageName.isEmpty()) {
                continue;
            }
            grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    private void tintMenuItemIcon(MenuItem item, ColorStateList tint) {
        if (item == null || item.getIcon() == null) {
            return;
        }
        item.setIcon(DrawableCompat.wrap(item.getIcon().mutate()));
        DrawableCompat.setTintList(item.getIcon(), tint);
    }

    @Override
    protected boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_export) {
            exportLogs();
            return true;
        }
        if (itemId == R.id.menu_share) {
            shareLogs();
            return true;
        }
        if (itemId == R.id.menu_filter) {
            showFilterMenu(resolveFilterMenuAnchor());
            return true;
        }
        return super.onMenuItemClick(item);
    }

    public void showFilterMenu(View view) {
        HyperPopupMenu popupMenu = new HyperPopupMenu(this, view);
        popupMenu.inflate(R.menu.log_sort_menu);

        injectDynamicSubMenu(popupMenu, R.id.log_level, Arrays.asList(LogLevelFilter.getTitles(this)));
        injectDynamicSubMenu(popupMenu, R.id.log_tag, mCurrentAvailableTags);
        popupMenu.preCheckSecondaryItem(mPopupCheckedState);
        popupMenu.notifyDataChanged();

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            int groupId = item.getGroupId();

            if (itemId == R.id.log_clear) {
                clearCurrentLogs();
            } else if (groupId == R.id.log_level) {
                mSelectedLevelPos = itemId;
                updateSecondaryMapState(groupId, itemId);
                dispatchFilter();
            } else if (groupId == R.id.log_tag) {
                mSelectedTagPos = itemId;
                updateSecondaryMapState(groupId, itemId);
                dispatchFilter();
            }
            popupMenu.saveSecondaryCheckedMap(mPopupCheckedState);
        });
        popupMenu.show();
    }

    private void injectDynamicSubMenu(HyperPopupMenu popupMenu, int parentId, List<String> items) {
        Menu menu = popupMenu.getMenu();
        MenuItem parentItem = menu.findItem(parentId);
        if (parentItem == null) return;

        SubMenu subMenu = parentItem.getSubMenu();
        if (subMenu == null) return;

        subMenu.clear();
        Boolean[] status = mPopupCheckedState.get(parentId);
        if (status == null || status.length != items.size()) {
            status = new Boolean[items.size()];
            for (int i = 0; i < items.size(); i++) {
                boolean checked = parentId == R.id.log_level ? i == mSelectedLevelPos : i == mSelectedTagPos;
                status[i] = checked;
            }
            mPopupCheckedState.put(parentId, status);
        }

        for (int i = 0; i < items.size(); i++) {
            String title = items.get(i);
            if (parentId == R.id.log_tag && i == 0 && ALL_TAG_VALUE.equals(title)) {
                title = getString(R.string.log_filter_all);
            }
            MenuItem subItem = subMenu.add(parentId, i, Menu.NONE, title);
            subItem.setCheckable(true);
            subItem.setChecked(Boolean.TRUE.equals(status[i]));
        }
    }

    private void updateSecondaryMapState(int groupId, int index) {
        Boolean[] status = mPopupCheckedState.get(groupId);
        if (status == null || index >= status.length) return;

        Arrays.fill(status, Boolean.FALSE);
        status[index] = Boolean.TRUE;
    }

    private View resolveFilterMenuAnchor() {
        View decorView = getWindow().getDecorView();
        View actionView = findViewByContentDescription(decorView, getString(R.string.log_menu_filter));
        if (actionView != null) {
            return actionView;
        }

        View topRightView = findTopRightClickableView(decorView);
        return topRightView != null ? topRightView : mNestedHeaderLayout;
    }

    private View findViewByContentDescription(View root, CharSequence contentDescription) {
        if (root == null || TextUtils.isEmpty(contentDescription)) {
            return null;
        }
        if (TextUtils.equals(contentDescription, root.getContentDescription())) {
            return root;
        }
        if (!(root instanceof ViewGroup viewGroup)) {
            return null;
        }
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = findViewByContentDescription(viewGroup.getChildAt(i), contentDescription);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    private View findTopRightClickableView(View root) {
        if (root == null || !root.isShown() || root.getWidth() == 0 || root.getHeight() == 0) {
            return null;
        }
        View best = null;
        int[] location = new int[2];
        int minLeft = getResources().getDisplayMetrics().widthPixels / 2;

        if (root.isClickable()) {
            root.getLocationOnScreen(location);
            if (location[1] <= dpToPx(96) && location[0] >= minLeft) {
                best = root;
            }
        }

        if (!(root instanceof ViewGroup viewGroup)) {
            return best;
        }
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View candidate = findTopRightClickableView(viewGroup.getChildAt(i));
            if (candidate == null) {
                continue;
            }
            if (best == null || isFurtherTopRight(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean isFurtherTopRight(View candidate, View current) {
        int[] candidateLocation = new int[2];
        int[] currentLocation = new int[2];
        candidate.getLocationOnScreen(candidateLocation);
        current.getLocationOnScreen(currentLocation);
        if (candidateLocation[1] != currentLocation[1]) {
            return candidateLocation[1] < currentLocation[1];
        }
        return candidateLocation[0] > currentLocation[0];
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
