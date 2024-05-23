package com.sevtinge.hyperceiler.ui.navigator.page;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.ModData;
import com.sevtinge.hyperceiler.data.adapter.ModSearchAdapter;
import com.sevtinge.hyperceiler.ui.CeilerTabActivity;
import com.sevtinge.hyperceiler.ui.settings.adapter.HeaderAdapter;
import com.sevtinge.hyperceiler.ui.settings.adapter.ProxyHeaderViewAdapter;
import com.sevtinge.hyperceiler.utils.SettingLauncher;
import com.sevtinge.hyperceiler.utils.search.SearchModeHelper;

import fan.recyclerview.widget.RecyclerView;
import fan.view.SearchActionMode;

public class HomePageFragment extends BasePageFragment {

    String lastFilter;
    View mSearchView;
    TextView mSearchInputView;
    androidx.recyclerview.widget.RecyclerView mSearchResultView;
    ModSearchAdapter mSearchAdapter;

    private RecyclerView mListView;

    private HeaderAdapter mHeaderAdapter;
    private ProxyHeaderViewAdapter mProxyAdapter;

    private volatile boolean mIsScrollEnableForListView = true;

    @Override
    protected int getHeadersResourceId() {
        return R.xml.home_header;
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_search_fragment, container, false);
        Log.e("onInflateView", String.valueOf(view.getSourceLayoutResId()) + "|||" + String.valueOf(R.layout.home_search_fragment));
        mListView = view.findViewById(R.id.scroll_headers);
        mListView.setFocusable(true);
        mListView.setFocusableInTouchMode(true);
        mListView.setItemAnimator(null);
        mListView.setItemViewCacheSize(-1);
        return view;
    }

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
        initSearchView(view);
    }

    private void initSearchView(View view) {
        mSearchView = view.findViewById(R.id.header_view);
        mSearchInputView = view.findViewById(android.R.id.input);
        mSearchResultView = view.findViewById(R.id.search_result);
        mSearchAdapter = new ModSearchAdapter();
        mSearchInputView.setHint(getResources().getString(R.string.search));

        mSearchResultView.setVisibility(View.GONE);
        mSearchResultView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mSearchResultView.setAdapter(mSearchAdapter);
        registerCoordinateScrollView(mSearchResultView);

        mSearchView.setOnClickListener(v -> startSearchMode());
        mSearchAdapter.setOnItemClickListener((v, ad) -> onSearchItemClickListener(ad));
    }

    private void onSearchItemClickListener(ModData ad) {
        Bundle args = new Bundle();
        args.putString(":settings:fragment_args_key", ad.key);
        new SettingLauncher(requireContext())
                .setArguments(args)
                .setDestination(ad.fragment)
                .setTitleRes(ad.catTitleResId)
                .launch();
    }

    private SearchActionMode startSearchMode() {
        return SearchModeHelper.startSearchMode(
                this,
                mSearchResultView,
                mListView,
                mSearchView,
                mListView,
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
        adapter.getFilter(requireContext()).filter(filter);
    }

    @Override
    public void buildAdapter() {
        super.buildAdapter();
        CeilerTabActivity tabActivity = (CeilerTabActivity) getActivity();
        mHeaderAdapter = new HeaderAdapter(tabActivity, mHeaders);
        mHeaderAdapter.setHasStableIds(true);
        mProxyAdapter = new ProxyHeaderViewAdapter(mHeaderAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity()) {
            @Override
            public boolean canScrollVertically() {
                return mIsScrollEnableForListView && super.canScrollVertically();
            }
        };
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        mListView.setLayoutManager(linearLayoutManager);
        mListView.setAdapter(mProxyAdapter);
        /*if (!mIsInActionMode) {
            mSearchResultListView.setVisibility(View.GONE);
            LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(getActivity());
            linearLayoutManager2.setOrientation(RecyclerView.VERTICAL);
            this.mSearchResultListView.setLayoutManager(linearLayoutManager2);
            this.mSearchResultListView.setAdapter(this.mSearchAdapter);
        } else if (!TextUtils.isEmpty(mSearchText)) {
            refreshSearchResult();
        }*/
        /*startSelectHeader();*/
        /*if (SettingsFeatures.isSplitTablet(miuiSettings)) {
            loadRemovableHint();
        }*/
    }

}
