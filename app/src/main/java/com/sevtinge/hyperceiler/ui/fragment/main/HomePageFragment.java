package com.sevtinge.hyperceiler.ui.fragment.main;

import static com.sevtinge.hyperceiler.prefs.PreferenceHeader.notInSelectedScope;
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
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.callback.ModSearchCallback;
import com.sevtinge.hyperceiler.data.ModData;
import com.sevtinge.hyperceiler.data.adapter.ModSearchAdapter;
import com.sevtinge.hyperceiler.expansion.utils.SignUtils;
import com.sevtinge.hyperceiler.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.ui.activity.MainActivityContextHelper;
import com.sevtinge.hyperceiler.ui.activity.SubSettings;
import com.sevtinge.hyperceiler.ui.fragment.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.ui.fragment.main.helper.HomepageEntrance;
import com.sevtinge.hyperceiler.utils.SettingLauncherHelper;
import com.sevtinge.hyperceiler.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.ui.fragment.main.ContentFragment.IFragmentChange;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;

import fan.appcompat.app.Fragment;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.springback.view.SpringBackLayout;

public class HomePageFragment extends DashboardFragment
        implements HomepageEntrance.EntranceState,
        ModSearchCallback.OnSearchListener, IFragmentChange {

    View mRootView;
    ViewGroup mPrefsContainer;
    NestedHeaderLayout mNestedHeaderLayout;

    View mSearchBar;
    TextView mSearchInputView;
    RecyclerView mSearchResultView;
    ModSearchAdapter mSearchAdapter;
    ModSearchCallback mSearchCallBack;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_home_page, container, false);
        mPrefsContainer = mRootView.findViewById(R.id.prefs_container);
        View view = super.onCreateView(inflater, container, savedInstanceState);
        setOverlayMode();
        mPrefsContainer.addView(view);

        RecyclerView listView = getListView();
        View parent = (View) listView.getParent();
        if (parent instanceof SpringBackLayout) {
            parent.setEnabled(false);
            listView.setPaddingRelative(listView.getPaddingStart(), 0, listView.getPaddingEnd(), 0);
        }

        mNestedHeaderLayout = mRootView.findViewById(R.id.nested_header);
        registerCoordinateScrollView(mNestedHeaderLayout);
        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initSearchView(view);
    }

    private void initSearchView(View view) {
        mSearchBar = view.findViewById(R.id.search_bar);
        mSearchInputView = view.findViewById(android.R.id.input);
        mSearchResultView = view.findViewById(R.id.search_result_view);

        mSearchAdapter = new ModSearchAdapter();
        mSearchInputView.setHint(getResources().getString(R.string.search));
        mSearchResultView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mSearchResultView.setAdapter(mSearchAdapter);

        onTextSearch();

        mSearchBar.setOnClickListener(v -> startSearchMode());
        mSearchAdapter.setOnItemClickListener((v, ad) -> onSearchItemClickListener(ad));
    }


    public void onTextSearch() {
        if (mSearchBar != null) {
            onSearchRequest(mSearchBar);
        }
    }

    protected void onSearchRequest(View view) {
        if (mSearchCallBack == null) {
            mSearchCallBack = new ModSearchCallback(getActivity(), mSearchResultView, this);
        }
        mSearchCallBack.setup(view, mNestedHeaderLayout.getScrollableView());
    }

    @Override
    public void onCreateSearchMode(ActionMode mode, Menu menu) {
        //mInSearchMode = true;
        if (isAdded()) {
            mNestedHeaderLayout.setInSearchMode(true);
            mPrefsContainer.setVisibility(View.GONE);
            mSearchResultView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroySearchMode(ActionMode mode) {
        //mInSearchMode = false;
    }

    @Override
    public void onSearchModeAnimStart(boolean z) {

    }

    @Override
    public void onSearchModeAnimStop(boolean z) {
        if (isAdded()) {
            if (z) {
                //homeViewModel.setVpScrollable(false);
            } else {
                mNestedHeaderLayout.setInSearchMode(false);
                mPrefsContainer.setVisibility(View.VISIBLE);
                mSearchResultView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onSearchModeAnimUpdate(boolean z, float f) {

    }

    private void startSearchMode() {
        ((Fragment) getParentFragment()).startActionMode(mSearchCallBack);
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

    Preference mCamera;
    Preference mSecurityCenter;
    Preference mMiLink;
    Preference mAod;
    Preference mGuardProvider;
    Preference mHeadtipWarn;
    Preference mHeadtipNotice;
    Preference mHeadtipBirthday;
    Preference mHeadtipHyperCeiler;
    Preference mHeadtipTip;
    MainActivityContextHelper mainActivityContextHelper;
    private final String TAG = "MainFragment";
    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    boolean mWarnTipVisible = false;
    boolean mNoticeTipVisible = false;
    boolean mBirthdayTipVisible = false;
    boolean mHyperCeilerTipVisible = false;
    boolean mTipTipVisible;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_main;
    }

    @Override
    public void initPrefs() {
        HomepageEntrance.setEntranceStateListen(this);
        Resources resources = getResources();
        ThreadPoolManager.getInstance().submit(() -> {
            XmlResourceParser xml = null;
            try {
                xml = resources.getXml(R.xml.prefs_set_homepage_entrance);
                int event = xml.getEventType();
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && "SwitchPreference".equals(xml.getName())) {
                        String key = xml.getAttributeValue(ANDROID_NS, "key");
                        processSwitchPreference(key);
                    }
                    event = xml.next();
                }
            } catch (XmlPullParserException | IOException e) {
                AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
            } finally {
                if (xml != null) {
                    xml.close(); // Ensure the parser is closed
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
        mHeadtipTip = findPreference("prefs_key_headtip_tip");

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

        // 优先级由上往下递减，优先级低的会被覆盖执行
        // HyperCeiler
        isFuckCoolapkSDay();
        // Birthday
        isBirthday();
        // Notice
        isLoggerAlive();
        // Warn
        isSignPass();
        isFullSupportSysVer();
        isOfficialRom();
        // Tip
        isSupportAutoSafeMode();

    }

    private void processSwitchPreference(String key) {
        if (key != null) {
            String checkKey = key.replace("_state", "");
            boolean state = getSharedPreferences().getBoolean(key, true);
            if (!state) {
                PreferenceHeader preferenceHeader = findPreference(checkKey);
                if (preferenceHeader != null && preferenceHeader.isVisible()) {
                    preferenceHeader.setVisible(false);
                }
            }
        }
    }

    private void setPreference() {
        Resources resources = getResources();
        try (XmlResourceParser xml = resources.getXml(R.xml.prefs_main)) {
            int event = xml.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && isPreferenceHeaderTag(xml)) {
                    String key = xml.getAttributeValue(ANDROID_NS, "key");
                    String summary = xml.getAttributeValue(ANDROID_NS, "summary");
                    processPreferenceHeader(key, summary, xml);
                }
                event = xml.next();
            }
        } catch (XmlPullParserException | IOException e) {
            AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
        }
    }

    private boolean isPreferenceHeaderTag(XmlResourceParser xml) {
        return "com.sevtinge.hyperceiler.prefs.PreferenceHeader".equals(xml.getName());
    }

    private void processPreferenceHeader(String key, String summary, XmlResourceParser xml) {
        if (key != null && summary != null) {
            PreferenceHeader preferenceHeader = findPreference(key);
            if (preferenceHeader != null) {
                Drawable icon = getPackageIcon(summary);
                String name = getPackageName(summary);
                preferenceHeader.setIcon(icon);
                if (!summary.equals("android")) {
                    preferenceHeader.setTitle(name);
                }
            }
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
        if (mBirthdayTipVisible) return;;
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        mHeadtipBirthday.setVisible(currentMonth == Calendar.MAY && currentDay == 1);
        mBirthdayTipVisible = true;
    }

    public void isFuckCoolapkSDay() {
        if (mHyperCeilerTipVisible) return;;
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        mHeadtipHyperCeiler.setVisible(currentMonth == Calendar.JULY && currentDay == 14);
        mHeadtipHyperCeiler.setTitle(R.string.headtip_tip_fuck_coolapk);
        mHyperCeilerTipVisible = true;
    }

    public void isOfficialRom() {
        if (mWarnTipVisible) return;
        mHeadtipWarn.setTitle(R.string.headtip_warn_not_offical_rom);
        mHeadtipWarn.setVisible(getIsOfficialRom());
        mWarnTipVisible = true;
    }

    public void isFullSupportSysVer() {
        if (mWarnTipVisible) return;
        mHeadtipWarn.setTitle(R.string.headtip_warn_unsupport_sysver);
        mHeadtipWarn.setVisible(!isFullSupport());
        mWarnTipVisible = true;
    }

    public void isLoggerAlive() {
        if (mNoticeTipVisible) return;
        if (!IS_LOGGER_ALIVE && BuildConfig.BUILD_TYPE != "release") {
            mHeadtipNotice.setTitle(R.string.headtip_notice_dead_logger);
            mHeadtipNotice.setVisible(true);
            mNoticeTipVisible = true;
        }
    }

    public boolean getIsOfficialRom() {
        String baseOs = getBaseOs();
        String romAuthor = getRomAuthor();
        String host = SystemSDKKt.getHost();

        boolean isNotCustomBaseOs = !baseOs.startsWith("V") &&
                !baseOs.startsWith("Xiaomi") &&
                !baseOs.startsWith("Redmi") &&
                !baseOs.startsWith("POCO") &&
                !"null".equals(baseOs);

        boolean hasRomAuthor = !romAuthor.isEmpty();

        boolean isNotCustomHost = !host.startsWith("pangu-build-component-system") &&
                !host.startsWith("builder-system") &&
                !host.startsWith("non-pangu-pod") &&
                !host.equals("xiaomi.com");

        return isNotCustomBaseOs || hasRomAuthor || Objects.equals(host, "xiaomi.eu") || isNotCustomHost;
    }


    public void isSignPass() {
        if (mWarnTipVisible) return;
        mHeadtipWarn.setTitle(R.string.headtip_warn_sign_verification_failed);
        mHeadtipWarn.setVisible(!SignUtils.isSignCheckPass(requireContext()));
        mWarnTipVisible = true;
    }

    public void isSupportAutoSafeMode() {
        if (mTipTipVisible) return;
        mHeadtipTip.setTitle(R.string.headtip_tip_auto_safe_mode);
        mHeadtipTip.setVisible(notInSelectedScope.contains("android"));
        mTipTipVisible = true;
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

    @Override
    public void onEnter() {

    }

    @Override
    public void onLeave() {

    }
}
