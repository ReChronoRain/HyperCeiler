/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
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
