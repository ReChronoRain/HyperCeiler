package com.sevtinge.hyperceiler.provision.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import com.sevtinge.hyperceiler.ui.R;

public abstract class BaseListFragment extends ListFragment {

    protected View mRootView;
    protected ViewGroup mCustomView;

    protected int getLayoutId() {
        return R.layout.provision_list_page_layout;
    }

    protected abstract int getCustomLayoutId();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(getLayoutId(), null);
        mCustomView = mRootView.findViewById(R.id.custom_view);
        if (getCustomLayoutId() != 0) {
            View customView = inflater.inflate(getCustomLayoutId(), null);
            mCustomView.addView(customView);
        }
        return mRootView;
    }

}
