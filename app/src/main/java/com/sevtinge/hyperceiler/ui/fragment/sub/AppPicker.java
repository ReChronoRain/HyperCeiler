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

import android.os.Bundle;
import android.os.Handler;
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
import com.sevtinge.hyperceiler.utils.PackageManagerUtils;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import moralnorm.appcompat.app.AlertDialog;

public class AppPicker extends Fragment {

    private Bundle args;
    private String TAG = "AppPicker";
    private String key = null;
    private boolean appSelector;
    private int modeSelection;
    private View mRootView;
    private ProgressBar mAmProgress;
    private ListView mAppListRv;
    private AppDataAdapter mAppListAdapter;
    private List<AppData> appDataList;
    public Handler mHandler;
    private Set<String> selectedApps;
    private IAppSelectCallback mAppSelectCallback;

    public static IEditCallback iEditCallback;

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
        args = requireActivity().getIntent().getExtras();
        assert args != null;
        appSelector = args.getBoolean("is_app_selector");
        modeSelection = args.getInt("need_mode");
        if (appSelector) {
            if (modeSelection == 3) {
                key = args.getString("key");
            } else
                key = args.getString("app_selector_key");
        } else {
            key = args.getString("key");
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
                AppData appData = getAppInfo().get((int) id);
                // Log.e(TAG, "onItemClick: " + appData.packageName, null);
                switch (modeSelection) {
                    case 1 -> {
                        mAppSelectCallback.sendMsgToActivity(BitmapUtils.Bitmap2Bytes(appData.icon),
                            appData.label,
                            appData.packageName,
                            appData.versionName + "(" + appData.versionCode + ")",
                            appData.activityName);
                        requireActivity().finish();
                    }
                    case 2 -> {
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
                    case 3 -> {
                        showEditDialog(appData.label, new EditDialogCallback() {
                                @Override
                                public void onInputReceived(String userInput) {
                                    iEditCallback.editCallback(appData.label, appData.packageName, userInput);
                                }
                            }
                        );
                    }
                }
            }
        });
    }

    private void showEditDialog(String defaultText, EditDialogCallback callback) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.edit_dialog, null);
        EditText input = view.findViewById(R.id.title);
        input.setText(defaultText);

        new AlertDialog.Builder(getActivity())
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
                        mAppListAdapter = new AppDataAdapter(getActivity(), R.layout.item_app_list, getAppInfo(), key, modeSelection);
                        mAppListRv.setAdapter(mAppListAdapter);
                        mAmProgress.setVisibility(View.GONE);
                        mAppListRv.setVisibility(View.VISIBLE);
                    }
                }, 120);
            }
        }).start();
    }

    public List<AppData> getAppInfo() {
        List<AppData> appDataList;
        if (appSelector) {
            appDataList = PackageManagerUtils.getPackageByLauncher();
        } else {
            appDataList = PackageManagerUtils.getOpenWithApps();
        }
        return appDataList;
    }
}
