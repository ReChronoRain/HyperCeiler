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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemsettings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.ui.MainActivity;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import moralnorm.os.Build;

public class HyperCeilerSettings extends BaseHook {

    private int settingsIconResId;

    private Class<?> mPreferenceHeader;

    int mIconModeInt = mPrefsMap.getStringAsInt("settings_icon_mode", 0);

    @Override
    public void init() {
        addIconResource();

        Class<?> mMiuiSettings = findClassIfExists("com.android.settings.MiuiSettings");

        findAndHookMethod(mMiuiSettings, "updateHeaderList", List.class, new MethodHook() {
            @Override
            @SuppressLint("DiscouragedApi")
            protected void after(MethodHookParam param) throws Throwable {
                if (param.args[0] == null) return;

                Context mContext = ((Activity) param.thisObject).getBaseContext();
                int opt = Integer.parseInt(mPrefsMap.getString("settings_icon", "0"));
                if (opt == 0) return;

                Resources modRes = getModuleRes(mContext);
                mPreferenceHeader = findClassIfExists("com.android.settingslib.miuisettings.preference.PreferenceActivity$Header");
                if (mPreferenceHeader == null) return;

                Intent mIntent = new Intent();
                mIntent.putExtra("isDisplayHomeAsUpEnabled", true);
                mIntent.setClassName(ProjectApi.mAppModulePkg, MainActivity.class.getCanonicalName());

                Object header = XposedHelpers.newInstance(mPreferenceHeader);
                XposedHelpers.setLongField(header, "id", 666);
                XposedHelpers.setObjectField(header, "intent", mIntent);
                XposedHelpers.setIntField(header, "iconRes", settingsIconResId);
                XposedHelpers.setObjectField(header, "title", modRes.getString(R.string.app_name));

                Bundle bundle = new Bundle();
                ArrayList<UserHandle> users = new ArrayList<>();
                users.add((UserHandle) XposedHelpers.newInstance(UserHandle.class, 0));
                bundle.putParcelableArrayList("header_user", users);
                XposedHelpers.setObjectField(header, "extras", bundle);

                int themes = mContext.getResources().getIdentifier("launcher_settings", "id", mContext.getPackageName());
                int special = mContext.getResources().getIdentifier("other_special_feature_settings", "id", mContext.getPackageName());
                int timer = mContext.getResources().getIdentifier("app_timer", "id", mContext.getPackageName());

                List<Object> headers = (List<Object>) param.args[0];
                int position = 0;
                for (Object head : headers) {
                    position++;
                    long id = XposedHelpers.getLongField(head, "id");
                    if (opt == 1 && id == -1) {
                        headers.add(position - 1, header);
                    } else if (opt == 2 && id == themes) {
                        headers.add(position, header);
                    } else if (opt == 3 && id == (Integer.parseInt(Build.getMiuiVersionCode()) < 14 ? special : timer)) {
                        headers.add(position, header);
                    }
                }
                if (headers.size() > 25) {
                    headers.add(25, header);
                } else {
                    headers.add(header);
                }
            }
        });
    }


    private void addIconResource() {
        if (mIconModeInt == 0) {
            settingsIconResId = mResHook.addResource("ic_hyperceiler_settings", R.drawable.ic_hyperceiler_settings_v140);
        } else {
            settingsIconResId = mResHook.addResource("ic_hyperceiler_settings", R.drawable.ic_hyperceiler_settings_v130);
        }
    }
}
