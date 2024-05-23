package com.sevtinge.hyperceiler.ui.navigator.page;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.CeilerTabActivity;
import com.sevtinge.hyperceiler.ui.page.utils.HeaderUtils;
import com.sevtinge.hyperceiler.ui.settings.adapter.HeaderAdapter;
import com.sevtinge.hyperceiler.ui.settings.adapter.PreferenceHeader;
import com.sevtinge.hyperceiler.ui.settings.adapter.ProxyHeaderViewAdapter;

import java.util.ArrayList;
import java.util.List;

import fan.appcompat.app.Fragment;
import fan.recyclerview.widget.RecyclerView;

public abstract class BasePageFragment extends Fragment {

    private RecyclerView mList;
    private static final String HEADERS_TAG = ":android:headers";

    protected List<PreferenceHeader> mHeaders;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (("android.intent.action.AIRPLANE_MODE".equals(action) || "android.intent.action.SIM_STATE_CHANGED".equals(action))) {
                ProxyHeaderViewAdapter adapter = getHeaderAdapter();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };

    protected abstract int getHeadersResourceId();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        getActivity().registerReceiver(mReceiver, intentFilter, Context.RECEIVER_EXPORTED);
    }

    @Override
    public void setThemeRes(int i) {

    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_search_fragment, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        buildAdapter();
        super.onViewCreated(view, savedInstanceState);
    }

    public void buildAdapter() {
        buildHeaders();
        if (mHeaders == null) {
            mHeaders = new ArrayList<>();
            HeaderAdapter baseAdapter = getHeaderAdapter() != null ? getHeaderAdapter().getBaseAdapter() : null;
            if (baseAdapter != null) {
                for (int i = 0; i < baseAdapter.getItemCount(); i++) {
                    mHeaders.add(baseAdapter.getItem(i));
                }
            }
        }
        ProxyHeaderViewAdapter headerAdapter = getHeaderAdapter();
        if (headerAdapter != null) {
            headerAdapter.pause();
        }
    }

    private RecyclerView getListView() {
        View view = getView();
        if (view == null) return null;
        mList = view.findViewById(R.id.scroll_headers);
        if (mList != null) return mList;
        throw new RuntimeException("Your content must have a ListView whose id attribute is 'android.R.id.list'");
    }


    public ProxyHeaderViewAdapter getHeaderAdapter() {
        if (getListView() == null) return null;
        return (ProxyHeaderViewAdapter) getListView().getAdapter();
    }


    public void buildHeaders() {
        CeilerTabActivity tabActivity = (CeilerTabActivity) getActivity();
        if (tabActivity != null) {
            List<PreferenceHeader> headers = new ArrayList<>();
            int headersResourceId = getHeadersResourceId();
            if (headersResourceId > 0) {
                HeaderUtils.loadHeadersFromResource(getContext(), headersResourceId, headers);
                tabActivity.updateHeaderList(headers);
            }
            mHeaders = headers;
        }
    }
}
