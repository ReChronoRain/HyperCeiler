package com.sevtinge.hyperceiler.sub;

import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sevtinge.hyperceiler.callback.SearchCallback;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.model.adapter.AppDataAdapter;
import com.sevtinge.hyperceiler.model.data.AppData;
import com.sevtinge.hyperceiler.model.data.AppDataManager;
import com.sevtinge.hyperceiler.utils.ScopeManager;
import com.sevtinge.hyperceiler.utils.ThreadUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import fan.appcompat.app.AppCompatActivity;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.recyclerview.card.CardDefaultItemAnimator;
import fan.recyclerview.card.CardItemDecoration;
import fan.recyclerview.widget.RecyclerView;

public class ScopePickerActivity extends AppCompatActivity
    implements SearchView.OnQueryTextListener, SearchCallback.OnSearchListener {

    private static final String TAG = "ScopePickerActivity";
    public static final String EXTRA_EXCLUDED_PACKAGES = "excluded_packages";
    public static final String EXTRA_INITIALIZATION_MODE = "initialization_mode";
    private static final int MODE_SCOPE = 6;
    private static final String SYSTEM_SCOPE_PACKAGE = "system";

    private View mSearchBar;
    private TextView mSearchInputView;
    private ProgressBar mProgressBar;
    private NestedHeaderLayout mNestedHeaderLayout;
    private RecyclerView mAppListRecyclerView;
    private AppDataAdapter mAppListAdapter;
    private SearchCallback mSearchCallback;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final AppDataManager mAppDataManager = new AppDataManager();
    private final List<AppData> mOriginalAppDataList = new ArrayList<>();
    private final List<AppData> mCurrentAppDataList = new ArrayList<>();
    private final Set<String> mCurrentScopePackages = new LinkedHashSet<>();
    private final Set<String> mInitialSelectedPackages = new LinkedHashSet<>();
    private final Set<String> mExcludedPackages = new LinkedHashSet<>();
    private boolean mInitializationMode = false;

    private boolean mIsApplyingScope = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.sevtinge.hyperceiler.core.R.layout.activity_app_picker);
        setExtraHorizontalPaddingEnable(true);

        extractIntentData();
        if (getActionBar() != null) {
            getActionBar().setTitle(com.sevtinge.hyperceiler.core.R.string.settings_scope);
        }

        initializeViews();
        invalidateOptionsMenu();
        initializeData();
    }

    private void extractIntentData() {
        Bundle args = getIntent().getExtras();
        if (args == null) {
            return;
        }

        mInitializationMode = args.getBoolean(EXTRA_INITIALIZATION_MODE, false);
        if (mInitializationMode) {
            return;
        }

        ArrayList<String> excludedPackages = args.getStringArrayList(EXTRA_EXCLUDED_PACKAGES);
        if (excludedPackages == null) {
            return;
        }

        for (String packageName : excludedPackages) {
            String normalized = ScopeManager.normalizeScopePackageName(packageName);
            if (normalized != null) {
                mExcludedPackages.add(normalized);
            }
        }
    }

    private void initializeViews() {
        mProgressBar = findViewById(com.sevtinge.hyperceiler.core.R.id.am_progressBar);
        mSearchBar = findViewById(com.sevtinge.hyperceiler.core.R.id.search_bar);
        mSearchInputView = mSearchBar.findViewById(android.R.id.input);
        mNestedHeaderLayout = findViewById(com.sevtinge.hyperceiler.core.R.id.nested_header_layout);
        mAppListRecyclerView = findViewById(com.sevtinge.hyperceiler.core.R.id.app_list_rv);

        mSearchBar.setClickable(false);
        mAppListRecyclerView.setVisibility(View.GONE);

        mAppListAdapter = new AppDataAdapter(new ArrayList<>(), null, MODE_SCOPE);
        mAppListAdapter.setDeferredSelectionMode(true);
        mAppListRecyclerView.setAdapter(mAppListAdapter);
        mAppListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAppListRecyclerView.setHasFixedSize(true);
        mAppListRecyclerView.addItemDecoration(new CardItemDecoration(this));
        mAppListRecyclerView.setItemAnimator(new CardDefaultItemAnimator());

        mSearchCallback = new SearchCallback(this, this);
        mSearchCallback.setup(mSearchBar, mNestedHeaderLayout.getScrollableView());
        mSearchBar.setOnClickListener(v -> startSearchActionMode());
    }

    private void startSearchActionMode() {
        if (mSearchCallback == null || mSearchCallback.isSearchOn()) {
            return;
        }

        ActionMode actionMode = onWindowStartingActionMode(mSearchCallback);
        if (actionMode != null) {
            return;
        }

        AndroidLog.w(TAG, "startSearchActionMode: activity delegate did not start search mode");
    }

    private void initializeData() {
        mProgressBar.setVisibility(View.VISIBLE);

        ThreadUtils.postOnBackgroundThread(() -> {
            try {
                mCurrentScopePackages.clear();
                Set<String> scopePackages = ScopeManager.peekNormalizedScopeSync();
                if (scopePackages != null) {
                    mCurrentScopePackages.addAll(scopePackages);
                }

                List<AppData> loadedData = mAppDataManager.getAppInfo(MODE_SCOPE);
                loadedData = prepareScopeModeData(new ArrayList<>(loadedData));
                List<AppData> processedData = processAppData(new ArrayList<>(loadedData));
                mHandler.post(() -> displayAppData(processedData));
            } catch (Exception e) {
                AndroidLog.e(TAG, "initializeData: failed to load app data", e);
                mHandler.post(this::showLoadAppsError);
            }
        });
    }

    private void displayAppData(List<AppData> processedData) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        mOriginalAppDataList.clear();
        mOriginalAppDataList.addAll(processedData);

        mCurrentAppDataList.clear();
        mCurrentAppDataList.addAll(processedData);

        mAppListAdapter.setData(mCurrentAppDataList);
        mAppListAdapter.setSelectedPackages(mInitialSelectedPackages);
        mSearchInputView.setHint(String.format(
            getString(com.sevtinge.hyperceiler.core.R.string.search_apps_hint),
            mAppListAdapter.getData().size()
        ));
        mProgressBar.setVisibility(View.GONE);
        mSearchBar.setClickable(!mIsApplyingScope);
        mAppListRecyclerView.setVisibility(View.VISIBLE);
        invalidateOptionsMenu();
    }

    private void showLoadAppsError() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        mProgressBar.setVisibility(View.GONE);
        invalidateOptionsMenu();
        Toast.makeText(this, getString(com.sevtinge.hyperceiler.core.R.string.load_apps_failed), Toast.LENGTH_SHORT).show();
    }

    private List<AppData> processAppData(List<AppData> data) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }

        if (!mExcludedPackages.isEmpty()) {
            data.removeIf(appData -> mExcludedPackages.contains(
                ScopeManager.normalizeScopePackageName(appData.packageName)
            ));
        }

        Collator collator = Collator.getInstance(Locale.getDefault());
        data.sort((app1, app2) -> collator.compare(app1.label, app2.label));

        AppData tagApp = null;
        AppData systemFrameworkApp = null;
        Iterator<AppData> iterator = data.iterator();
        while (iterator.hasNext()) {
            AppData app = iterator.next();
            if ("com.android.apps.tag".equals(app.packageName)) {
                tagApp = app;
                iterator.remove();
                continue;
            }
            if (SYSTEM_SCOPE_PACKAGE.equals(app.packageName)) {
                systemFrameworkApp = app;
                iterator.remove();
            }
        }

        if (tagApp != null) {
            data.add(0, tagApp);
        }
        if (systemFrameworkApp != null) {
            data.add(0, systemFrameworkApp);
        }

        if (!mCurrentScopePackages.isEmpty()) {
            List<AppData> selectedAppList = new ArrayList<>();
            iterator = data.iterator();

            mInitialSelectedPackages.clear();
            while (iterator.hasNext()) {
                AppData appData = iterator.next();
                String normalizedPackage = ScopeManager.normalizeScopePackageName(appData.packageName);
                if (normalizedPackage != null && mCurrentScopePackages.contains(normalizedPackage)) {
                    appData.isSelected = true;
                    mInitialSelectedPackages.add(appData.packageName);
                    selectedAppList.add(appData);
                    iterator.remove();
                }
            }

            data.addAll(0, selectedAppList);
        }

        return data;
    }

    private List<AppData> prepareScopeModeData(List<AppData> data) {
        if (!mExcludedPackages.contains(SYSTEM_SCOPE_PACKAGE)) {
            data.add(0, createSystemFrameworkApp());
        }
        return data;
    }

    private AppData createSystemFrameworkApp() {
        AppData appData = new AppData();
        appData.packageName = SYSTEM_SCOPE_PACKAGE;
        appData.label = getString(com.sevtinge.hyperceiler.core.R.string.system_framework);
        appData.icon = loadAndroidPackageIcon();
        appData.enabled = true;
        appData.isSystemApp = true;
        return appData;
    }

    private Drawable loadAndroidPackageIcon() {
        try {
            PackageManager packageManager = getPackageManager();
            return packageManager.getApplicationInfo("android", 0).loadIcon(packageManager);
        } catch (PackageManager.NameNotFoundException e) {
            AndroidLog.w(TAG, "loadAndroidPackageIcon: failed to load android package icon", e);
            return ContextCompat.getDrawable(this, com.sevtinge.hyperceiler.core.R.drawable.ic_system_framework_new);
        }
    }

    private void syncSelectionFromScope(Set<String> scopePackages) {
        mCurrentScopePackages.clear();
        if (scopePackages != null) {
            mCurrentScopePackages.addAll(scopePackages);
        }

        mInitialSelectedPackages.clear();
        for (AppData appData : mOriginalAppDataList) {
            String normalizedPackage = ScopeManager.normalizeScopePackageName(appData.packageName);
            boolean isSelected = normalizedPackage != null && mCurrentScopePackages.contains(normalizedPackage);
            appData.isSelected = isSelected;
            if (isSelected) {
                mInitialSelectedPackages.add(appData.packageName);
            }
        }

        mAppListAdapter.setSelectedPackages(mInitialSelectedPackages);
    }

    private void applyScopeSelection() {
        if (mIsApplyingScope) {
            return;
        }

        Set<String> currentSelected = new LinkedHashSet<>(mInitialSelectedPackages);
        Set<String> targetSelected = mAppListAdapter.getSelectedPackages();

        setScopeApplying(true);
        ScopeManager.applyScopeDiffAsync(this, currentSelected, targetSelected, (success, message) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            setScopeApplying(false);

            Set<String> finalScope = ScopeManager.peekNormalizedScopeSync();
            if (finalScope == null) {
                finalScope = ScopeManager.normalizeScopePackages(targetSelected);
            }
            syncSelectionFromScope(finalScope);

            if (success) {
                setResult(RESULT_OK);
                finish();
                return;
            }

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void setScopeApplying(boolean isApplying) {
        mIsApplyingScope = isApplying;
        mSearchBar.setClickable(!isApplying && mProgressBar.getVisibility() != View.VISIBLE);
        invalidateOptionsMenu();
    }

    private void filterAppList(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            mCurrentAppDataList.clear();
            mCurrentAppDataList.addAll(mOriginalAppDataList);
            mAppListAdapter.setData(mCurrentAppDataList);
            return;
        }

        List<AppData> filteredList = new ArrayList<>();
        String searchTerm = keyword.toLowerCase().trim();

        for (AppData appData : mOriginalAppDataList) {
            if (appData.label != null && appData.label.toLowerCase().contains(searchTerm)) {
                filteredList.add(appData);
            }
        }

        mCurrentAppDataList.clear();
        mCurrentAppDataList.addAll(filteredList);
        mAppListAdapter.setData(mCurrentAppDataList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(com.sevtinge.hyperceiler.core.R.menu.menu_scope_picker_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem confirmItem = menu.findItem(com.sevtinge.hyperceiler.core.R.id.action_confirm_scope);
        if (confirmItem != null) {
            confirmItem.setEnabled(!mIsApplyingScope && mProgressBar.getVisibility() != View.VISIBLE);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == com.sevtinge.hyperceiler.core.R.id.action_confirm_scope) {
            applyScopeSelection();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        filterAppList(newText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        filterAppList(query);
        return true;
    }

    @Override
    public void onCreateSearchMode(ActionMode actionMode, Menu menu) {
    }

    @Override
    public void onSearchModeAnimStart(boolean enabled) {
        if (enabled) {
            mNestedHeaderLayout.setInSearchMode(true);
        } else {
            mAppListRecyclerView.stopScroll();
        }
    }

    @Override
    public void onSearchModeAnimUpdate(boolean enabled, float f) {
    }

    @Override
    public void onSearchModeAnimStop(boolean enabled) {
        if (!enabled) {
            mNestedHeaderLayout.setInSearchMode(false);
            mAppListRecyclerView.scrollToPosition(0);
        }
    }

    @Override
    public void onDestroySearchMode(ActionMode actionMode) {
        mCurrentAppDataList.clear();
        mCurrentAppDataList.addAll(mOriginalAppDataList);
        mAppListAdapter.setData(mCurrentAppDataList);
        mSearchInputView.setHint(String.format(
            getString(com.sevtinge.hyperceiler.core.R.string.search_apps_hint),
            mAppListAdapter.getData().size()
        ));
        mAppListRecyclerView.scrollToPosition(0);
    }

    @Override
    public void onContentInsetChanged(Rect rect) {
        super.onContentInsetChanged(rect);
        if (mAppListRecyclerView != null) {
            mAppListRecyclerView.setPadding(
                mAppListRecyclerView.getPaddingLeft(),
                mAppListRecyclerView.getPaddingTop(),
                mAppListRecyclerView.getPaddingRight(),
                rect.bottom
            );
        }
    }
}
