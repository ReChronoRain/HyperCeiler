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
package com.sevtinge.hyperceiler.ui.sub;

import android.content.Intent;

import com.sevtinge.hyperceiler.common.callback.IAppSelectCallback;
import com.sevtinge.hyperceiler.dashboard.SettingsActivity;

public class SubPickerActivity extends SettingsActivity {
    AppPickerFragment mAppSelectFragment = new AppPickerFragment();

    @Override
    public void initCreate() {
        mAppSelectFragment.setAppSelectCallback(new IAppSelectCallback() {
            @Override
            public void sendMsgToActivity(byte[] appIcon, String appName, String appPackageName, String appVersion, String appActivityName) {
                Intent intent = new Intent();
                intent.putExtra("appIcon", appIcon);
                intent.putExtra("appName", appName);
                intent.putExtra("appPackageName", appPackageName);
                intent.putExtra("appVersion", appVersion);
                intent.putExtra("appActivityName", appActivityName);
                setResult(1, intent);
            }
            @Override
            public String getMsgFromActivity(String s) {
                return null;
            }
        });
        setFragment(mAppSelectFragment);
    }
}
