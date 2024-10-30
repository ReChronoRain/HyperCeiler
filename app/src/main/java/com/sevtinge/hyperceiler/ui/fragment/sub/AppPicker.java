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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui.fragment.sub;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.callback.IAppSelectCallback;
import com.sevtinge.hyperceiler.callback.IEditCallback;
import com.sevtinge.hyperceiler.data.AppData;
import com.sevtinge.hyperceiler.data.adapter.AppDataAdapter;
import com.sevtinge.hyperceiler.utils.BitmapUtils;
import com.sevtinge.hyperceiler.utils.PackagesUtils;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import fan.appcompat.app.AlertDialog;

public class AppPicker extends Fragment {

    private final String TAG = "AppPicker";
    private String key = null;
    private int modeSelection;
    private View mRootView;
    private ProgressBar mAmProgress;
    private ListView mAppListRv;
    private AppDataAdapter mAppListAdapter;
    public Handler mHandler;
    private Set<String> selectedApps;
    private List<AppData> appDataList = new ArrayList<>();
    private final HashMap<String, Integer> hashMap = new HashMap<>();
    private IAppSelectCallback mAppSelectCallback;

    public static IEditCallback iEditCallback;

    public static final int APP_OPEN_MODE = 0;

    public static final int LAUNCHER_MODE = 1;
    public static final int CALLBACK_MODE = 2;
    public static final int INPUT_MODE = 3;
    public static final int PROCESS_TEXT_MODE = 4;

    public void setAppSelectCallback(IAppSelectCallback callback) {
        mAppSelectCallback = callback;
    }

    public interface EditDialogCallback {
        void onInputReceived(String userInput);
    }

    public static void setEditCallback(IEditCallback editCallback) {
        iEditCallback = editCallback;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_app_picker, container, false);
        initView();
        return mRootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().setTitle(R.string.array_global_actions_launch_choose);
        Bundle args = requireActivity().getIntent().getExtras();
        assert args != null;
        modeSelection = args.getInt("mode");
        switch (modeSelection) {
            case APP_OPEN_MODE, LAUNCHER_MODE, INPUT_MODE, PROCESS_TEXT_MODE -> key = args.getString("key");
            default -> {
            }
        }
        mHandler = new Handler();
        initData();
    }

    private void initView() {
        mAmProgress = mRootView.findViewById(R.id.am_progressBar);
        mAppListRv = mRootView.findViewById(R.id.app_list_rv);
        mAppListRv.setVisibility(View.GONE);

        mAppListRv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppData appData = appDataList.get((int) id);
                // Log.e(TAG, "onItemClick: " + appData.packageName, null);
                switch (modeSelection) {
                    case CALLBACK_MODE -> {
                        mAppSelectCallback.sendMsgToActivity(BitmapUtils.Bitmap2Bytes(appData.icon),
                                appData.label,
                                appData.packageName,
                                appData.versionName + "(" + appData.versionCode + ")",
                                appData.activityName);
                        requireActivity().finish();
                    }
                    case LAUNCHER_MODE, APP_OPEN_MODE, PROCESS_TEXT_MODE -> {
                        CheckBox checkBox = view.findViewById(android.R.id.checkbox);
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
                    case INPUT_MODE -> showEditDialog(appData.label, new EditDialogCallback() {
                                @Override
                                public void onInputReceived(String userInput) {
                                    iEditCallback.editCallback(appData.label, appData.packageName, userInput);
                                }
                            }
                    );
                }
            }
        });
    }

    private void showEditDialog(String defaultText, EditDialogCallback callback) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.edit_dialog, null);
        EditText input = view.findViewById(R.id.title);
        input.setText(defaultText);

        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.edit)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String userInput = input.getText().toString();
                    callback.onInputReceived(userInput);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void initData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
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

                        mAppListAdapter = new AppDataAdapter(requireActivity(),
                                R.layout.item_app_list, appDataList, key, modeSelection);
                        mAppListRv.setAdapter(mAppListAdapter);

                        mAmProgress.setVisibility(View.GONE);
                        mAppListRv.setVisibility(View.VISIBLE);
                    }
                }, 120);
            }
        }).start();
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
}
