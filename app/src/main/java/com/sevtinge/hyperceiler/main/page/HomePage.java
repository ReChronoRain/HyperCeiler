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
package com.sevtinge.hyperceiler.main.page;

import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.callback.ModSearchCallback;
import com.sevtinge.hyperceiler.common.model.adapter.ModSearchAdapter;
import com.sevtinge.hyperceiler.common.model.data.ModData;
import com.sevtinge.hyperceiler.common.utils.SettingLauncherHelper;
import com.sevtinge.hyperceiler.dashboard.SubSettings;
import com.sevtinge.hyperceiler.main.fragment.ContentFragment.IFragmentChange;
import com.sevtinge.hyperceiler.main.page.main.HomeFragment;
import com.sevtinge.hyperceiler.main.fragment.PageFragment;

import fan.appcompat.app.ActionBar;
import fan.preference.PreferenceFragment;

public class HomePage extends PageFragment
    implements IFragmentChange, ModSearchCallback.OnSearchListener,
    SearchView.OnQueryTextListener {

    private View mSearchBar;
    private TextView mSearchInputView;
    private RecyclerView mSearchResultView;
    private ModSearchAdapter mSearchAdapter;
    private ModSearchCallback mSearchCallBack;

    @Override
    public PreferenceFragment getPreferenceFragment() {
        return new HomeFragment();
    }

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
        initSearchView(view);
    }

    private void initSearchView(View view) {
        mSearchBar = view.findViewById(R.id.search_bar);
        mSearchInputView = view.findViewById(android.R.id.input);
        mSearchResultView = view.findViewById(R.id.search_result);

        if (mSearchAdapter == null) {
            mSearchAdapter = new ModSearchAdapter();
            mSearchResultView.setLayoutManager(new LinearLayoutManager(requireContext()));
            mSearchResultView.setAdapter(mSearchAdapter);
            mSearchAdapter.setOnItemClickListener((v, ad) -> onSearchItemClickListener(ad));
        }
        mSearchInputView.setHint(getResources().getString(com.sevtinge.hyperceiler.core.R.string.search));

        mSearchBar.setOnClickListener(v -> onTextSearch());
    }

    void findMod(String filter) {
        mSearchResultView.setVisibility(filter.isEmpty() ? View.GONE : View.VISIBLE);
        ModSearchAdapter adapter = (ModSearchAdapter) mSearchResultView.getAdapter();
        if (adapter == null) return;
        adapter.getFilter(requireContext()).filter(filter);
    }

    public void onTextSearch() {
        if (mSearchBar != null) {
            onSearchRequest();
        }
    }

    protected void onSearchRequest() {
        if (mSearchCallBack == null) {
            mSearchCallBack = new ModSearchCallback(this, this);
        }
        mSearchCallBack.setup(mSearchBar, mRootView);
        startSearchMode();
    }

    private void startSearchMode() {
        if (mSearchCallBack != null) {
            startActionMode(mSearchCallBack);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        findMod(newText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }


    @Override
    public void onCreateSearchMode(ActionMode mode, Menu menu) {
        if (isAdded()) {
            mNestedHeaderLayout.getScrollableView().setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDestroySearchMode(ActionMode mode) {
        mNestedHeaderLayout.getScrollableView().setVisibility(View.VISIBLE);
    }

    private void onSearchItemClickListener(ModData ad) {
        if (ad == null) return;
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

    @Override
    public void onEnter(ActionBar actionBar) {}

    @Override
    public void onLeave(ActionBar actionBar) {}
}
