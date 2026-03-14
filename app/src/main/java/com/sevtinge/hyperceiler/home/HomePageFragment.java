package com.sevtinge.hyperceiler.home;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.home.adapter.HeaderAdapter;
import com.sevtinge.hyperceiler.home.adapter.ProxyHeaderViewAdapter;
import com.sevtinge.hyperceiler.home.banner.BannerCallback;
import com.sevtinge.hyperceiler.home.base.BasePreferenceFragment;
import com.sevtinge.hyperceiler.home.order.OnCompleteCallBack;
import com.sevtinge.hyperceiler.home.tips.HomePageTipHelper;
import com.sevtinge.hyperceiler.home.utils.HeaderManager;
import com.sevtinge.hyperceiler.home.utils.IntentUtils;
import com.sevtinge.hyperceiler.home.utils.SearchHistorySPUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.LogManager;
import com.sevtinge.hyperceiler.search.SearchHelper;
import com.sevtinge.hyperceiler.search.SearchResultAdapter;
import com.sevtinge.hyperceiler.search.data.ModEntity;
import com.sevtinge.hyperceiler.search.widget.FlowLayout;
import com.sevtinge.hyperceiler.utils.DialogHelper;
import com.sevtinge.hyperceiler.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import fan.core.utils.MiuiBlurUtils;
import fan.internal.utils.ViewUtils;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.recyclerview.card.CardItemDecoration;
import fan.recyclerview.widget.RecyclerView;
import fan.theme.token.ContainerToken;
import fan.view.ActionModeAnimationListener;
import fan.view.SearchActionMode;

public class HomePageFragment extends BasePreferenceFragment implements OnCompleteCallBack {

    public static int getHomeHeadersResourceId() {
        return R.xml.settings_header;
    }

    private volatile boolean isClicking = false;
    private volatile boolean mIsInActionMode;
    private volatile boolean mIsScrollEnableForListView = true;

    private BannerCallback mBannerCallback;

    private View mAnchorView;

    private NestedHeaderLayout mNestedHeaderLayout;

    private RecyclerView mListView;

    private String mSearchText;
    private String mSearchHistoryText;
    private List<String> mSearchHistoryLists = new ArrayList<>();
    private SearchResultAdapter mSearchAdapter;

    private SearchHandler mSearchHandler;

    private EditText mSearchInput;

    private View mSearchLoadingView;

    private FlowLayout mSearchHistoryFl;
    private RecyclerView mSearchResultListView;
    private NestedScrollView mSearchListLayout;

    private LinearLayout mSearchResultLinearLayout;

    private HeaderAdapter mHeaderAdapter;
    private ProxyHeaderViewAdapter mProxyAdapter;

    private SearchHistorySPUtils mSearchHistorySPUtils;
    private HandlerThread mSearchThread;

    private final Handler mTipsHandler = new Handler(Looper.getMainLooper());
    private final Runnable mTipsAutoTask = new Runnable() {
        @Override
        public void run() {
            Context context = getContext();
            if (context != null) {
                HomePageTipHelper.refreshCurrentTip(context);
            }
            mTipsHandler.postDelayed(this, 30000);
        }
    };

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
            String query = s.toString().trim();
            updateSearch(query, false);
            if (!TextUtils.isEmpty(query)) {
                mSearchHistoryText = query;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String query = (s == null) ? "" : s.toString().trim();
            mSearchText = query;
            if (TextUtils.isEmpty(query)) {
                if (mSearchResultListView != null) {
                    mSearchResultListView.setVisibility(View.GONE);
                }
                setSearchHistoryVisiable(mSearchHistoryLists != null && !mSearchHistoryLists.isEmpty());
            } else {
                if (mSearchResultListView != null) {
                    mSearchResultListView.setVisibility(View.VISIBLE);
                }
                if (mListView != null) {
                    mListView.setVisibility(View.GONE);
                }
            }
           refreshSearchResult();
        }
    };

    private SearchActionMode.Callback mSearchCallback = new SearchActionMode.Callback() {
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getSwitchManager().hide();
            SearchActionMode searchActionMode = getSearchActionMode((SearchActionMode) mode);
            searchActionMode.setSearchMaskAlwaysHidden(true);
            searchActionMode.setAnchorView(mAnchorView);
            searchActionMode.setAnimateView(mListView);
            searchActionMode.setResultView(mSearchResultLinearLayout);
            searchActionMode.setAnchorApplyExtraPaddingByUser(true);

            mSearchInput = searchActionMode.getSearchInput();
            mSearchInput.setImeOptions(3);
            mSearchInput.addTextChangedListener(mTextWatcher);
            mSearchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    return true;
                }
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    processSearchHistory(mSearchHistoryText);
                    return true;
                }
                return false;
            });
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            getSwitchManager().show();
            mIsInActionMode = false;
            if (mSearchInput != null) {
                mSearchInput.removeTextChangedListener(mTextWatcher);
            }
            mSearchInput = null;
            if (mSearchResultListView != null) {
                mSearchResultListView.stopScroll();
                mSearchResultListView.setVisibility(View.GONE);
            }
            if (mSearchLoadingView != null) {
                mSearchLoadingView.setVisibility(View.GONE);
            }
            setSearchHistoryVisiable(false);
            if (mListView != null) {
                mListView.setVisibility(View.VISIBLE);
            }
            mSearchText = null;
            mSearchAdapter.refresh(null, "", isChina(requireContext()));
            if (mSearchHandler != null) {
                mSearchHandler.removeMessages(1);
            }
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mIsInActionMode = true;
            return false;
        }
    };

    @NonNull
    private SearchActionMode getSearchActionMode(SearchActionMode mode) {
        mode.addAnimationListener(new ActionModeAnimationListener() {

            @Override
            public void onStart(boolean z) {
                mIsScrollEnableForListView = false;
            }

            @Override
            public void onUpdate(boolean z, float f) {
                if (mSearchListLayout != null) {
                    mSearchListLayout.setAlpha(0.0f);
                }
            }

            @Override
            public void onStop(boolean z) {
                mIsScrollEnableForListView = !z;
                if (mSearchListLayout != null) {
                    mSearchListLayout.setAlpha(1.0f);
                }
                if (z && mSearchHistoryLists != null && !mSearchHistoryLists.isEmpty()) {
                    setSearchHistoryVisiable(true);
                } else {
                    setSearchHistoryVisiable(false);
                }
                isClicking = false;
            }
        });
        return mode;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeRes(R.style.HomeNavigatorContentTheme);

        mSearchHistorySPUtils = new SearchHistorySPUtils(requireContext(), "search_history");
        mSearchAdapter = new SearchResultAdapter();
        mSearchAdapter.setOnItemClickListener((view, ad) -> {
            processSearchHistory(mSearchHistoryText);
        });

        if (mBannerCallback == null) {
            mBannerCallback = new BannerCallback(this);
        }

        LogManager.onHealthCheckDone(() -> mMainHandler.post(this::refreshHeader));
    }

    @Override
    protected int getHeadersResourceId() {
        return getHomeHeadersResourceId();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTipsHandler.postDelayed(mTipsAutoTask, 30000);
    }

    @Override
    public void onPause() {
        super.onPause();
        mTipsHandler.removeCallbacks(mTipsAutoTask);
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.home_settings, container, false);
        mSearchResultLinearLayout = v.findViewById(R.id.search_result_ll);
        mListView = v.findViewById(R.id.scroll_headers);
        mListView.setFocusable(true);
        mListView.setFocusableInTouchMode(true);
        mListView.setItemAnimator(null);
        mListView.setItemViewCacheSize(-1);

        mSearchResultListView = v.findViewById(R.id.search_result);
        mSearchResultListView.setFocusable(true);
        mSearchResultListView.setFocusableInTouchMode(true);
        mSearchResultListView.setItemAnimator(null);
        mSearchLoadingView = v.findViewById(R.id.search_loading);
        mSearchListLayout = v.findViewById(R.id.search_history);
        mSearchHistoryFl = v.findViewById(R.id.search_history_fl);

        TextView searchHistoryClearTv = v.findViewById(R.id.search_history_clear_tv);
        searchHistoryClearTv.setOnClickListener(view -> {
            mSearchHistorySPUtils.removeDateList("tagSearchHistory");
            mSearchHistoryLists.clear();
            mSearchHistoryFl.removeAllViews();
            setSearchHistoryVisiable(false);
        });
        initSearchHistoryView();
        return v;
    }


    public void updateSearch(String query, boolean z) {
        if (!Objects.equals(mSearchText, query) || z) {
            ensureSearchHandler();
            mSearchHandler.removeMessages(1);
            mSearchHandler.obtainMessage(1, query).sendToTarget();
            if (TextUtils.isEmpty(mSearchText)) {
                mSearchLoadingView.setVisibility(View.VISIBLE);
            }
            mSearchText = query;
        }
    }

    public void setSearchHistoryVisiable(boolean visiable) {
        if (mSearchListLayout != null) {
            mSearchListLayout.setVisibility(visiable ? View.VISIBLE : View.GONE);
            if (visiable && mListView != null) {
                mListView.setVisibility(View.GONE);
            }
        }
    }

    public void processSearchHistory(String query) {
        if (!TextUtils.isEmpty(query)) {
            ThreadUtils.postOnBackgroundThread(() -> {
                List<String> searchHistory = mSearchHistorySPUtils.loadDataList("tagSearchHistory");
                if (!searchHistory.isEmpty()) {
                    mSearchHistoryLists.clear();
                    mSearchHistoryLists.addAll(searchHistory);
                }
                if (!mSearchHistoryLists.contains(query)) {
                    if (mSearchHistoryLists.size() >= 15) {
                        mSearchHistoryLists.remove(0);
                        mSearchHistoryLists.add(mSearchHistoryLists.size(), query);
                    } else {
                        mSearchHistoryLists.add(query);
                    }
                } else {
                    int i = -1;
                    for (int i2 = 0; i2 < mSearchHistoryLists.size(); i2++) {
                        if (query.equals(mSearchHistoryLists.get(i2))) {
                            i = i2;
                        }
                    }
                    mSearchHistoryLists.remove(i);
                    mSearchHistoryLists.add(mSearchHistoryLists.size(), query);
                }
                mSearchHistorySPUtils.saveDataList("tagSearchHistory", mSearchHistoryLists);
                mMainHandler.post(() -> initSearchHistoryView());
            });
        }
    }

    private void initSearchHistoryView() {
        try {
            mSearchHistoryFl.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            mSearchHistoryLists = mSearchHistorySPUtils.loadDataList("tagSearchHistory");
            if (!mSearchHistoryLists.isEmpty()) {
                for (int size = mSearchHistoryLists.size() - 1; size >= 0; size--) {
                    TextView textView = (TextView) inflater.inflate(R.layout.search_history_tv, mSearchHistoryFl, false);
                    String str = mSearchHistoryLists.get(size);
                    if (textView != null) {
                        textView.setText(str);
                        textView.setOnClickListener(view -> {
                            if (mSearchInput != null) {
                                mSearchInput.setText(textView.getText());
                                mSearchInput.setSelection(textView.length());
                                mSearchListLayout.setVisibility(View.GONE);
                            }
                        });
                        mSearchHistoryFl.addView(textView);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SettingsFragment", "initSearchHistoryView fail: ", e);
        }
    }

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
        getActionBar().setTitle(com.sevtinge.hyperceiler.core.R.string.app_name);
        setExtraHorizontalPaddingEnable(true);
        mNestedHeaderLayout = view.findViewById(R.id.nestedheaderlayout);
        registerCoordinateScrollView(mNestedHeaderLayout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAnchorView = view.findViewById(R.id.header_view);
        mAnchorView.setOnClickListener(v -> {
            if (isClicking || mIsInActionMode) return;
            isClicking = true;
            ensureSearchHandler();
            if (mListView != null) {
                mListView.setVisibility(View.GONE);
            }
            startActionMode(mSearchCallback);
        });
        TextView textView = mAnchorView.findViewById(android.R.id.input);
        textView.setHint(com.sevtinge.hyperceiler.core.R.string.search);


        mNestedHeaderLayout = view.findViewById(R.id.nestedheaderlayout);
        mNestedHeaderLayout.setOverlayMode(MiuiBlurUtils.isEffectEnable(getContext()));
        mNestedHeaderLayout.setEnableBlur(MiuiBlurUtils.isEffectEnable(getContext()));
    }

    @Override
    public void updateHeaderList(List<Header> headers) {
        Set<String> headerRemoveList = PrefsBridge.getStringSet("header_remove_list");
        for (Header header : headers) {

            String pkgName = (header.summary != null) ? header.summary.toString() : "";

            boolean isInstalled = isAppInstalled(pkgName);
            boolean isUserHidden = headerRemoveList.contains(pkgName);
            header.displayStatus = isInstalled && !isUserHidden;
        }
    }

    private boolean isAppInstalled(String packageName) {
        if (TextUtils.isEmpty(packageName) || !packageName.contains(".")) {
            return true;
        }
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void refreshHeader() {
        if (mProxyAdapter != null) {
            HomePageHeaderHelper.refreshAll(getContext(), mProxyAdapter, mBannerCallback);
        }
    }

    @Override
    public void buildAdapter() {
        super.buildAdapter();

        List<Header> displayHeaders = HeaderManager.getDisplayHeaders(getContext(), mHeaders);

        mHeaderAdapter = new HeaderAdapter(this, displayHeaders);
        mHeaderAdapter.setHasStableIds(true);
        mProxyAdapter = new ProxyHeaderViewAdapter(mHeaderAdapter);

        LinearLayoutManager manager = new LinearLayoutManager(getContext()) {
            @Override
            public boolean canScrollVertically() {
                return mIsScrollEnableForListView && super.canScrollVertically();
            }
        };
        manager.setOrientation(LinearLayoutManager.VERTICAL);

        mListView.setLayoutManager(manager);
        mListView.setAdapter(mProxyAdapter);
        if (mListView.getItemDecorationCount() == 0) {
            CardItemDecoration decoration = new CardItemDecoration(getActivity());
            decoration.setCardMarginTop(getContext().getResources().getDimensionPixelSize(R.dimen.settings_banner_ly_padding_top_and_bottom));
            mListView.addItemDecoration(decoration);
        }
        mProxyAdapter.updateGroupInfo();
        if (!mIsInActionMode) {
            mSearchResultListView.setVisibility(View.GONE);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            mSearchResultListView.setLayoutManager(layoutManager);
            mSearchResultListView.setAdapter(mSearchAdapter);
            if (mSearchResultListView.getItemDecorationCount() == 0) {
                mSearchResultListView.addItemDecoration(new CardItemDecoration(getActivity()));
            }
            mSearchAdapter.updateGroupInfo();
        } else if (!TextUtils.isEmpty(mSearchText)) {
            refreshSearchResult();
        }
        refreshHeader();
    }

    public void refreshSearchResult() {
        if (!TextUtils.isEmpty(mSearchText)) {
            ensureSearchHandler();
            mSearchHandler.removeMessages(1);
            mSearchHandler.obtainMessage(1, mSearchText).sendToTarget();
        }
    }

    public void ensureSearchHandler() {
        if (mSearchThread == null) {
            mSearchThread = new HandlerThread("SettingsFragment-Search");
            mSearchThread.start();
            mSearchHandler = new SearchHandler(mSearchThread.getLooper());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.function_setting) {
            IntentUtils.goToCustomOrderDialog(requireActivity(), this);
        } else if (itemId == R.id.quick_restart) {
            DialogHelper.showRestartDialog(requireContext());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void refresh() {
        buildAdapter();
    }

    @Override
    public void onDismiss() {

    }

    private class SearchHandler extends Handler {
        public SearchHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            try {
                if (msg.what == 1) {
                    String query = (String) msg.obj;
                    if (mIsInActionMode && getContext() != null) {
                        mMainHandler.post(() -> {
                            if (mIsInActionMode) {
                                mSearchLoadingView.setVisibility(View.VISIBLE);
                            }
                        });
                        List<ModEntity> results = SearchHelper.search(getContext(), query);

                        boolean isChinaLocale = isChina(getContext());

                        mMainHandler.post(() -> {
                            if (mIsInActionMode && mSearchAdapter != null) {
                                mSearchAdapter.refresh(results, query, isChinaLocale);
                                mSearchLoadingView.setVisibility(View.GONE);
                                mListView.setVisibility(View.GONE);
                                if (TextUtils.isEmpty(query) && mSearchHistoryLists != null && mSearchHistoryLists.size() > 0) {
                                    setSearchHistoryVisiable(true);
                                } else {
                                    setSearchHistoryVisiable(false);
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("SettingsFragment", "handleMessage Exception " + e);
            }

        }
    }

    public boolean isChina(Context context) {
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        return locale.getLanguage().contains(new Locale("zh").getLanguage());
    }

    @Override
    public void onContentInsetChanged(Rect rect) {
        super.onContentInsetChanged(rect);
        if (mListView != null) {
            ViewUtils.RelativePadding relativePadding = new ViewUtils.RelativePadding(mListView);
            boolean isLayoutRtl = ViewUtils.isLayoutRtl(mListView);
            relativePadding.start += isLayoutRtl ? rect.right : rect.left;
            relativePadding.end += isLayoutRtl ? rect.left : rect.right;
            relativePadding.bottom = rect.top;
            relativePadding.applyToView(mListView);
        }
        mSearchResultLinearLayout.setPadding(
            mSearchResultLinearLayout.getPaddingLeft(),
            rect.top,
            mSearchResultLinearLayout.getPaddingRight(),
            mSearchResultLinearLayout.getPaddingBottom()
        );
    }

    @Override
    public void onExtraPaddingChanged(int extraHorizontalPadding) {
        super.onExtraPaddingChanged(extraHorizontalPadding);
        int margin = (int) (extraHorizontalPadding + (ContainerToken.PADDING_BASE_DP * 3 * getResources().getDisplayMetrics().density));
        setExtraPadding(mListView, margin);
        setExtraPadding(mSearchResultListView, margin);
        if (mAnchorView != null) {
            FrameLayout frameLayout = mAnchorView.findViewById(fan.appcompat.R.id.search_mode_stub);
            if (frameLayout != null) {
                frameLayout.setPaddingRelative(margin, frameLayout.getPaddingTop(), margin, frameLayout.getPaddingBottom());
            }
        }
        if (mSearchListLayout != null) {
            mSearchListLayout.setPaddingRelative(margin, mSearchListLayout.getPaddingTop(), margin, mSearchListLayout.getPaddingBottom());
        }
    }

    private void setExtraPadding(RecyclerView recyclerView, int margin) {
        if (recyclerView == null || recyclerView.getItemDecorationCount() == 0) {
            return;
        }
        RecyclerView.ItemDecoration itemDecoration = recyclerView.getItemDecorationAt(0);
        if (itemDecoration instanceof CardItemDecoration cardItemDecoration) {
            cardItemDecoration.setCardMarginStart(margin);
            cardItemDecoration.setCardMarginEnd(margin);
            if (recyclerView.getAdapter() != null) {
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTipsHandler.removeCallbacksAndMessages(null);
        if (mSearchHandler != null) {
            mSearchHandler.removeCallbacksAndMessages(null);
        }
        if (mSearchThread != null) {
            mSearchThread.quitSafely();
            mSearchThread = null;
            mSearchHandler = null;
        }
    }

}
