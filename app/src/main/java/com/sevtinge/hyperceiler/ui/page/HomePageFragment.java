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
import com.sevtinge.hyperceiler.widget.ListContainerView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.function.BiConsumer;

import fan.appcompat.app.ActionBar;
import fan.appcompat.app.Fragment;
import fan.navigator.NavigatorFragmentListener;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.springback.view.SpringBackLayout;

public class HomePageFragment extends DashboardFragment
        implements HomepageEntrance.EntranceState, ModSearchCallback.OnSearchListener,
        NavigatorFragmentListener, IFragmentChange {

    ListContainerView mContainerView;
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
        mContainerView = new ListContainerView(requireContext());
        mNestedHeaderLayout = mContainerView.getNestedHeader();
        mContainerView.addPrefsContainer(super.onCreateView(inflater, container, savedInstanceState));
        setOverlayMode();
        registerCoordinateScrollView(mNestedHeaderLayout);

        RecyclerView listView = getListView();
        View parent = (View) listView.getParent();
        if (parent instanceof SpringBackLayout) {
            parent.setEnabled(false);
            listView.setPaddingRelative(listView.getPaddingStart(), 0, listView.getPaddingEnd(), 0);
        }
        return mContainerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initSearchView();
    }

    private void initSearchView() {
        mContainerView.showHeaderView();
        mContainerView.setRefreshEnable(false);
        mSearchBar = mContainerView.getHeaderView();
        mSearchInputView = mSearchBar.findViewById(android.R.id.input);
        mSearchResultView = mContainerView.getRecyclerView();

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
            mContainerView.setRefreshEnable(false);
            mContainerView.showRecyclerView();
        }
    }

    @Override
    public void onDestroySearchMode(ActionMode mode) {
        //mInSearchMode = false;
        mNestedHeaderLayout.setInSearchMode(false);
        mContainerView.setRefreshEnable(false);
        mContainerView.showPrefsContainer();
    }

    @Override
    public void onSearchModeAnimStart(boolean z) {

    }

    @Override
    public void onSearchModeAnimStop(boolean z) {

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
    public void onEnter(ActionBar actionBar) {

    }

    @Override
    public void onLeave(ActionBar actionBar) {

    }
}
