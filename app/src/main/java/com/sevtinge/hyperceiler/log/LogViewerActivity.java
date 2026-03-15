package com.sevtinge.hyperceiler.log;

import static com.sevtinge.hyperceiler.logviewer.XposedLogLoader.loadLogsSync;

import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.base.BaseActivity;
import com.sevtinge.hyperceiler.common.log.LogLevelFilter;
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.common.widget.SearchEditText;
import com.sevtinge.hyperceiler.log.db.LogRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fan.miuixbase.widget.FilterSortTabView;
import fan.miuixbase.widget.FilterSortView2;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.viewpager.widget.ViewPager;

public class LogViewerActivity extends BaseActivity {

    private static final String TAG = "LogViewerActivity";
    private static final int EXPORT_CODE = 1001;
    private static final int SHARE_CODE = 1002;
    private static final String FILE_PROVIDER_AUTHORITY = ProjectApi.mAppModulePkg + ".fileprovider";

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
    private List<String> mCurrentAvailableTags = new ArrayList<>(); // 当前 Tab 可用的 Tag

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
    protected void onResume() {
        super.onResume();
        dispatchFilter();
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

        mLogTypeApp.setOnClickListener(v -> {
            onTabChanged(0);
            mViewPager.setCurrentItem(0, mViewPager.isDraggable());
        });

        mLogTypeXposed.setOnClickListener(v -> {
            onTabChanged(1);
            mViewPager.setCurrentItem(1, mViewPager.isDraggable());
        });
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
        mSearchEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                mKeyword = s.toString();
                // 搜索框一动，立即同步给当前可见的 Fragment
                dispatchFilter();
            }
        });
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
            temp.add("全部标签"); // 默认 Pos 0
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
            String tagValue = "ALL";
            if (!mCurrentAvailableTags.isEmpty() && mSelectedTagPos < mCurrentAvailableTags.size()) {
                tagValue = mCurrentAvailableTags.get(mSelectedTagPos);
            }

            // 4. 下发给 Fragment 执行真正的数据库查询
            // 这里会触发 Fragment 内部的“加载态 -> 成功/空态”切换
            currentFragment.applyFilter(mKeyword, levelValue, tagValue);
        }
    }

    @Override
    protected int getMenuResId() {
        return R.menu.menu_log_actions;
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

    // ===== 清空日志 =====
    private void clearCurrentLogs() {
        new fan.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.log_clear_title)
            .setMessage(R.string.log_clear_all_message)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                new Thread(() -> {
                    try {
                        LogManager.getInstance().clearAllLogs();
                        showToast(getString(R.string.log_clear_success));
                    } catch (Exception e) {
                        showToast(getString(R.string.log_clear_failed));
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
                loadLogsSync();
            } catch (Exception e) {
                Log.w(TAG, "Failed to sync Xposed logs before export", e);
            }

            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                intent.putExtra(Intent.EXTRA_TITLE, com.sevtinge.hyperceiler.logviewer.LogManager.generateZipFileName());
                startActivityForResult(intent, EXPORT_CODE);
            });
        }).start();
    }

    // ===== 分享日志压缩包 =====
    private void shareLogs() {

    }
}
