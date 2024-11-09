/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui.fragment.main;

import static com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.dp2px;
import static com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.sp2px;
import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getBaseOs;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getRomAuthor;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isFullSupport;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.log.LogManager.IS_LOGGER_ALIVE;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.ModData;
import com.sevtinge.hyperceiler.data.adapter.ModSearchAdapter;
import com.sevtinge.hyperceiler.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.ui.activity.MainActivityContextHelper;
import com.sevtinge.hyperceiler.ui.activity.SubSettings;
import com.sevtinge.hyperceiler.ui.fragment.DashboardFragment;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.ui.fragment.main.helper.CantSeeAppsFragment;
import com.sevtinge.hyperceiler.ui.fragment.main.helper.HomepageEntrance;
import com.sevtinge.hyperceiler.utils.SettingLauncherHelper;
import com.sevtinge.hyperceiler.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.expansionpacks.utils.SignUtils;
import com.sevtinge.hyperceiler.utils.search.SearchModeHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;

import fan.appcompat.app.AppCompatActivity;
import fan.core.widget.NestedScrollView;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.springback.view.SpringBackLayout;
import fan.view.SearchActionMode;

public class MainFragment extends DashboardFragment implements HomepageEntrance.EntranceState {

    String lastFilter;
    View mSearchView;
    TextView mSearchInputView;
    RecyclerView mSearchResultView;
    ModSearchAdapter mSearchAdapter;

    View mRootView;
    NestedHeaderLayout mNestedHeaderLayout;
    NestedScrollView mScrollView;
    SpringBackLayout mSpringBackView;
    TextView mHelpCantSeeAppsView;

    Preference mCamera;
    Preference mSecurityCenter;
    Preference mMiLink;
    Preference mAod;
    Preference mGuardProvider;
    Preference mHeadtipWarn;
    Preference mHeadtipNotice;
    Preference mHeadtipBirthday;
    Preference mHeadtipHyperCeiler;
    MainActivityContextHelper mainActivityContextHelper;
    private final String TAG = "MainFragment";
    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_main;
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_home_page, container, false);
        ViewGroup prefsContainer = mRootView.findViewById(R.id.prefs_container);
        View view = super.onInflateView(inflater, container, savedInstanceState);
        prefsContainer.addView(view);



        mNestedHeaderLayout = mRootView.findViewById(R.id.nested_header);

        registerCoordinateScrollView(mNestedHeaderLayout);
        return mRootView;
    }

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
        //initSearchView(view);
        mHelpCantSeeAppsView = view.findViewById(R.id.help_cant_see_app);
        mHelpCantSeeAppsView.setVisibility(getSharedPreferences().getBoolean("prefs_key_help_cant_see_apps_switch", false) ? View.GONE : View.VISIBLE);
        mHelpCantSeeAppsView.setOnClickListener(v -> {
            SettingLauncherHelper.onStartSettings(
                    getContext(),
                    SubSettings.class,
                    CantSeeAppsFragment.class,
                    mHelpCantSeeAppsView.getText().toString()
            );
        });
    }

    private void initSearchView(View view) {
        mSearchView = view.findViewById(R.id.search_view);
        mSearchInputView = view.findViewById(android.R.id.input);
        mSearchResultView = view.findViewById(R.id.search_result_view);
        mSearchAdapter = new ModSearchAdapter();
        mSearchInputView.setHint(getResources().getString(R.string.search));
        mSearchResultView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mSearchResultView.setAdapter(mSearchAdapter);

        mSearchView.setOnClickListener(v -> startSearchMode());
        mSearchAdapter.setOnItemClickListener((v, ad) -> onSearchItemClickListener(ad));

        ViewCompat.setOnApplyWindowInsetsListener(mSearchResultView, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets inset = Insets.max(insets.getInsets(WindowInsetsCompat.Type.systemBars()),
                        insets.getInsets(WindowInsetsCompat.Type.displayCutout()));
                v.setPadding(0, 0, 0, inset.bottom);
                return insets;
            }
        });
    }

    private void onSearchItemClickListener(ModData ad) {
        Bundle args = new Bundle();
        args.putString(":settings:fragment_args_key", ad.key);
        SettingLauncherHelper.onStartSettingsForArguments(
                requireContext(),
                SubSettings.class,
                ad.fragment,
                args,
                ad.catTitleResId
        );

    }

    private SearchActionMode startSearchMode() {
        return SearchModeHelper.startSearchMode(
                (AppCompatActivity) requireActivity(),
                mSearchResultView,
                mRootView,
                mSearchView,
                mRootView,
                mSearchResultListener
        );
    }

    TextWatcher mSearchResultListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            findMod(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {
            // findMod(s.toString());
        }
    };

    void findMod(String filter) {
        lastFilter = filter;
        mSearchResultView.setVisibility(filter.isEmpty() ? View.GONE : View.VISIBLE);
        ModSearchAdapter adapter = (ModSearchAdapter) mSearchResultView.getAdapter();
        if (adapter == null) return;
        adapter.getFilter(requireActivity()).filter(filter);
    }

    @Override
    public void initPrefs() {
        HomepageEntrance.setEntranceStateListen(this);
        Resources resources = getResources();
        ThreadPoolManager.getInstance().submit(() -> {
            try (XmlResourceParser xml = resources.getXml(R.xml.prefs_set_homepage_entrance)) {
                try {
                    int event = xml.getEventType();
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG) {
                            if (xml.getName().equals("SwitchPreference")) {
                                String key = xml.getAttributeValue(ANDROID_NS, "key");
                                if (key != null) {
                                    String checkKey = key.replace("_state", "");
                                    boolean state = getSharedPreferences().getBoolean(key, true);
                                    if (!state) {
                                        PreferenceHeader preferenceHeader = findPreference(checkKey);
                                        if (preferenceHeader != null) {
                                            boolean visible = preferenceHeader.isVisible();
                                            if (visible) {
                                                preferenceHeader.setVisible(false);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        event = xml.next();
                    }
                } catch (XmlPullParserException | IOException e) {
                    AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
                }
            }
        });
        mCamera = findPreference("prefs_key_camera_2");
        mSecurityCenter = findPreference("prefs_key_security_center");
        mMiLink = findPreference("prefs_key_milink");
        mAod = findPreference("prefs_key_aod");
        mGuardProvider = findPreference("prefs_key_guardprovider");
        mHeadtipWarn = findPreference("prefs_key_headtip_warn");
        mHeadtipNotice = findPreference("prefs_key_headtip_notice");
        mHeadtipBirthday = findPreference("prefs_key_headtip_hyperceiler_birthday");
        mHeadtipHyperCeiler = findPreference("prefs_key_headtip_hyperceiler");

        if (isHyperOSVersion(1f)) {
            mSecurityCenter.setTitle(R.string.security_center_hyperos);
        } else {
            if (isPad()) {
                mSecurityCenter.setTitle(R.string.security_center_pad);
            } else {
                mSecurityCenter.setTitle(R.string.security_center);
            }
        }

        if (isMoreHyperOSVersion(1f)) {
            mCamera.setFragment("com.sevtinge.hyperceiler.ui.fragment.app.CameraNewFragment");
            mAod.setTitle(R.string.aod_hyperos);
            mMiLink.setTitle(R.string.milink_hyperos);
            mGuardProvider.setTitle(R.string.guard_provider_hyperos);
        } else {
            mCamera.setFragment("com.sevtinge.hyperceiler.ui.fragment.app.CameraFragment");
            mAod.setTitle(R.string.aod);
            mMiLink.setTitle(R.string.milink);
            mGuardProvider.setTitle(R.string.guard_provider);
        }

        setPreference();

        mainActivityContextHelper = new MainActivityContextHelper(requireContext());

        isBirthday();
        isFuckCoolapkSDay();
        isOfficialRom();
        isLoggerAlive();
        if (!getIsOfficialRom()) if (isFullSupport()) isSignPass(); else isFullSupportSysVer();

    }

    private void setPreference() {
        Resources resources = getResources();
        try (XmlResourceParser xml = resources.getXml(R.xml.prefs_main)) {
            int event = xml.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && xml.getName().equals("com.sevtinge.hyperceiler.prefs.PreferenceHeader")) {
                    String key = xml.getAttributeValue(ANDROID_NS, "key");
                    String summary = xml.getAttributeValue(ANDROID_NS, "summary");
                    if (key != null && summary != null) {
                        Drawable icon = getPackageIcon(summary); // 替换为获取图标的方法
                        String name = getPackageName(summary);
                        PreferenceHeader preferenceHeader = findPreference(key);
                        if (preferenceHeader != null) {
                            preferenceHeader.setIcon(icon);
                            if (!summary.equals("android")) preferenceHeader.setTitle(name);
                        }
                    }
                }
                event = xml.next();
            }
        } catch (XmlPullParserException | IOException e) {
            AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
        }
    }


    private Drawable getPackageIcon(String packageName) {
        try {
            return requireContext().getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getPackageName(String packageName) {
        try {
            return (String) requireContext().getPackageManager().getApplicationLabel(requireContext().getPackageManager().getApplicationInfo(packageName, 0));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null; // 如果包名找不到则返回 null
        }
    }

    public void isBirthday() {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        mHeadtipBirthday.setVisible(currentMonth == Calendar.MAY && currentDay == 1);
    }

    public void isFuckCoolapkSDay() {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        mHeadtipHyperCeiler.setVisible(currentMonth == Calendar.JULY && currentDay == 14);
        mHeadtipHyperCeiler.setTitle(R.string.headtip_tip_fuck_coolapk);
    }

    public void isOfficialRom() {
        mHeadtipWarn.setTitle(R.string.headtip_warn_not_offical_rom);
        mHeadtipWarn.setVisible(getIsOfficialRom());
    }

    public void isFullSupportSysVer() {
        mHeadtipWarn.setTitle(R.string.headtip_warn_unsupport_sysver);
        mHeadtipWarn.setVisible(isFullSupport());
    }

    public void isLoggerAlive() {
        if (!IS_LOGGER_ALIVE && BuildConfig.BUILD_TYPE != "release") {
            mHeadtipNotice.setTitle(R.string.headtip_notice_dead_logger);
            mHeadtipNotice.setVisible(true);
        }
    }

    public boolean getIsOfficialRom() {
        return (
                !getBaseOs().startsWith("V") &&
                        !getBaseOs().startsWith("Xiaomi") &&
                        !getBaseOs().startsWith("Redmi") &&
                        !getBaseOs().startsWith("POCO") &&
                        !getBaseOs().equals("null")
        ) ||
                !getRomAuthor().isEmpty() ||
                Objects.equals(SystemSDKKt.getHost(), "xiaomi.eu") ||
                (
                        !SystemSDKKt.getHost().startsWith("pangu-build-component-system") &&
                                !SystemSDKKt.getHost().startsWith("builder-system") &&
                                !SystemSDKKt.getHost().startsWith("non-pangu-pod") &&
                                !Objects.equals(SystemSDKKt.getHost(), "xiaomi.com")
                );
    }


    public void isSignPass() {
        mHeadtipWarn.setTitle(R.string.headtip_warn_sign_verification_failed);
        mHeadtipWarn.setVisible(!SignUtils.isSignCheckPass(requireContext()));
    }

    @Override
    public void onEntranceStateChange(String key, boolean state) {
        String mainKey = key.replace("_state", "");
        PreferenceHeader preferenceHeader = findPreference(mainKey);
        if (preferenceHeader != null) {
            boolean last = preferenceHeader.isVisible();
            if (!last || state) return;
            preferenceHeader.setVisible(false);
        }
    }
}
