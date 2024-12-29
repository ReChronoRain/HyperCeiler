package com.sevtinge.hyperceiler.ui.fragment.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;

import fan.appcompat.app.Fragment;

public class DetailFragment extends Fragment {

    String mFragmentName;

    View mEmptyView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeRes(R.style.NavigatorSecondaryContentTheme);
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
        mEmptyView = view.findViewById(R.id.empty);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
    }

    @Override
    public void onUpdateArguments(Bundle args) {
        super.onUpdateArguments(args);
        if (args != null) {
            mFragmentName = args.getString("FragmentName");
            //int fragmentResId = args.getInt(":settings:fragment_resId");

            if (!TextUtils.isEmpty(mFragmentName)) {
                getChildFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_content, androidx.fragment.app.Fragment.instantiate(requireContext(), mFragmentName, args))
                        .commit();

                mEmptyView.setVisibility(View.INVISIBLE);

            }
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }
}
