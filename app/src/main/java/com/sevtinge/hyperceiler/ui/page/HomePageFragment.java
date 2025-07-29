package com.sevtinge.hyperceiler.ui.page;

import android.content.pm.ApplicationInfo;
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
import androidx.preference.PreferenceCategory;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.common.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.common.utils.SettingLauncherHelper;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.dashboard.SubSettings;
import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.common.callback.ModSearchCallback;
import com.sevtinge.hyperceiler.common.model.data.ModData;
import com.sevtinge.hyperceiler.common.model.adapter.ModSearchAdapter;
import com.sevtinge.hyperceiler.ui.page.settings.helper.HomepageEntrance;
import com.sevtinge.hyperceiler.ui.ContentFragment.IFragmentChange;
import com.sevtinge.hyperceiler.common.utils.MainActivityContextHelper;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.banner.HomePageBannerHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
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

    PreferenceCategory mHeadtipGround;
    MainActivityContextHelper mainActivityContextHelper;
    private static final String TAG = "MainFragment";
    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_main;
    }

    @Override
    public void initPrefs() {
        HomepageEntrance.setEntranceStateListen(this);
        setPreference();
        mHeadtipGround = findPreference("prefs_key_headtip_ground");
        mainActivityContextHelper = new MainActivityContextHelper(requireContext());
        HomePageBannerHelper.init(requireContext(), mHeadtipGround);
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
        return "com.sevtinge.hyperceiler.common.prefs.PreferenceHeader".equals(xml.getName());
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
            PreferenceHeader header = findPreference(key);
            if (header != null) {
                setIconAndTitle(header, summary);
            }
        }
    }

    private void setIconAndTitle(PreferenceHeader header, String packageName) {
        try {
            PackageManager pm = requireContext().getPackageManager();
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
            Drawable icon = applicationInfo.loadIcon(pm);
            CharSequence name = applicationInfo.loadLabel(pm);
            header.setIcon(icon);
            if (!packageName.equals("android")) {
                header.setTitle(name);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
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
