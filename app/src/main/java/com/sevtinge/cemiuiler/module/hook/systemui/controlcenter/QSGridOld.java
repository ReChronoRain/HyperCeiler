package com.sevtinge.cemiuiler.module.hook.systemui.controlcenter;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.base.BaseHook;

public class QSGridOld extends BaseHook {

    @Override
    public void init() {
        int cols = mPrefsMap.getInt("system_control_center_old_qs_column", 2);
        int rows = mPrefsMap.getInt("system_control_center_old_qs_row", 1);
        int colsRes = R.integer.quick_settings_num_columns_3;
        int rowsRes = R.integer.quick_settings_num_rows_4;

        switch (cols) {
            case 3 -> colsRes = R.integer.quick_settings_num_columns_3;
            case 4 -> colsRes = R.integer.quick_settings_num_columns_4;
            case 5 -> colsRes = R.integer.quick_settings_num_columns_5;
            case 6 -> colsRes = R.integer.quick_settings_num_columns_6;
            case 7 -> colsRes = R.integer.quick_settings_num_columns_7;
        }

        switch (rows) {
            case 2 -> rowsRes = R.integer.quick_settings_num_rows_2;
            case 3 -> rowsRes = R.integer.quick_settings_num_rows_3;
            case 4 -> rowsRes = R.integer.quick_settings_num_rows_4;
            case 5 -> rowsRes = R.integer.quick_settings_num_rows_5;
        }

        if (cols > 2)
            mResHook.setResReplacement("com.android.systemui", "integer", "quick_settings_num_columns", colsRes);
        if (rows > 1) mResHook.setResReplacement("com.android.systemui", "integer", "quick_settings_num_rows", rowsRes);
    }
}
