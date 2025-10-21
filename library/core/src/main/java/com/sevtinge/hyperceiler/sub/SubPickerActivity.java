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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sevtinge.hyperceiler.common.callback.IAppSelectCallback;
import com.sevtinge.hyperceiler.common.model.adapter.AppDataAdapter;
import com.sevtinge.hyperceiler.common.model.data.AppData;
import com.sevtinge.hyperceiler.common.utils.PackagesUtils;
import com.sevtinge.hyperceiler.common.callback.SearchCallback;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.hook.utils.BitmapUtils;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
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

    private final String TAG = "AppPicker";
    private String key = null;
    private int modeSelection;

    private View mSearchBar;
    private TextView mSearchInputView;
    private ProgressBar mAmProgress;

    private NestedHeaderLayout mNestedHeader;

    private RecyclerView mAppListRv;
    private AppDataAdapter mAppListAdapter;
    private Handler mHandler = new Handler();
    private Set<String> selectedApps;
    private List<AppData> appDataList = new ArrayList<>();
    private final HashMap<String, Integer> hashMap = new HashMap<>();
    private SearchCallback mSearchCallBack;

    public static final int APP_OPEN_MODE = 0;

    public static final int LAUNCHER_MODE = 1;
    public static final int CALLBACK_MODE = 2;
    public static final int INPUT_MODE = 3;
    public static final int PROCESS_TEXT_MODE = 4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_app_picker);
        setExtraHorizontalPaddingEnable(true);
        Bundle args = getIntent().getExtras();
        assert args != null;
        modeSelection = args.getInt("mode");
        switch (modeSelection) {
            case APP_OPEN_MODE, LAUNCHER_MODE, INPUT_MODE, PROCESS_TEXT_MODE ->
                key = args.getString("key");
            default -> {}
        }
        initView();
        initData();
    }

    @Override
    public void onContentInsetChanged(Rect rect) {
        super.onContentInsetChanged(rect);
        if (mAppListRv != null) {
            mAppListRv.setPadding(
                mAppListRv.getPaddingLeft(),
                mAppListRv.getPaddingTop(),
                mAppListRv.getPaddingRight(),
                rect.bottom
            );
        }
    }

    private void initView() {

        mAmProgress = findViewById(R.id.am_progressBar);

        mSearchBar = findViewById(R.id.search_bar);
        mSearchInputView = mSearchBar.findViewById(android.R.id.input);
        mSearchInputView.setHint("搜索应用");

        mNestedHeader = findViewById(R.id.nested_header_layout);
        mNestedHeader.setHeaderViewVisible(false);
        registerCoordinateScrollView(mNestedHeader);

        mAppListRv = findViewById(R.id.app_list_rv);
        mSearchBar.setClickable(false);
        mAppListRv.setVisibility(View.GONE);

        mSearchCallBack = new SearchCallback(this, this);
        mSearchCallBack.setup(mSearchBar, mNestedHeader.getScrollableView());
        mSearchBar.setOnClickListener(v -> startActionMode(mSearchCallBack, 0));

        mAppListAdapter = new AppDataAdapter(appDataList, key, modeSelection);
        mAppListRv.setAdapter(mAppListAdapter);
        mAppListRv.setLayoutManager(new LinearLayoutManager(this));
        mAppListRv.addItemDecoration(new CardItemDecoration(this));
        mAppListRv.setItemAnimator(new CardDefaultItemAnimator());

        mAppListAdapter.setOnItemClickListener((itemView, appData, position) -> {
            // Log.e(TAG, "onItemClick: " + appData.packageName, null);
            switch (modeSelection) {
                case CALLBACK_MODE -> {
                    sendMsgToActivity(appData.icon,
                        appData.label,
                        appData.packageName,
                        appData.versionName + "(" + appData.versionCode + ")",
                        appData.activityName);
                    finish();
                }
                case LAUNCHER_MODE, APP_OPEN_MODE, PROCESS_TEXT_MODE -> {
                    CheckBox checkBox = itemView.findViewById(android.R.id.checkbox);
                    selectedApps = new LinkedHashSet<>(PrefsUtils.mSharedPreferences.getStringSet(key, new LinkedHashSet<>()));
                    if (checkBox.isChecked()) {
                        checkBox.setChecked(false);
                        selectedApps.remove(appData.packageName);
                    } else {
                        checkBox.setChecked(true);
                        selectedApps.add(appData.packageName);
                    }
                    PrefsUtils.mSharedPreferences.edit().putStringSet(key, selectedApps).apply();
                }
                case INPUT_MODE -> showEditDialog(appData);
            }
        });
    }

    private void showEditDialog(AppData data) {
        View view = LayoutInflater.from(this).inflate(R.layout.edit_dialog, null);
        EditText input = view.findViewById(R.id.title);
        input.setText(data.label);

        new AlertDialog.Builder(this)
            .setTitle(R.string.edit)
            .setView(view)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                String userInput = input.getText().toString();
                mAppListAdapter.editCallback(data.label, data.packageName, userInput);
                data.label = userInput;
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                dialog.dismiss();
            })
            .show();
    }

    private void initData() {
        new Thread(() -> mHandler.postDelayed(() -> {
            notifyAppDataList();

            mAppListAdapter.updateData(appDataList);

            mAmProgress.setVisibility(View.GONE);
            mSearchBar.setClickable(true);
            mAppListRv.setVisibility(View.VISIBLE);
        }, 120)).start();
    }

    public void notifyAppDataList() {
        appDataList = getAppInfo();

        Collator collator = Collator.getInstance(Locale.getDefault());
        appDataList.sort((app1, app2) -> collator.compare(app1.label, app2.label));

        AppData tagApp = null;
        for (AppData app : appDataList) {
            if ("com.android.apps.tag".equals(app.packageName)) {
                tagApp = app;
                break;
            }
        }
        if (tagApp != null) {
            appDataList.remove(tagApp);
            appDataList.add(0, tagApp);
        }

        selectedApps = new LinkedHashSet<>(PrefsUtils.mSharedPreferences.getStringSet(key, new LinkedHashSet<>()));
        List<AppData> selectedAppList = new ArrayList<>();
        for (String packageName : selectedApps) {
            for (AppData appData : appDataList) {
                if (packageName.equals(appData.packageName)) {
                    selectedAppList.add(appData);
                    appDataList.remove(appData);
                    break;
                }
            }
        }
        appDataList.addAll(0, selectedAppList);
    }

    public List<AppData> getAppInfo() {
        return switch (modeSelection) {
            case LAUNCHER_MODE, CALLBACK_MODE, INPUT_MODE ->
                PackagesUtils.getPackagesByCode(new PackagesUtils.IPackageCode() {
                    @Override
                    public List<Parcelable> getPackageCodeList(PackageManager pm) {

                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        List<ResolveInfo> resolveInfoList = new ArrayList<>();
                        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES);
                        List<ResolveInfo> resolveInfosHaveNoLauncher = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN), PackageManager.GET_ACTIVITIES);

                        hashMap.clear();
                        for (ResolveInfo resolveInfo : resolveInfosHaveNoLauncher) {
                            Integer added = hashMap.get(resolveInfo.activityInfo.applicationInfo.packageName);
                            if (added == null || added != 1) {
                                hashMap.put(resolveInfo.activityInfo.applicationInfo.packageName, 1);
                            } else {
                                continue;
                            }
                            resolveInfoList.add(resolveInfo);
                        }

                        Collator collator = Collator.getInstance(Locale.getDefault());
                        resolveInfoList.sort((r1, r2) -> {
                            CharSequence label1 = r1.loadLabel(pm);
                            CharSequence label2 = r2.loadLabel(pm);
                            return collator.compare(label1.toString(), label2.toString());
                        });

                        return new ArrayList<>(resolveInfoList);
                    }
                });
            case APP_OPEN_MODE -> PackagesUtils.getOpenWithApps();
            case PROCESS_TEXT_MODE ->
                PackagesUtils.getPackagesByCode(new PackagesUtils.IPackageCode() {
                    @Override
                    public List<Parcelable> getPackageCodeList(PackageManager pm) {
                        Intent intent = new Intent()
                            .setAction(Intent.ACTION_PROCESS_TEXT)
                            .setType("text/plain");
                        intent.putExtra("HyperCeiler", true);
                        List<ResolveInfo> resolveInfos =
                            pm.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES);
                        List<ResolveInfo> resolveInfoList = new ArrayList<>();
                        hashMap.clear();
                        for (ResolveInfo resolveInfo : resolveInfos) {
                            Integer added = hashMap.get(resolveInfo.activityInfo.applicationInfo.packageName);
                            if (added == null || added != 1) {
                                hashMap.put(resolveInfo.activityInfo.applicationInfo.packageName, 1);
                            } else {
                                continue;
                            }
                            resolveInfoList.add(resolveInfo);
                        }
                        return new ArrayList<>(resolveInfoList);
                    }
                });
            default -> new ArrayList<>();
        };
    }

    private void filterAppList(String keyword) {
        List<AppData> filteredList = new ArrayList<>();
        for (AppData appData : appDataList) {
            if (appData.label.toLowerCase().contains(keyword.toLowerCase())) {
                filteredList.add(appData);
            }
        }
        mAppListAdapter.updateData(filteredList);
    }


    @Override
    public boolean onQueryTextChange(String newText) {
        mAppListAdapter.resetData();
        filterAppList(newText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mAppListAdapter.resetData();
        filterAppList(query);
        return true;
    }

    @Override
    public void onCreateSearchMode(ActionMode actionMode, Menu menu) {

    }

    @Override
    public void onSearchModeAnimStart(boolean enabled) {
        if (enabled) {
            mNestedHeader.setInSearchMode(true);
        } else {
            mAppListRv.stopScroll();
        }
    }

    @Override
    public void onSearchModeAnimUpdate(boolean enabled, float f) {

    }

    @Override
    public void onSearchModeAnimStop(boolean enabled) {
        if (!enabled) {
            mNestedHeader.setInSearchMode(false);
            mAppListRv.scrollToPosition(0);
        }
    }

    @Override
    public void onDestroySearchMode(ActionMode actionMode) {
        mAppListAdapter.resetData();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterCoordinateScrollView(mNestedHeader);
    }
}
