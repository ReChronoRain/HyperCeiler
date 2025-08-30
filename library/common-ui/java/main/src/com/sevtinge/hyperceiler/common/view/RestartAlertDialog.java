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
package com.sevtinge.hyperceiler.common.view;

import static com.sevtinge.hyperceiler.common.utils.DialogHelper.showAlertDialog;
import static com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.checkRootPermission;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool;
import com.sevtinge.hyperceiler.hook.module.skip.GlobalActions;
import com.sevtinge.hyperceiler.ui.R;

import java.util.Arrays;
import java.util.List;

import fan.appcompat.app.AlertDialog;

public class RestartAlertDialog extends AlertDialog {

    private List<String> mAppNameList;
    private List<String> mAppPackageNameList;

    public RestartAlertDialog(Context context) {
        super(context);
        setTitle(R.string.hyperceiler_restart_quick);
        setView(createMultipleChoiceView(context));
    }

    private MultipleChoiceView createMultipleChoiceView(Context context) {
        Resources mRes = context.getResources();
        MultipleChoiceView view = new MultipleChoiceView(context);
        mAppNameList = Arrays.asList(mRes.getStringArray(R.array.restart_apps_name_hyperos));
        mAppPackageNameList = Arrays.asList(mRes.getStringArray(R.array.restart_apps_packagename));
        view.setData(mAppNameList, null);
        view.deselectAll();
        view.setOnCheckedListener(sparseBooleanArray -> {
            dismiss();
            int size = sparseBooleanArray.size();
            if (checkRootPermission() != 0) {
                showAlertDialog(context, false, true);
                return;
            }
            for (int i = 0; i < size; i++) {
                if (sparseBooleanArray.get(i)) {
                    // ShellUtils.execCommand("pkill -l 9 -f " + mAppPackageNameList.get(i), true, false);
                    // String test = "XX";
                    String packageGet = mAppPackageNameList.get(i);
                    AppsTool.killApps(packageGet);
                }
            }
        });
        return view;
    }

    public void restartApp(Context context, String packageName) {
        Intent intent = new Intent(GlobalActions.ACTION_PREFIX + "RestartApps");
        intent.putExtra("packageName", packageName);
        context.sendBroadcast(intent);
    }

    public void restartSystemUI(Context context) {
        Intent intent = new Intent(GlobalActions.ACTION_PREFIX + "RestartSystemUI");
        intent.setPackage("com.android.systemui");
        context.sendBroadcast(intent);
    }
}
