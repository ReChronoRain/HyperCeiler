package com.sevtinge.hyperceiler.provision.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import fan.bottomsheet.BottomSheetBehavior;
import fan.bottomsheet.BottomSheetModal;

public class WebFragment extends Fragment {

    public BottomSheetModal mBottomSheetModal;

    public void setBottomSheetModal(BottomSheetModal bottomSheetModal) {
        mBottomSheetModal = bottomSheetModal;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return new FrameLayout(container.getContext());
    }
}
