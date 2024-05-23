package com.sevtinge.hyperceiler.ui.sub.systemui;

import android.os.Bundle;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class CardTileSettings extends SettingsPreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getContentResId() {
        return R.xml.system_ui_control_center_card_tile;
    }
}
