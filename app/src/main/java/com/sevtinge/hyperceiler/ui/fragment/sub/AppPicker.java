package com.sevtinge.hyperceiler.ui.fragment.sub;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
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
import com.sevtinge.hyperceiler.provider.SharedPrefsProvider;
import com.sevtinge.hyperceiler.utils.BitmapUtils;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import java.util.ArrayList;
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
        // mAppListRv.set(new LinearLayoutManager(getContext()));
        mAppListAdapter = new AppDataAdapter(getActivity(), R.layout.item_app_list, getAppInfo(getContext()), key, modeSelection);
        mAppListRv.setAdapter(mAppListAdapter);

        mAppListRv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppData appData = getAppInfo(getContext()).get((int) id);
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
//                String key = "prefs_key_system_framework_clean_share_apps";
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
                appDataList = getAppInfo(getContext());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAppListRv.setAdapter(mAppListAdapter);
                        mAppListAdapter.setData(appDataList);
                        mAmProgress.setVisibility(View.INVISIBLE);
                        mAppListRv.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }


    /**
     * 该方法提供了用于判断一个程序是系统程序还是用户程序的功能。
     *
     * @param applicationInfo
     * @return true 用户自己安装的软件
     * fasle  系统软件.
     */
    public static boolean filterApp(ApplicationInfo applicationInfo) {
        if ((applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            return true;
        } else if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            return true;
        }
        return false;
    }


    public void getOpenWithApps(Context context, List<AppData> appInfoList) {
        PackageManager pm = context.getPackageManager();

        Intent mainIntent = new Intent();
        mainIntent.setAction(Intent.ACTION_VIEW);
        mainIntent.setDataAndType(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/test/5"), "*/*");
        mainIntent.putExtra("HyperCeiler", true);
        List<ResolveInfo> packs = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL | PackageManager.MATCH_DISABLED_COMPONENTS);

        mainIntent = new Intent();
        mainIntent.setAction(Intent.ACTION_VIEW);
        mainIntent.setData(Uri.parse("https://home.miui.com/"));
        mainIntent.putExtra("HyperCeiler", true);
        List<ResolveInfo> packs2 = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL);

        mainIntent = new Intent();
        mainIntent.setAction(Intent.ACTION_VIEW);
        mainIntent.setData(Uri.parse("vnd.youtube:n9AcG0glVu4"));
        mainIntent.putExtra("HyperCeiler", true);
        List<ResolveInfo> packs3 = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL);

        mainIntent = new Intent();
        mainIntent.setAction(Intent.ACTION_SEND);
        mainIntent.putExtra(Intent.EXTRA_TEXT, "HyperCeiler is the best!");
        mainIntent.setType("*/*");
        List<ResolveInfo> packs4 = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL);

        packs.addAll(packs2);
        packs.addAll(packs3);
        packs.addAll(packs4);

        AppData app;
        for (ResolveInfo pack : packs)
            try {
                boolean exists = false;
                for (AppData openWithApp : appInfoList) {
                    if (openWithApp.packageName.equals(pack.activityInfo.applicationInfo.packageName)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    continue;
                }
                app = new AppData();
                app.icon = BitmapUtils.drawableToBitmap(pack.activityInfo.applicationInfo.loadIcon(pm));
                app.packageName = pack.activityInfo.applicationInfo.packageName;
                app.enabled = pack.activityInfo.applicationInfo.enabled;
                app.label = pack.activityInfo.applicationInfo.loadLabel(pm).toString();
                appInfoList.add(app);
            } catch (Throwable e) {
                e.printStackTrace();
            }
    }


    public List<AppData> getAppInfo(Context context) {
        List<AppData> appDataList = new ArrayList<>();
        if (appSelector) {
            getAppSelector(context, appDataList);
        } else {
            getOpenWithApps(context, appDataList);
        }
        return appDataList;
    }

    public void getAppSelector(Context context, List<AppData> appInfoList) {
        PackageManager packageManager = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(mainIntent, 0);

        AppData appData;
        for (ResolveInfo resolveInfo : resolveInfos) {
            appData = new AppData();
            appData.icon = BitmapUtils.drawableToBitmap(resolveInfo.loadIcon(packageManager));
            appData.label = resolveInfo.loadLabel(packageManager).toString();
            appData.packageName = resolveInfo.activityInfo.packageName;
            appData.activityName = resolveInfo.activityInfo.name;
            appData.enabled = resolveInfo.activityInfo.enabled;
            appInfoList.add(appData);
        }
    }
}
