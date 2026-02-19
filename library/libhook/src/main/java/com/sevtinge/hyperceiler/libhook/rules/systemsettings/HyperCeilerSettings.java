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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemsettings;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getModuleRes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.api.PropUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import java.util.ArrayList;
import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class HyperCeilerSettings extends BaseHook {

    private int settingsIconResId;
    private Class<?> mPreferenceHeader;
    private final int opt = mPrefsMap.getStringAsInt("settings_icon", 0);
    private final int mIconModeInt = mPrefsMap.getStringAsInt("settings_icon_mode", 0);

    @Override
    public void init() {
        initSettingsIcon();
        hookUpdateHeaderList();
    }

    private void initSettingsIcon() {
        settingsIconResId = mIconModeInt == 0
            ? R.drawable.ic_hyperceiler_settings_v140
            : R.drawable.ic_hyperceiler_settings_v130;
    }

    private void hookUpdateHeaderList() {
        Class<?> mMiuiSettings = findClassIfExists("com.android.settings.MiuiSettings");
        if (mMiuiSettings == null) return;

        findAndHookMethod(mMiuiSettings, "updateHeaderList", List.class, new IMethodHook() {
            @Override
            @SuppressLint("DiscouragedApi")
            public void after(AfterHookParam param) {
                if (param.getArgs()[0] == null || opt == 0) return;

                Activity activity = (Activity) param.getThisObject();
                Context context = activity.getBaseContext();
                List<Object> headers = (List<Object>) param.getArgs()[0];

                Object header = createHyperCeilerHeader(context);
                if (header == null) return;

                insertHeaderAtPosition(headers, context, header);
            }
        });
    }

    private Object createHyperCeilerHeader(Context context) {
        mPreferenceHeader = findClassIfExists("com.android.settingslib.miuisettings.preference.PreferenceActivity$Header");
        if (mPreferenceHeader == null) return null;

        Resources modRes = getModuleRes(context);
        Object header = EzxHelpUtils.newInstance(mPreferenceHeader);

        // 设置基本属性
        EzxHelpUtils.setLongField(header, "id", 666);
        EzxHelpUtils.setIntField(header, "iconRes", settingsIconResId);
        EzxHelpUtils.setObjectField(header, "title", modRes.getString(R.string.library_app_name));

        // 设置 Intent
        Intent intent = new Intent();
        intent.putExtra("isDisplayHomeAsUpEnabled", true);
        intent.setClassName(ProjectApi.mAppModulePkg, "com.sevtinge.hyperceiler.oldui.ui.HyperCeilerTabActivity");
        EzxHelpUtils.setObjectField(header, "intent", intent);

        // 设置用户信息
        Bundle bundle = new Bundle();
        ArrayList<UserHandle> users = new ArrayList<>();
        users.add((UserHandle) EzxHelpUtils.newInstance(UserHandle.class, 0));
        bundle.putParcelableArrayList("header_user", users);
        EzxHelpUtils.setObjectField(header, "extras", bundle);

        return header;
    }

    private void insertHeaderAtPosition(List<Object> headers, Context context, Object header) {
        int insertPosition = findInsertPosition(headers, context);

        if (insertPosition != -1) {
            headers.add(insertPosition, header);
        } else if (headers.size() > 25) {
            headers.add(25, header);
        } else {
            headers.add(header);
        }
    }

    private int findInsertPosition(List<Object> headers, Context context) {
        int device = context.getResources().getIdentifier("my_device", "id", context.getPackageName());
        int themes = context.getResources().getIdentifier("launcher_settings", "id", context.getPackageName());
        int special = context.getResources().getIdentifier("other_special_feature_settings", "id", context.getPackageName());
        int timer = context.getResources().getIdentifier("app_timer", "id", context.getPackageName());

        for (int position = 0; position < headers.size(); position++) {
            Object head = headers.get(position);
            long id = EzxHelpUtils.getLongField(head, "id");

            if (shouldInsertAfter(id, device, themes, special, timer)) {
                return position + 1;
            }
        }

        return -1;
    }

    private boolean shouldInsertAfter(long id, int device, int themes, int special, int timer) {
        return (opt == 1 && id == device)
            || (opt == 2 && id == themes)
            || (opt == 3 && id == (PropUtils.getProp("ro.miui.ui.version.code", 0) < 14 ? special : timer));
    }
}
