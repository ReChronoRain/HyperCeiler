package com.sevtinge.hyperceiler.provision.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {

    protected Context mContext;
    protected View mRootView;

    protected abstract int getLayoutId();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return initFragment(inflater, container);
    }

    private View initFragment(LayoutInflater inflater, ViewGroup container) {
        if (mRootView == null) {
            mRootView = inflater.inflate(getLayoutId(), null);
        }
        return mRootView;
    }

    public void setResult(int resultCode) {
        requireActivity().setResult(resultCode);
    }

    public void startActivity(Intent intent) {
        requireActivity().startActivity(intent);
    }

    public void finish() {
        requireActivity().finish();
    }
}
