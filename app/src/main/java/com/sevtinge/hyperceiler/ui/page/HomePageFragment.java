package com.sevtinge.hyperceiler.ui.page;

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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sevtinge.hyperceiler.home.HomePageHeaderHelper;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.common.utils.SettingsFeatures;
import com.sevtinge.hyperceiler.home.banner.BannerCallback;
import com.sevtinge.hyperceiler.home.FlowLayout;
import com.sevtinge.hyperceiler.home.Header;
import com.sevtinge.hyperceiler.home.IntentUtils;
import com.sevtinge.hyperceiler.home.OnCompleteCallBack;
import com.sevtinge.hyperceiler.home.SearchResultAdapter;
import com.sevtinge.hyperceiler.home.banner.BannerBean;
import com.sevtinge.hyperceiler.home.tips.HomePageTipHelper;
import com.sevtinge.hyperceiler.home.utils.HeaderManager;
import com.sevtinge.hyperceiler.home.utils.SearchHistorySPUtils;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.search.SearchHelper;
import com.sevtinge.hyperceiler.search.data.ModEntity;
import com.sevtinge.hyperceiler.ui.adapter.HeaderAdapter;
import com.sevtinge.hyperceiler.ui.adapter.ProxyHeaderViewAdapter;
import com.sevtinge.hyperceiler.utils.ThreadUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import fan.core.utils.MiuiBlurUtils;
import fan.internal.utils.ViewUtils;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.recyclerview.card.CardItemDecoration;
import fan.recyclerview.widget.RecyclerView;
import fan.view.ActionModeAnimationListener;
import fan.view.SearchActionMode;

public class HomePageFragment extends BasePreferenceFragment implements OnCompleteCallBack {

    private boolean isFirstCreated = true;


    private volatile boolean isClicking = false;

    private volatile boolean mIsInActionMode;
    private volatile boolean mIsActionModeDestroy;

    private volatile boolean mIsScrollEnableForListView = true;

    private volatile BannerBean mTipsLocalModel;
    private BannerCallback mBannerCallback;

    private List mClickedList = new LinkedList();

    private View mAnchorView;

    private NestedHeaderLayout mNestedHeaderLayout;

    private RecyclerView mListView;

    private String mSearchText;
    private String mSearchHistoryText;
    private List<String> mSearchHistoryLists;
    private List mSearchResultItems;
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
    // 自动轮播任务
    private final Runnable mTipsAutoTask = new Runnable() {
        @Override
        public void run() {
            // 仅仅刷新 Tip 的文字内容，不重绘整个 Banner 容器以节省性能
            refreshHeader();
            mTipsHandler.postDelayed(this, 30000); // 30秒换一次
        }
    };

    private Handler mMainHandler = new Handler();
    private TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
            String query = s.toString().trim();
            updateSearch(query, false);
            mClickedList.clear();
            if (!TextUtils.isEmpty(query)) {
                mSearchHistoryText = query;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String query = (s == null) ? "" : s.toString().trim();
            mSearchText = query; // 更新成员变量供 refreshSearchResult 使用
            if (TextUtils.isEmpty(query)) {
                if (mSearchResultListView != null) {
                    mSearchResultListView.setVisibility(View.GONE);
                }
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
            SearchActionMode searchActionMode = (SearchActionMode) mode;
            //searchActionMode.setFitWindowInsetsEnabled(false);

            searchActionMode.addAnimationListener(new ActionModeAnimationListener() {
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
                    if (z && mSearchHistoryLists != null && mSearchHistoryLists.size() > 0) {
                        setSearchHistoryVisiable(true);
                        setSearchMaskVisiable(false);
                    } else {
                        setSearchHistoryVisiable(false);
                    }
                    isClicking = false;
                }
            });
            searchActionMode.setAnchorView(mAnchorView);
            searchActionMode.setAnimateView(mListView);
            searchActionMode.setResultView(mSearchResultLinearLayout);
            searchActionMode.setAnchorApplyExtraPaddingByUser(true);

            mSearchInput = searchActionMode.getSearchInput();
            //mSearchInput.setOnTouchListener(new SettingsFragment$12$2(this));
            mSearchInput.setImeOptions(3);
            mSearchInput.addTextChangedListener(mTextWatcher);
            mSearchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    return false;
                }
            });
            /*if (SettingsFeatures.isSplitTablet(this.this$0.getContext())) {
                searchActionMode.addAnimationListener(new SettingsFragment$12$4(this));
            }*/
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            getSwitchManager().show();
            mIsInActionMode = false;
            mSearchInput.removeTextChangedListener(mTextWatcher);
            mSearchInput = null;
            mSearchResultListView.stopScroll();
            mSearchResultListView.setVisibility(View.GONE);
            mSearchLoadingView.setVisibility(View.GONE);
            setSearchHistoryVisiable(false);
            mListView.setVisibility(View.VISIBLE);
            mSearchText = null;
            if (mSearchHandler != null) {
                mSearchHandler.removeMessages(1);
            }
            if (SettingsFeatures.isSplitTablet(getContext())) {
                mIsActionModeDestroy = true;
            }
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mIsInActionMode = true;
            return false;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeRes(R.style.HomeNavigatorContentTheme);
        mSearchAdapter = new SearchResultAdapter();
        mSearchAdapter.setOnItemClickListener((view, ad) -> {
            processSearchHistory(mSearchHistoryText);
        });
        mSearchHistorySPUtils = new SearchHistorySPUtils(requireContext(), "search_history");

        if (mBannerCallback == null) {
            mBannerCallback = new BannerCallback(this);
        }
        isFirstCreated = true;
    }

    @Override
    protected int getHeadersResourceId() {
        return R.xml.settings_header;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHeader();
        // 开始自动轮播
        mTipsHandler.postDelayed(mTipsAutoTask, 30000);
    }

    @Override
    public void onPause() {
        super.onPause();
        // 停止轮播，防止内存泄漏
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
        //mSearchResultListView.setOnTouchListener(this);
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
                Log.e("SettingsFragment", "Force mListView to be gone");
            }
        } else {
            Log.e("SettingsFragment", "setSearchHistoryVisiable: mSearchListLayout is null");
        }
    }

    public void setSearchMaskVisiable(boolean visiable) {
        try {
            getActivity().getWindow().findViewById(fan.appcompat.R.id.search_mask).setVisibility(visiable ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Log.e("SettingsFragment", "setSearchMaskVisiable: ", e);
        }
    }

    public void processSearchHistory(String query) {
        if (!TextUtils.isEmpty(query)) {
            ThreadUtils.postOnBackgroundThread(() -> {
                List<String> searchHistory = mSearchHistorySPUtils.loadDataList("tagSearchHistory");
                if (searchHistory.size() != 0) {
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
            if (mSearchHistoryLists.size() != 0) {
                for (int size = mSearchHistoryLists.size() - 1; size >= 0; size--) {
                    final TextView textView = (TextView) inflater.inflate(R.layout.search_history_tv, mSearchHistoryFl, false);
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
        getActionBar().setTitle(R.string.navigation_home_title);
        setExtraHorizontalPaddingEnable(true);
        mNestedHeaderLayout = view.findViewById(R.id.nestedheaderlayout);
        registerCoordinateScrollView(mNestedHeaderLayout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAnchorView = view.findViewById(R.id.header_view);
        mAnchorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isClicking || mIsInActionMode) return;
                isClicking = true;
                ensureSearchHandler();
                if (mListView != null) {
                    mListView.setVisibility(View.GONE);
                    Log.d("SettingsFragment", "onClick: Set mListView to gone");
                }
                if (mSearchInput != null) {
                    mSearchInput.addTextChangedListener(mTextWatcher);
                }
                mSearchHandler.obtainMessage(2).sendToTarget();
                startActionMode(mSearchCallback);
            }
        });
        TextView textView = mAnchorView.findViewById(android.R.id.input);
        textView.setHint(com.sevtinge.hyperceiler.core.R.string.search);


        mNestedHeaderLayout = view.findViewById(R.id.nestedheaderlayout);
        mNestedHeaderLayout.setOverlayMode(MiuiBlurUtils.isEffectEnable(getContext()));
        mNestedHeaderLayout.setEnableBlur(MiuiBlurUtils.isEffectEnable(getContext()));
    }

    @Override
    public void updateHeaderList(List<Header> headers) {
        Set<String> headerRemoveList = PrefsUtils.getSharedStringSetPrefs(getContext(), "header_remove_list");
        for (Header header : headers) {

            String pkgName = (header.summary != null) ? header.summary.toString() : "";

            // 1. 检查是否安装
            boolean isInstalled = isAppInstalled(pkgName);

            // 2. 检查用户是否手动隐藏
            boolean isUserHidden = headerRemoveList.contains(pkgName);

            // 逻辑判断：只有 (已安装) 且 (用户没隐藏) 时，才标记为显示
            // 注意：如果应用未安装，无论用户勾没勾选，displayStatus 都应为 false
            header.displayStatus = isInstalled && !isUserHidden;

            // [可选] 标记未安装的 Header，以便在弹窗里特殊处理
            // header.isInstalled = isInstalled;
        }
    }

    private boolean isAppInstalled(String packageName) {
        // 如果 summary 根本不包含点（包名通常有点，如 com.android.xxx）
        // 或者 summary 为空，可以默认它是系统内置项，不进行安装检测
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

    /**
     * 统一刷新 Header 的方法
     */
    private void refreshHeader() {
        if (mProxyAdapter != null) {
            // 调用之前定义的“非静态”全家桶助手
            HomePageHeaderHelper.refreshAll(getContext(), mProxyAdapter, mBannerCallback);
        }
    }

    @Override
    public void buildAdapter() {
        super.buildAdapter();

        // 使用工具类获取过滤后的显示列表
        List<Header> displayHeaders = HeaderManager.getDisplayHeaders(getContext(), mHeaders);

        mHeaderAdapter = new HeaderAdapter(this, displayHeaders);
        mHeaderAdapter.setHasStableIds(true);
        mProxyAdapter = new ProxyHeaderViewAdapter(mHeaderAdapter);

        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        manager.setOrientation(LinearLayoutManager.VERTICAL);

        mListView.setLayoutManager(manager);
        mListView.setAdapter(mProxyAdapter);
        if (mListView.getItemDecorationCount() == 0) {
            mListView.addItemDecoration(new CardItemDecoration(getActivity()));
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
        } else {
            if (!TextUtils.isEmpty(mSearchText)) {
                refreshSearchResult();
            }
        }
        //startSelectHeader();

        // 不管列表怎么变，这一行代码能把 Banner 和 Tips 全找回来
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
                        // 执行数据库搜索 (当前在 HandlerThread 子线程)
                        // 使用方案 3 中定义的 SearchHelper.search 方法
                        List<ModEntity> results = SearchHelper.search(getContext(), query);

                        boolean isChinaLocale = isChina(getContext());// 保持你之前的 isChina 判断

                        mMainHandler.post(() -> {
                            if (mIsInActionMode && mSearchAdapter != null) {
                                mSearchAdapter.refresh(results, query, isChinaLocale);
                                mSearchLoadingView.setVisibility(View.GONE);
                                mSearchResultListView.setVisibility(View.VISIBLE);
                                mListView.setVisibility(View.GONE);
                                // 处理搜索历史显示逻辑
                                if (TextUtils.isEmpty(query) && mSearchHistoryLists != null && mSearchHistoryLists.size() > 0) {
                                    setSearchHistoryVisiable(true);
                                    setSearchMaskVisiable(false);
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
    }
}
