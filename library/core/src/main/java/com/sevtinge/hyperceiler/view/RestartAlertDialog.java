/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.view;

import static com.sevtinge.hyperceiler.common.utils.ShellUtils.checkRootPermission;

import android.content.Context;
import android.content.res.Resources;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;
import com.sevtinge.hyperceiler.utils.DialogHelper;

import java.util.Arrays;
import java.util.List;

import fan.appcompat.app.AlertDialog;

public class RestartAlertDialog extends AlertDialog {

    public RestartAlertDialog(Context context) {
        super(context);
        setTitle(R.string.hyperceiler_restart_quick);
        setView(createMultipleChoiceView(context));
    }

    private MultipleChoiceView createMultipleChoiceView(Context context) {
        Resources resources = context.getResources();
        List<String> appNames = Arrays.asList(resources.getStringArray(R.array.restart_apps_name_hyperos));
        List<String> packageNames = Arrays.asList(resources.getStringArray(R.array.restart_apps_packagename));

        MultipleChoiceView view = new MultipleChoiceView(context);
        view.setData(appNames, null);
        view.deselectAll();
        view.setOnCheckedListener(checkedItems -> {
            dismiss();
            if (checkRootPermission() != 0) {
                DialogHelper.showAlertDialog(context, false, true);
                return;
            }

            for (int i = 0; i < checkedItems.size(); i++) {
                int appIndex = checkedItems.keyAt(i);
                if (checkedItems.valueAt(i) && appIndex < packageNames.size()) {
                    AppsTool.killApps(packageNames.get(appIndex));
                }
            }
        });
        return view;
    }
}
