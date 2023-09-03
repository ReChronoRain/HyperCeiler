package com.sevtinge.cemiuiler.module.hook.systemui.controlcenter;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.base.BaseHook;

public class QQSGrid extends BaseHook {
    @Override
    public void init() {
        int cols = mPrefsMap.getInt("system_control_center_old_qs_grid_columns", 2);
        int colsResId = switch (cols) {
            case 3 -> R.integer.quick_quick_settings_num_rows_3;
            case 4 -> R.integer.quick_quick_settings_num_rows_4;
            case 5 -> R.integer.quick_quick_settings_num_rows_5;
            case 6 -> R.integer.quick_quick_settings_num_rows_6;
            case 7 -> R.integer.quick_quick_settings_num_rows_7;
            default -> R.integer.quick_quick_settings_num_rows_5;
        };
        mResHook.setResReplacement("com.android.systemui", "integer", "quick_settings_qqs_count", colsResId);
    }
}
