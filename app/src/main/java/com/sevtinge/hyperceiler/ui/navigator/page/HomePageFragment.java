package com.sevtinge.hyperceiler.ui.navigator.page;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.ModData;
import com.sevtinge.hyperceiler.data.adapter.ModSearchAdapter;
import com.sevtinge.hyperceiler.ui.CeilerTabActivity;
import com.sevtinge.hyperceiler.ui.settings.adapter.HeaderAdapter;
import com.sevtinge.hyperceiler.ui.settings.adapter.ProxyHeaderViewAdapter;
import com.sevtinge.hyperceiler.ui.settings.notify.SettingsNotify;
import com.sevtinge.hyperceiler.ui.settings.notify.SettingsNotifyBuilder;
import com.sevtinge.hyperceiler.ui.settings.tips.TipsListener;
import com.sevtinge.hyperceiler.ui.settings.tips.TipsLocalModel;
import com.sevtinge.hyperceiler.ui.settings.tips.TipsUtils;
import com.sevtinge.hyperceiler.ui.settings.utils.SettingsFeatures;
import com.sevtinge.hyperceiler.ui.settings.utils.ThreadUtils;
import com.sevtinge.hyperceiler.utils.SettingLauncher;
import com.sevtinge.hyperceiler.utils.search.SearchModeHelper;

import fan.animation.Folme;
import fan.animation.ITouchStyle;
import fan.animation.base.AnimConfig;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.recyclerview.widget.RecyclerView;
import fan.view.SearchActionMode;

public class HomePageFragment extends BasePageFragment {

    String lastFilter;
    View mSearchView;
    TextView mSearchInputView;
    androidx.recyclerview.widget.RecyclerView mSearchResultView;
    ModSearchAdapter mSearchAdapter;

    private View mRemovableHintView;

    private NestedHeaderLayout mNestedHeaderView;
    private RecyclerView mListView;

    private HeaderAdapter mHeaderAdapter;
    private ProxyHeaderViewAdapter mProxyAdapter;

    private TipsListener mTipsListener;
    private volatile TipsLocalModel mTipsLocalModel;
    private volatile boolean mIsScrollEnableForListView = true;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0x11) {
                //在这里写需要刷新完成的代码
                mProxyAdapter.updateHeaderViewInfo();
                loadRemovableHint();
                removeMessages(0x11);
                sendEmptyMessageDelayed(0x11, 6000);//这里想几秒刷新一次就写几秒
            }
        }
    };

    @Override
    protected int getHeadersResourceId() {
        return R.xml.home_header;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (mTipsListener == null) {
            mTipsListener = new TipsListener(this);
        }
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_search_fragment, container, false);
        Log.e("onInflateView", view.getSourceLayoutResId() + "|||" + R.layout.home_search_fragment);
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
        mNestedHeaderView = view.findViewById(R.id.nestedheaderlayout);
        registerCoordinateScrollView(mNestedHeaderView);
        initSearchView(view);

        Message message = mHandler.obtainMessage(0x11);
        mHandler.sendMessageDelayed(message, 6000);
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
        mNestedHeaderView.setInSearchMode(true);
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
        if (SettingsFeatures.isSplitTablet(tabActivity)) {
            loadRemovableHint();
        }
    }

    public PackageManager getPackageManager() {
        return getActivity().getPackageManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRemovableHint();
    }

    public void loadRemovableHint() {
        SettingsNotifyBuilder builder = new SettingsNotifyBuilder();
        SettingsNotify notify = builder.build(requireContext());
        if (notify != null) {
            updateTips(
                    true,
                    builder.getTitle(),
                    builder.getSummary(),
                    builder.getIcon(),
                    0,
                    builder.getTextColor(),
                    builder.getBackground(),
                    builder.getOnClickListener());
        } else {
            ThreadUtils.postOnBackgroundThread(() -> {
                setTipsLocalModel(TipsUtils.query(getContext()));
                if (mTipsListener != null) {
                    mTipsListener.sendEmptyMessage(0);
                }
            });
        }
    }

    public void updateTips(boolean isEmpty, String title, String summary, Drawable icon, int arrowRightResId,int textColor,  Drawable background, View.OnClickListener listener) {
        Log.d("SettingsFragment", "updateTips title: " + title);
        if (isAdded()) {
            View removeHintView = mProxyAdapter.getRemoveHintView();
            FragmentActivity activity = getActivity();
            if (activity != null) {
                if (!isEmpty) {
                    if (removeHintView != null) {
                        mProxyAdapter.removeRemovableHintView(removeHintView);
                        setTipsLocalModel(null);
                        setRemovableHintView(null);
                    } else {
                        if (mRemovableHintView != null) {
                            mProxyAdapter.removeRemovableHintView(mRemovableHintView);
                            setRemovableHintView(null);
                        }
                    }
                } else {
                    if (removeHintView == null) {
                        removeHintView = activity.getLayoutInflater().inflate(R.layout.home_banner_layout, null);
                        removeHintView.setTag("removable_hint");
                        mProxyAdapter.addRemovableHintView(removeHintView);
                    }
                    setRemovableHintView(removeHintView);
                    Folme.useAt(removeHintView).touch().setScale(1.0f, new ITouchStyle.TouchType[0]).handleTouchOf(removeHintView, new AnimConfig[0]);
                    View contentView = removeHintView.findViewById(R.id.settings_banner_ly);
                    TextView titleView = removeHintView.findViewById(android.R.id.title);
                    TextView summaryView = removeHintView.findViewById(android.R.id.summary);
                    ImageView iconView = removeHintView.findViewById(android.R.id.icon);
                    ImageView arrowRightView = removeHintView.findViewById(R.id.arrow_right);

                    if (contentView != null && background != null) {
                        contentView.setBackground(background);
                    }

                    if (iconView != null && icon != null) {
                        iconView.setImageDrawable(icon);
                    } else {
                        ((View)iconView.getParent()).setVisibility(View.GONE);
                    }

                    if (titleView != null && !TextUtils.isEmpty(title)) {
                        titleView.setText(title);
                        if (textColor != 0) titleView.setTextColor(textColor);
                    } else {
                        titleView.setVisibility(View.GONE);
                    }

                    if (summaryView != null && !TextUtils.isEmpty(summary)) {
                        summaryView.setText(summary);
                        if (textColor != 0) summaryView.setTextColor(textColor);
                    } else {
                        summaryView.setVisibility(View.GONE);
                    }

                    if (arrowRightView != null) {
                        arrowRightView.setVisibility(View.GONE);
                    }
                    removeHintView.setOnClickListener(listener);
                }
            }
        } else {
            Log.e("SettingsFragment", "updateTips: Fragment SettingsFragment not attached to a context");
        }
    }


    public void setRemovableHintView(View view) {
        mRemovableHintView = view;
    }

    public TipsLocalModel getTipsLocalModel() {
        return mTipsLocalModel;
    }

    public void setTipsLocalModel(TipsLocalModel model) {
        mTipsLocalModel = model;
    }

}
