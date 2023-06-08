package com.sevtinge.cemiuiler.ui.sub;

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
import android.widget.CheckBox;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.data.AppData;
import com.sevtinge.cemiuiler.data.adapter.AppDataAdapter;
import com.sevtinge.cemiuiler.provider.SharedPrefsProvider;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import moralnorm.appcompat.app.Fragment;

public class AppPickerFragment extends Fragment {

    private Bundle args;
    private String key = null;
    private boolean appSelector;

    private View mRootView;
    private ProgressBar mAmProgress;
    private RecyclerView mAppListRv;
    private AppDataAdapter mAppListAdapter;
    private List<AppData> appDataList;
    public Handler mHandler;

    private Set<String> selectedApps;

    @Override
    public View onInflateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_app_picker, container, false);
        initView();
        return mRootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getAppCompatActivity().getAppCompatActionBar().hide();
        args = getActivity().getIntent().getExtras();
        appSelector = args.getBoolean("app_selector");
        if (appSelector) {
            key = args.getString("app_selector_key");
        } else {
            key = args.getString("key");
        }
        initData();
    }

    private void initView() {
        mAmProgress = mRootView.findViewById(R.id.am_progressBar);
        mAppListRv = mRootView.findViewById(R.id.app_list_rv);
        mAppListRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mAppListAdapter = new AppDataAdapter(getActivity(), key, appSelector ? 1 : 0);
        mHandler = new Handler();

        mAppListAdapter.setOnItemClickListener((view, position, appData, isCheck) -> {
            if (appSelector) {
                PrefsUtils.editor().putString(key + "_app", appData.packageName + "|" + appData.activityName).apply();
                getActivity().finish();
            } else {
                CheckBox checkBox = view.findViewById(android.R.id.checkbox);
//                String key = "prefs_key_system_framework_clean_share_apps";
                selectedApps = new LinkedHashSet<>(PrefsUtils.mSharedPreferences.getStringSet(key, new LinkedHashSet<String>()));
                if (checkBox.isChecked()) {
                    checkBox.setChecked(false);
                    selectedApps.remove(appData.packageName);
                } else {
                    checkBox.setChecked(true);
                    selectedApps.add(appData.packageName);
                }
                PrefsUtils.mSharedPreferences.edit().putStringSet(key, selectedApps).apply();
            }
        });
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
        mainIntent.putExtra("Cemiuiler", true);
        List<ResolveInfo> packs = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL | PackageManager.MATCH_DISABLED_COMPONENTS);

        mainIntent = new Intent();
        mainIntent.setAction(Intent.ACTION_VIEW);
        mainIntent.setData(Uri.parse("https://home.miui.com/"));
        mainIntent.putExtra("Cemiuiler", true);
        List<ResolveInfo> packs2 = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL);

        mainIntent = new Intent();
        mainIntent.setAction(Intent.ACTION_VIEW);
        mainIntent.setData(Uri.parse("vnd.youtube:n9AcG0glVu4"));
        mainIntent.putExtra("Cemiuiler", true);
        List<ResolveInfo> packs3 = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL);

        mainIntent = new Intent();
        mainIntent.setAction(Intent.ACTION_SEND);
        mainIntent.putExtra(Intent.EXTRA_TEXT, "Cemiuiler is the best!");
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
                if (exists) continue;
                app = new AppData();
                app.icon = pack.activityInfo.applicationInfo.loadIcon(pm);
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
            appData.icon = resolveInfo.loadIcon(packageManager);
            appData.label = resolveInfo.loadLabel(packageManager).toString();
            appData.packageName = resolveInfo.activityInfo.packageName;
            appData.activityName = resolveInfo.activityInfo.name;
            appData.enabled = resolveInfo.activityInfo.enabled;
            appInfoList.add(appData);
        }
    }
}
