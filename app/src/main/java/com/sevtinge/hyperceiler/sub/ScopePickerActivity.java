package com.sevtinge.hyperceiler.sub;

import android.content.Context;
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
import com.sevtinge.hyperceiler.common.utils.PermissionUtils;
import com.sevtinge.hyperceiler.model.adapter.AppDataAdapter;
import com.sevtinge.hyperceiler.model.data.AppData;
import com.sevtinge.hyperceiler.model.data.AppDataManager;
import com.sevtinge.hyperceiler.utils.LanguageHelper;
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
    private static final int REQUEST_GET_INSTALLED_APPS = 1201;
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
    private final Set<String> mAvailableScopePackages = new LinkedHashSet<>();
    private final Set<String> mSelectableScopePackages = new LinkedHashSet<>();
    private final Set<String> mExcludedPackages = new LinkedHashSet<>();
    private boolean mInitializationMode = false;

    private boolean mIsApplyingScope = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.wrapContext(newBase));
    }

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
        if (!ensureInstalledAppsPermission()) {
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);

        ThreadUtils.postOnBackgroundThread(() -> {
            try {
                Set<String> scopePackages = ScopeManager.peekNormalizedScopeSync();
                Set<String> currentScopePackages = scopePackages != null
                    ? new LinkedHashSet<>(scopePackages)
                    : new LinkedHashSet<>();

                List<AppData> loadedData = mAppDataManager.getAppInfo(MODE_SCOPE);
                loadedData = prepareScopeModeData(new ArrayList<>(loadedData));
                Set<String> availableScopePackages = collectScopePackages(loadedData);
                Set<String> sanitizedScopePackages = ScopeManager.filterScopePackages(
                    currentScopePackages,
                    availableScopePackages
                );

                mCurrentScopePackages.clear();
                mCurrentScopePackages.addAll(sanitizedScopePackages);
                mAvailableScopePackages.clear();
                mAvailableScopePackages.addAll(availableScopePackages);

                cleanupUnavailableScopePackages(currentScopePackages, sanitizedScopePackages);

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

        mSelectableScopePackages.clear();
        mSelectableScopePackages.addAll(collectScopePackages(processedData));

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

    private boolean ensureInstalledAppsPermission() {
        if (PermissionUtils.canReadInstalledApps(this)) {
            return true;
        }
        requestPermissions(new String[]{PermissionUtils.PERMISSION_GET_INSTALLED_APPS}, REQUEST_GET_INSTALLED_APPS);
        return false;
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

        mInitialSelectedPackages.clear();
        promoteSelectedAppsToTop(data, mCurrentScopePackages, mInitialSelectedPackages);

        return data;
    }

    private List<AppData> prepareScopeModeData(List<AppData> data) {
        if (!mExcludedPackages.contains(SYSTEM_SCOPE_PACKAGE)) {
            data.add(0, createSystemFrameworkApp());
        }
        return data;
    }

    private Set<String> collectScopePackages(List<AppData> data) {
        Set<String> packages = new LinkedHashSet<>();
        if (data == null || data.isEmpty()) {
            return packages;
        }

        for (AppData appData : data) {
            String normalizedPackage = ScopeManager.normalizeScopePackageName(appData.packageName);
            if (normalizedPackage != null) {
                packages.add(normalizedPackage);
            }
        }
        return packages;
    }

    private void cleanupUnavailableScopePackages(Set<String> currentScopePackages, Set<String> sanitizedScopePackages) {
        if (currentScopePackages == null || currentScopePackages.equals(sanitizedScopePackages)) {
            return;
        }

        ScopeManager.applyScopeDiffAsync(this, currentScopePackages, sanitizedScopePackages, (success, message) -> {
            if (success) {
                AndroidLog.i(TAG, "cleanupUnavailableScopePackages: removed uninstalled scope entries");
                return;
            }
            AndroidLog.w(TAG, "cleanupUnavailableScopePackages: " + message);
        });
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
        mCurrentScopePackages.addAll(ScopeManager.filterScopePackages(scopePackages, mAvailableScopePackages));

        mInitialSelectedPackages.clear();
        for (AppData appData : mOriginalAppDataList) {
            String normalizedPackage = ScopeManager.normalizeScopePackageName(appData.packageName);
            boolean isSelected = normalizedPackage != null && mCurrentScopePackages.contains(normalizedPackage);
            appData.isSelected = isSelected;
            if (isSelected) {
                mInitialSelectedPackages.add(appData.packageName);
            }
        }

        promoteSelectedAppsToTop(mOriginalAppDataList, mCurrentScopePackages, null);
        mAppListAdapter.setSelectedPackages(mInitialSelectedPackages);
        filterAppList(mSearchInputView != null && mSearchInputView.getText() != null
            ? mSearchInputView.getText().toString()
            : null);
    }

    private void applyScopeSelection() {
        if (mIsApplyingScope) {
            return;
        }

        Set<String> currentSelected = ScopeManager.peekNormalizedScopeSync();
        if (currentSelected == null) {
            currentSelected = new LinkedHashSet<>(mCurrentScopePackages);
        }

        Set<String> targetSelected = ScopeManager.filterScopePackages(currentSelected, mAvailableScopePackages);
        targetSelected.removeAll(mSelectableScopePackages);
        targetSelected.addAll(ScopeManager.normalizeScopePackages(mAppListAdapter.getSelectedPackages()));

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
            promoteSelectedAppsToTop(
                mCurrentAppDataList,
                getEffectiveSelectedScopePackages(),
                null
            );
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
        promoteSelectedAppsToTop(
            mCurrentAppDataList,
            getEffectiveSelectedScopePackages(),
            null
        );
        mAppListAdapter.setData(mCurrentAppDataList);
    }

    private void promoteSelectedAppsToTop(List<AppData> data, Set<String> normalizedSelectedPackages, Set<String> selectedPackagesOut) {
        if (data == null || data.isEmpty() || normalizedSelectedPackages == null || normalizedSelectedPackages.isEmpty()) {
            return;
        }

        List<AppData> selectedAppList = new ArrayList<>();
        Iterator<AppData> iterator = data.iterator();
        while (iterator.hasNext()) {
            AppData appData = iterator.next();
            String normalizedPackage = ScopeManager.normalizeScopePackageName(appData.packageName);
            if (normalizedPackage != null && normalizedSelectedPackages.contains(normalizedPackage)) {
                appData.isSelected = true;
                if (selectedPackagesOut != null) {
                    selectedPackagesOut.add(appData.packageName);
                }
                selectedAppList.add(appData);
                iterator.remove();
            }
        }
        data.addAll(0, selectedAppList);
    }

    private Set<String> getEffectiveSelectedScopePackages() {
        Set<String> selected = ScopeManager.normalizeScopePackages(mAppListAdapter.getSelectedPackages());
        if (selected != null && !selected.isEmpty()) {
            return selected;
        }
        return mCurrentScopePackages;
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_GET_INSTALLED_APPS) {
            return;
        }
        if (PermissionUtils.canReadInstalledApps(this)
            || PermissionUtils.isInstalledAppsPermissionGranted(permissions, grantResults)) {
            initializeData();
            return;
        }
        showLoadAppsError();
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
