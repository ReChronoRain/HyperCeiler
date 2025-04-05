package com.sevtinge.hyperceiler.provision.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.provision.widget.PermissionItemView;

public class PermissionSettingsFragment extends BaseFragment {

    PermissionItemView mRootPermissionItem;
    PermissionItemView mNetworkPermissionItem;
    PermissionItemView mLspPermissionItem;

    @Override
    protected int getLayoutId() {
        return R.layout.provision_permission_layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRootPermissionItem = view.findViewById(R.id.root);
        mNetworkPermissionItem = view.findViewById(R.id.network);
        mLspPermissionItem = view.findViewById(R.id.lsp);

        mRootPermissionItem.setItemTitle("Root权限");
        mNetworkPermissionItem.setItemTitle("网络链接权限");
        mLspPermissionItem.setItemTitle("Xposed激活状态");

        mNetworkPermissionItem.setEnabled(false);
        mNetworkPermissionItem.setItemSelected(true);
    }
}
