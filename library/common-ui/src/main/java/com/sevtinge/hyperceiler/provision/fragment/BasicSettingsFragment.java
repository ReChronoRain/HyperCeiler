package com.sevtinge.hyperceiler.provision.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.ui.R;

import fan.preference.PreferenceFragment;

public class BasicSettingsFragment extends PreferenceFragment {

    private boolean mIsScrolledBottom = false;


    private RecyclerView mRecyclerView;


    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.provision_basic_settings, rootKey);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        mRecyclerView = getListView();
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!mRecyclerView.canScrollVertically(1)) {
                    mIsScrolledBottom = true;
                    adjustNextView();
                }
            }
        });
    }

    public void adjustNextView() {
        /*if (mNextView != null && mNextView instanceof TextView) {
            if (mIsScrolledBottom) {
                ((TextView) mNextView).setText(R.string.next);
            } else {
                ((TextView) mNextView).setText(R.string.more);
            }
        }*/
    }

}
