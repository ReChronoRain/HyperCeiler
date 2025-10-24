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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.sub;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sevtinge.hyperceiler.common.callback.IAppSelectCallback;
import com.sevtinge.hyperceiler.common.model.adapter.AppDataAdapter;
import com.sevtinge.hyperceiler.common.model.data.AppData;
import com.sevtinge.hyperceiler.common.model.data.AppDataManager;
import com.sevtinge.hyperceiler.common.callback.SearchCallback;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.hook.utils.BitmapUtils;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
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

    private static final int DELAY_LOAD_DATA = 120;

    private String mKey;
    private int mModeSelection;

    private View mSearchBar;
    private TextView mSearchInputView;
    private ProgressBar mProgressBar;
    private NestedHeaderLayout mNestedHeaderLayout;
    private RecyclerView mAppListRecyclerView;
    private AppDataAdapter mAppListAdapter;
    private Handler mHandler = new Handler();
    private SearchCallback mSearchCallback;

    private final AppDataManager mAppDataManager = new AppDataManager();
    private List<AppData> mOriginalAppDataList = new ArrayList<>(); // 原始数据备份
    private List<AppData> mCurrentAppDataList = new ArrayList<>();  // 当前显示数据

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_picker);
        setExtraHorizontalPaddingEnable(true);

        extractIntentData();
        initializeViews();
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
                Log.e(TAG, "Key is null for mode: " + mModeSelection);
                finish();
            }
        }
    }

    private boolean isKeyRequiredMode(int mode) {
        return mode == APP_OPEN_MODE || mode == LAUNCHER_MODE ||
            mode == INPUT_MODE || mode == PROCESS_TEXT_MODE;
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
        mSearchInputView.setHint("搜索应用");
        mSearchBar.setClickable(false);
    }

    private void initializeRecyclerView() {
        mNestedHeaderLayout = findViewById(R.id.nested_header_layout);
        mAppListRecyclerView = findViewById(R.id.app_list_rv);
        mAppListRecyclerView.setVisibility(View.GONE);

        mAppListAdapter = new AppDataAdapter(new ArrayList<>(), mKey, mModeSelection);
        mAppListRecyclerView.setAdapter(mAppListAdapter);
        mAppListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAppListRecyclerView.addItemDecoration(new CardItemDecoration(this));
        mAppListRecyclerView.setItemAnimator(new CardDefaultItemAnimator());

        setupItemClickListener();
    }

    private void setupSearchCallback() {
        mSearchCallback = new SearchCallback(this, this);
        mSearchCallback.setup(mSearchBar, mNestedHeaderLayout.getScrollableView());
        mSearchBar.setOnClickListener(v -> startActionMode(mSearchCallback, 0));
    }

    private void setupItemClickListener() {
        mAppListAdapter.setOnItemClickListener((itemView, appData, position) -> {
            handleAppItemClick(appData);
        });
    }

    private void handleAppItemClick(AppData appData) {
        switch (mModeSelection) {
            case CALLBACK_MODE -> {
                sendCallbackResult(appData);
            }
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
                .setOnCancelListener(dialog -> dialog.dismiss())
                .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing edit dialog", e);
        }
    }

    private void initializeData() {
        mProgressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                // 模拟加载延迟
                Thread.sleep(DELAY_LOAD_DATA);
                mHandler.post(this::loadAppData);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Data loading interrupted", e);
            }
        }).start();
    }

    private void loadAppData() {
        try {
            List<AppData> loadedData = mAppDataManager.getAppInfo(mModeSelection);
            processAndDisplayAppData(loadedData);
        } catch (Exception e) {
            runOnUiThread(() -> {
                mProgressBar.setVisibility(View.GONE);
                Toast.makeText(this, "加载应用列表失败", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void processAndDisplayAppData(List<AppData> loadedData) {
        // 处理数据：排序、移动特定应用到顶部等
        List<AppData> processedData = processAppData(loadedData);

        // 更新原始数据和当前数据
        mOriginalAppDataList.clear();
        mOriginalAppDataList.addAll(processedData);

        mCurrentAppDataList.clear();
        mCurrentAppDataList.addAll(processedData);

        runOnUiThread(() -> {
            mAppListAdapter.setData(mCurrentAppDataList);
            mProgressBar.setVisibility(View.GONE);
            mSearchBar.setClickable(true);
            mAppListRecyclerView.setVisibility(View.VISIBLE);
        });
    }

    private List<AppData> processAppData(List<AppData> data) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 排序
        Collator collator = Collator.getInstance(Locale.getDefault());
        Collections.sort(data, (app1, app2) -> collator.compare(app1.label, app2.label));

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

        // 3. 移动选中的应用到顶部
        if (mKey != null) {
            Set<String> selectedApps = new LinkedHashSet<>(
                PrefsUtils.mSharedPreferences.getStringSet(mKey, new LinkedHashSet<>()));

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
            mCurrentAppDataList.clear();
            mCurrentAppDataList.addAll(mOriginalAppDataList);
            mAppListAdapter.setData(mCurrentAppDataList);
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
        mCurrentAppDataList.clear();
        mCurrentAppDataList.addAll(filteredList);
        mAppListAdapter.setData(mCurrentAppDataList);

        Log.d(TAG, "filterAppList: filtered " + filteredList.size() + " items for: " + searchTerm);
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
        mCurrentAppDataList.clear();
        mCurrentAppDataList.addAll(mOriginalAppDataList);
        mAppListAdapter.setData(mCurrentAppDataList);
        mAppListRecyclerView.scrollToPosition(0);
    }

    @Override
    public void sendMsgToActivity(Drawable appIcon, String appName, String appPackageName, String appVersion, String appActivityName) {
        Bitmap bitmap = BitmapUtils.drawableToBitmap(appIcon);
        Intent intent = new Intent();
        intent.putExtra("appIcon", BitmapUtils.Bitmap2Bytes(bitmap));
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
}
