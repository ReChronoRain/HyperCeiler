package com.sevtinge.hyperceiler.ui.app.main;

import static com.sevtinge.hyperceiler.prefs.PreferenceHeader.notInSelectedScope;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getBaseOs;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getRomAuthor;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isFullSupport;
import static com.sevtinge.hyperceiler.utils.log.LogManager.IS_LOGGER_ALIVE;

import android.content.pm.PackageManager;
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
import com.sevtinge.hyperceiler.ui.app.helper.HomepageEntrance;
import com.sevtinge.hyperceiler.ui.app.main.ContentFragment.IFragmentChange;
import com.sevtinge.hyperceiler.ui.app.main.utils.MainActivityContextHelper;
import com.sevtinge.hyperceiler.ui.base.SubSettings;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.SettingLauncherHelper;
import com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;
import java.util.function.BiConsumer;

import fan.appcompat.app.Fragment;
import fan.navigator.NavigatorFragmentListener;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.springback.view.SpringBackLayout;

public class HomePageFragment extends DashboardFragment
        implements HomepageEntrance.EntranceState, ModSearchCallback.OnSearchListener,
        NavigatorFragmentListener, IFragmentChange {

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
        args.putInt(":settings:fragment_resId", ad.xml);
        SettingLauncherHelper.onStartSettingsForArguments(
                requireContext(),
                SubSettings.class,
                ad.fragment,
                args,
                ad.catTitleResId
        );
    }

    Preference mHeadtipWarn;
    Preference mHeadtipNotice;
    Preference mHeadtipBirthday;
    Preference mHeadtipHyperCeiler;
    Preference mHeadtipTip;
    MainActivityContextHelper mainActivityContextHelper;
    private static final String TAG = "MainFragment";
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
        setPreference();
        mHeadtipWarn = findPreference("prefs_key_headtip_warn");
        mHeadtipNotice = findPreference("prefs_key_headtip_notice");
        mHeadtipBirthday = findPreference("prefs_key_headtip_hyperceiler_birthday");
        mHeadtipHyperCeiler = findPreference("prefs_key_headtip_hyperceiler");
        mHeadtipTip = findPreference("prefs_key_headtip_tip");
        mainActivityContextHelper = new MainActivityContextHelper(requireContext());

        // 优先级由上往下递减，优先级低的会被覆盖执行
        // HyperCeiler
        isFuckCoolapkSDay();
        // Birthday
        isBirthday();
        // Notice
        isLoggerAlive();
        // Warn
        checkWarnings();
        // Tip
        isSupportAutoSafeMode();

    }

    private void setPreference() {
        try {
            processXmlResource(R.xml.prefs_set_homepage_entrance, (key, summary) -> processSwitchPreference(key));
            processXmlResource(R.xml.prefs_main, this::processPreferenceHeader);
        } catch (XmlPullParserException | IOException e) {
            AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
        }

    }

    private void processXmlResource(int xmlResId, BiConsumer<String, String> processor) throws XmlPullParserException, IOException {
        try (XmlResourceParser xml = getResources().getXml(xmlResId)) {
            int event = xml.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "SwitchPreference".equals(xml.getName())) {
                    String key = xml.getAttributeValue(ANDROID_NS, "key");
                    processor.accept(key, null);
                }
                if (event == XmlPullParser.START_TAG && isPreferenceHeaderTag(xml)) {
                    String key = xml.getAttributeValue(ANDROID_NS, "key");
                    String summary = xml.getAttributeValue(ANDROID_NS, "summary");
                    processor.accept(key, summary);
                }
                event = xml.next();
            }
        }
    }

    private boolean isPreferenceHeaderTag(XmlResourceParser xml) {
        return "com.sevtinge.hyperceiler.prefs.PreferenceHeader".equals(xml.getName());
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

    private void processPreferenceHeader(String key, String summary) {
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
        Drawable icon = null;
        try {
            icon = requireContext().getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return icon;
    }

    private String getPackageName(String packageName) {
        String name = null;
        try {
            name = (String) requireContext().getPackageManager().getApplicationLabel(requireContext().getPackageManager().getApplicationInfo(packageName, 0));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return name;
    }

    public void isBirthday() {
        if (mBirthdayTipVisible) return;
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        mHeadtipBirthday.setVisible(currentMonth == Calendar.MAY && currentDay == 1);
        mBirthdayTipVisible = true;
    }

    public void isFuckCoolapkSDay() {
        if (mHyperCeilerTipVisible) return;
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        mHeadtipHyperCeiler.setVisible(currentMonth == Calendar.JULY && currentDay == 14);
        mHeadtipHyperCeiler.setTitle(R.string.headtip_tip_fuck_coolapk);
        mHyperCeilerTipVisible = true;
    }

    public void isLoggerAlive() {
        if (mNoticeTipVisible) return;
        if (!IS_LOGGER_ALIVE && !Objects.equals(BuildConfig.BUILD_TYPE, "release")) {
            mHeadtipNotice.setTitle(R.string.headtip_notice_dead_logger);
            mHeadtipNotice.setVisible(true);
            mNoticeTipVisible = true;
        }
    }

    public void checkWarnings() {
        if (mWarnTipVisible) return;
        boolean isOfficialRom = getIsOfficialRom();
        boolean isFullSupport = isFullSupport();
        boolean isSignPass = SignUtils.isSignCheckPass(requireContext());

        if (!isSignPass) {
            mHeadtipWarn.setTitle(R.string.headtip_warn_sign_verification_failed);
            mHeadtipWarn.setVisible(true);
        } else if (isOfficialRom) {
            mHeadtipWarn.setTitle(R.string.headtip_warn_not_offical_rom);
            mHeadtipWarn.setVisible(true);
        } else if (!isFullSupport) {
            mHeadtipWarn.setTitle(R.string.headtip_warn_unsupport_sysver);
            mHeadtipWarn.setVisible(true);
        }

        mWarnTipVisible = true;
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

        return hasRomAuthor || Objects.equals(host, "xiaomi.eu") || (isNotCustomBaseOs && isNotCustomHost);
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
