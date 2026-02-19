package com.sevtinge.hyperceiler.ui.page;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.SettingLauncher;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.home.CustomOrderManager;
import com.sevtinge.hyperceiler.home.Header;
import com.sevtinge.hyperceiler.home.SubSettingLauncher;
import com.sevtinge.hyperceiler.home.utils.HeaderUtils;
import com.sevtinge.hyperceiler.ui.HomePageActivity;
import com.sevtinge.hyperceiler.ui.SwitchManager;
import com.sevtinge.hyperceiler.ui.adapter.HeaderAdapter;
import com.sevtinge.hyperceiler.ui.adapter.ProxyHeaderViewAdapter;

import java.util.ArrayList;
import java.util.List;

import fan.appcompat.app.Fragment;
import fan.recyclerview.widget.RecyclerView;

public abstract class BasePreferenceFragment extends Fragment {

    protected List<Header> mHeaders;
    private RecyclerView mList;

    protected int getHeadersResourceId() {
        return 0;
    }

    protected PackageManager getPackageManager() {
        return requireActivity().getPackageManager();
    }

    private RecyclerView getListView() {
        View view = getView();
        if (view == null) {
            return null;
        }
        mList = view.findViewById(R.id.scroll_headers);
        if (mList != null) {
            return mList;
        }
        throw new RuntimeException("Your content must have a ListView whose id attribute is 'android.R.id.list'");
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

    protected ProxyHeaderViewAdapter getHeaderAdapter() {
        if (getListView() == null) {
            return null;
        }
        return (ProxyHeaderViewAdapter) getListView().getAdapter();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        buildAdapter();
        super.onViewCreated(view, savedInstanceState);
    }

    public void onHeaderClick(Header header, int i) {
        if (header.fragment != null) {
            new SubSettingLauncher(requireContext())
                .setDestination(header.fragment)
                .setTitleRes(header.titleRes)
                .setArguments(header.fragmentArguments)
                .setInflatedXml(header.inflatedXml)
                .setResultListener(null, 0)
                .setIsSecondLayerPage(true)
                .launch();
            return;
        }

        if (header.intent != null) {
            //startSplitActivityIfNeed(header.intent);
        }
    }

    public void updateHeaderList(List<Header> headers) {}

    public void buildHeaders() {
        List<Header> headers = new ArrayList<>();
        if (getHeadersResourceId() > 0) {
            HeaderUtils.loadHeadersFromResource(requireContext(), getHeadersResourceId(), headers);
            updateHeaderList(headers);
        }
        mHeaders = headers;
    }


    @Override
    public void onResume() {
        super.onResume();
        ProxyHeaderViewAdapter headerAdapter = getHeaderAdapter();
        if (headerAdapter != null) {
            headerAdapter.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ProxyHeaderViewAdapter headerAdapter = getHeaderAdapter();
        if (headerAdapter != null) {
            headerAdapter.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        ProxyHeaderViewAdapter headerAdapter = getHeaderAdapter();
        if (headerAdapter != null) {
            headerAdapter.stop();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ProxyHeaderViewAdapter headerAdapter = getHeaderAdapter();
        if (headerAdapter != null) {
            headerAdapter.start();
        }
    }


    public SwitchManager getSwitchManager() {
        if (getActivity() instanceof HomePageActivity) {
           return ((HomePageActivity) getActivity()).getSwitchManager();
        }
        return null;
    }
}
