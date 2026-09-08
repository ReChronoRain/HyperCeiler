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
package com.sevtinge.hyperceiler.sub;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sevtinge.hyperceiler.callback.IAppSelectCallback;
import com.sevtinge.hyperceiler.callback.SearchCallback;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PermissionUtils;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.libhook.utils.api.BitmapUtils;
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

import fan.appcompat.app.AlertDialog;
import fan.appcompat.app.AppCompatActivity;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.recyclerview.card.CardDefaultItemAnimator;
import fan.recyclerview.card.CardItemDecoration;
import fan.recyclerview.widget.RecyclerView;

public class SubPickerActivity extends AppCompatActivity
    implements IAppSelectCallback, SearchView.OnQueryTextListener,
    SearchCallback.OnSearchListener {

    private static final String TAG = "AppPicker";
    public static final int APP_OPEN_MODE = 0;
    public static final int LAUNCHER_MODE = 1;
    public static final int CALLBACK_MODE = 2;
    public static final int INPUT_MODE = 3;
    public static final int PROCESS_TEXT_MODE = 4;
    public static final int ALL_APPS_MODE = 5;
    public static final int SCOPE_MODE = 6;
    public static final int LAUNCHER_PICK_MODE = 7;
    public static final int IME_MODE = 8;
    private static final int REQUEST_GET_INSTALLED_APPS = 1202;

    private String mKey;
    private int mModeSelection;

    private View mSearchBar;
    private TextView mSearchInputView;
    private ProgressBar mProgressBar;
    private NestedHeaderLayout mNestedHeaderLayout;
    private RecyclerView mAppListRecyclerView;
    private AppDataAdapter mAppListAdapter;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SearchCallback mSearchCallback;

    private final AppDataManager mAppDataManager = new AppDataManager();
    private final List<AppData> mOriginalAppDataList = new ArrayList<>(); // 原始数据备份
    private final List<AppData> mCurrentAppDataList = new ArrayList<>();  // 当前显示数据
    private final Set<String> mAvailableImePackages = new LinkedHashSet<>();
    private boolean mImeSelectionApplying = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.wrapContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_picker);
        setExtraHorizontalPaddingEnable(true);

        extractIntentData();
        if (isFinishing()) {
            return;
        }
        initializeViews();
        invalidateOptionsMenu();
        initializeData();
    }

    private void extractIntentData() {
        Bundle args = getIntent().getExtras();
        if (args == null) {
            finish();
            return;
        }

        mModeSelection = args.getInt("mode", -1);
        if (isKeyRequiredMode(mModeSelection)) {
            mKey = args.getString("key");
            if (mKey == null) {
                AndroidLog.e(TAG, "extractIntentData: key is null for mode " + mModeSelection);
                finish();
            }
        }
    }

    private boolean isKeyRequiredMode(int mode) {
        return mode == APP_OPEN_MODE || mode == LAUNCHER_MODE ||
            mode == INPUT_MODE || mode == PROCESS_TEXT_MODE ||
            mode == ALL_APPS_MODE || mode == LAUNCHER_PICK_MODE ||
            mode == IME_MODE;
    }

    private void initializeViews() {
        initializeProgressBar();
        initializeSearchBar();
        initializeRecyclerView();
        setupSearchCallback();
    }

    private void initializeProgressBar() {
        mProgressBar = findViewById(R.id.am_progressBar);
    }

    private void initializeSearchBar() {
        mSearchBar = findViewById(R.id.search_bar);
        mSearchInputView = mSearchBar.findViewById(android.R.id.input);
        mSearchBar.setClickable(false);
    }

    private void initializeRecyclerView() {
        mNestedHeaderLayout = findViewById(R.id.nested_header_layout);
        mAppListRecyclerView = findViewById(R.id.app_list_rv);
        mAppListRecyclerView.setVisibility(View.GONE);

        mAppListAdapter = new AppDataAdapter(new ArrayList<>(), mKey, mModeSelection);
        configureDeferredSelectionMode();
        mAppListRecyclerView.setAdapter(mAppListAdapter);
        mAppListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAppListRecyclerView.setHasFixedSize(true);
        mAppListRecyclerView.addItemDecoration(new CardItemDecoration(this));
        mAppListRecyclerView.setItemAnimator(new CardDefaultItemAnimator());

        setupItemClickListener();
    }

    private void setupSearchCallback() {
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

    private void setupItemClickListener() {
        mAppListAdapter.setOnItemClickListener((itemView, appData, position) -> handleAppItemClick(appData));
    }

    private void handleAppItemClick(AppData appData) {
        switch (mModeSelection) {
            case CALLBACK_MODE, LAUNCHER_PICK_MODE -> sendCallbackResult(appData);
            case INPUT_MODE -> showEditDialog(appData);
            // LAUNCHER_MODE, APP_OPEN_MODE, PROCESS_TEXT_MODE 已经在Adapter中处理
        }
    }

    private void sendCallbackResult(AppData appData) {
        sendMsgToActivity(
            appData.icon,
            appData.label,
            appData.packageName,
            appData.versionName + "(" + appData.versionCode + ")",
            appData.activityName
        );
        finish();
    }

    private void showEditDialog(AppData data) {
        try {
            View view = LayoutInflater.from(this).inflate(R.layout.edit_dialog, null);
            EditText input = view.findViewById(R.id.title);
            input.setText(data.label);

            new AlertDialog.Builder(this)
                .setTitle(R.string.edit)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String userInput = input.getText().toString().trim();
                    if (!userInput.isEmpty()) {
                        mAppListAdapter.editCallback(data.label, data.packageName, userInput);
                        data.label = userInput;
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setOnCancelListener(DialogInterface::dismiss)
                .show();
        } catch (Exception e) {
            AndroidLog.e(TAG, "showEditDialog failed", e);
        }
    }

    private void initializeData() {
        if (!ensureInstalledAppsPermission()) {
            return;
        }
        setLoadingState(true);

        ThreadUtils.postOnBackgroundThread(() -> {
            try {
                List<AppData> loadedData = mAppDataManager.getAppInfo(mModeSelection);
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

        if (supportsConfirmAction()) {
            mAvailableImePackages.clear();
            mAvailableImePackages.addAll(collectNormalizedPackages(processedData));

            LinkedHashSet<String> persistedSelectedPackages = getPersistedSelectedPackages();
            LinkedHashSet<String> sanitizedSelectedPackages = ScopeManager.filterScopePackages(
                persistedSelectedPackages,
                mAvailableImePackages
            );
            if (!persistedSelectedPackages.equals(sanitizedSelectedPackages)) {
                saveImeSelection(sanitizedSelectedPackages);
            }
            mAppListAdapter.setSelectedPackages(sanitizedSelectedPackages);
        }

        updateCurrentAppData(processedData);
        setLoadingState(false);
        mAppListRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showLoadAppsError() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        setLoadingState(false);
        Toast.makeText(this, getString(R.string.load_apps_failed), Toast.LENGTH_SHORT).show();
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

        // 1. 排序
        Collator collator = Collator.getInstance(Locale.getDefault());
        data.sort((app1, app2) -> collator.compare(app1.label, app2.label));

        // 2. 移动特定应用到顶部
        AppData tagApp = null;
        Iterator<AppData> iterator = data.iterator();
        while (iterator.hasNext()) {
            AppData app = iterator.next();
            if ("com.android.apps.tag".equals(app.packageName)) {
                tagApp = app;
                iterator.remove();
                break;
            }
        }
        if (tagApp != null) {
            data.add(0, tagApp);
        }

        // 3. 移动多选模式已选应用到顶部
        if (mKey != null) {
            Set<String> selectedApps = getPersistedSelectedPackages();

            List<AppData> selectedAppList = new ArrayList<>();
            iterator = data.iterator();

            while (iterator.hasNext()) {
                AppData appData = iterator.next();
                if (selectedApps.contains(appData.packageName)) {
                    appData.isSelected = true;
                    selectedAppList.add(appData);
                    iterator.remove();
                }
            }

            data.addAll(0, selectedAppList);
        }

        // 4. 手势应用选择保留 launcher 列表样式，但当前已选项需要置顶
        if ((mModeSelection == LAUNCHER_PICK_MODE || mModeSelection == CALLBACK_MODE) && mKey != null) {
            String selectedApp = PrefsBridge.getString(mKey + "_app", "");
            if (!selectedApp.isEmpty()) {
                String[] selectedParts = selectedApp.split("\\|", 2);
                String selectedPackage = selectedParts.length > 0 ? selectedParts[0] : "";
                String selectedActivity = selectedParts.length > 1 ? selectedParts[1] : "";

                AppData selectedAppData = null;
                iterator = data.iterator();
                while (iterator.hasNext()) {
                    AppData app = iterator.next();
                    if (selectedPackage.equals(app.packageName) &&
                        selectedActivity.equals(app.activityName)) {
                        app.isSelected = true;
                        selectedAppData = app;
                        iterator.remove();
                        break;
                    }
                }

                if (selectedAppData != null) {
                    data.add(0, selectedAppData);
                }
            }
        }

        return data;
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

    private void filterAppList(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // 搜索为空，恢复原始数据
            updateCurrentAppData(mOriginalAppDataList);
            return;
        }

        // 从原始数据中过滤
        List<AppData> filteredList = new ArrayList<>();
        String searchTerm = keyword.toLowerCase().trim();

        for (AppData appData : mOriginalAppDataList) {
            if (appData.label != null && appData.label.toLowerCase().contains(searchTerm)) {
                filteredList.add(appData);
            }
        }

        // 更新当前显示数据
        updateCurrentAppData(filteredList);

        AndroidLog.d(TAG, "filterAppList: filtered " + filteredList.size() + " items for: " + searchTerm);
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
        // 取消搜索时恢复原始数据
        updateCurrentAppData(mOriginalAppDataList);
        mAppListRecyclerView.scrollToPosition(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (supportsConfirmAction()) {
            getMenuInflater().inflate(R.menu.menu_scope_picker_actions, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (supportsConfirmAction()) {
            MenuItem confirmItem = menu.findItem(R.id.action_confirm_scope);
            if (confirmItem != null) {
                confirmItem.setEnabled(!isImePickerBusy());
            }
            return true;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (supportsConfirmAction() && item.getItemId() == R.id.action_confirm_scope) {
            confirmImeSelection();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
    public void sendMsgToActivity(Drawable appIcon, String appName, String appPackageName, String appVersion, String appActivityName) {
        Drawable safeIcon = appIcon;
        if (safeIcon == null && appPackageName != null && !appPackageName.isEmpty()) {
            try {
                safeIcon = getPackageManager().getApplicationIcon(appPackageName);
            } catch (Exception e) {
                AndroidLog.w(TAG, "sendMsgToActivity: failed to load app icon for " + appPackageName, e);
            }
        }
        if (safeIcon == null) {
            safeIcon = getDrawable(android.R.drawable.sym_def_app_icon);
        }

        Intent intent = new Intent();
        if (safeIcon != null) {
            Bitmap bitmap = BitmapUtils.drawableToBitmap(safeIcon);
            intent.putExtra("appIcon", BitmapUtils.Bitmap2Bytes(bitmap));
        }
        intent.putExtra("appName", appName);
        intent.putExtra("appPackageName", appPackageName);
        intent.putExtra("appVersion", appVersion);
        intent.putExtra("appActivityName", appActivityName);
        setResult(1, intent);
    }

    @Override
    public String getMsgFromActivity(String s) {
        return null;
    }

    private void configureDeferredSelectionMode() {
        if (!supportsConfirmAction()) {
            return;
        }
        mAppListAdapter.setDeferredSelectionMode(true);
        mAppListAdapter.setSelectedPackages(getPersistedSelectedPackages());
    }

    private LinkedHashSet<String> getPersistedSelectedPackages() {
        if (mKey == null) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(PrefsBridge.getStringSet(mKey));
    }

    private LinkedHashSet<String> collectNormalizedPackages(List<AppData> appDataList) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        if (appDataList == null || appDataList.isEmpty()) {
            return packages;
        }

        for (AppData appData : appDataList) {
            String normalizedPackage = ScopeManager.normalizeScopePackageName(appData.packageName);
            if (normalizedPackage != null) {
                packages.add(normalizedPackage);
            }
        }
        return packages;
    }

    private void updateCurrentAppData(List<AppData> appDataList) {
        mCurrentAppDataList.clear();
        if (appDataList != null) {
            mCurrentAppDataList.addAll(appDataList);
        }
        mAppListAdapter.setData(mCurrentAppDataList);
        refreshDeferredSelections();
        updateSearchHint();
    }

    private void refreshDeferredSelections() {
        if (supportsConfirmAction()) {
            mAppListAdapter.refreshSelections();
        }
    }

    private void updateSearchHint() {
        mSearchInputView.setHint(String.format(getString(R.string.search_apps_hint), mAppListAdapter.getData().size()));
    }

    private boolean isImePickerMode() {
        return mModeSelection == IME_MODE && mKey != null;
    }

    private boolean supportsConfirmAction() {
        return isImePickerMode();
    }

    private boolean isImePickerBusy() {
        return mImeSelectionApplying || isLoading();
    }

    private boolean isLoading() {
        return mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE;
    }

    private void setLoadingState(boolean isLoading) {
        mProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        updateActionState();
    }

    private void confirmImeSelection() {
        if (!supportsConfirmAction() || mImeSelectionApplying) {
            return;
        }

        LinkedHashSet<String> selectedPackages = ScopeManager.filterScopePackages(
            mAppListAdapter.getSelectedPackages(),
            mAvailableImePackages
        );
        LinkedHashSet<String> normalizedSelectedPackages = ScopeManager.normalizeScopePackages(selectedPackages);
        LinkedHashSet<String> currentScope = ScopeManager.peekNormalizedScopeSync();
        if (!selectedPackages.equals(mAppListAdapter.getSelectedPackages())) {
            mAppListAdapter.setSelectedPackages(selectedPackages);
        }

        LinkedHashSet<String> sourceScope = currentScope != null ? new LinkedHashSet<>(currentScope) : new LinkedHashSet<>();
        LinkedHashSet<String> targetScope = ScopeManager.filterInstalledScopePackages(this, sourceScope);
        targetScope.addAll(normalizedSelectedPackages);

        if (targetScope.equals(sourceScope)) {
            saveImeSelection(selectedPackages);
            setResult(RESULT_OK);
            finish();
            return;
        }

        setImeSelectionApplying(true);
        ScopeManager.applyScopeDiffAsync(this, sourceScope, targetScope,
            (success, message) -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                setImeSelectionApplying(false);
                if (success) {
                    saveImeSelection(selectedPackages);
                    setResult(RESULT_OK);
                    finish();
                    return;
                }

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            });
    }

    private void saveImeSelection(Set<String> selectedPackages) {
        if (mKey == null) {
            return;
        }
        PrefsBridge.putByApp(mKey, new LinkedHashSet<>(selectedPackages));
    }

    private void setImeSelectionApplying(boolean isApplying) {
        mImeSelectionApplying = isApplying;
        updateActionState();
    }

    private void updateActionState() {
        if (mSearchBar != null) {
            mSearchBar.setClickable(!isImePickerBusy());
        }
        invalidateOptionsMenu();
    }
}
