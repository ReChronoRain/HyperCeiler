package com.sevtinge.hyperceiler.ui;

import android.os.Bundle;

import fan.appcompat.app.Fragment;
import fan.navigator.Navigator;
import fan.navigator.navigatorinfo.FragmentNavInfo;

public class UpdateNavigationSelectNavInfo extends FragmentNavInfo {
    public UpdateNavigationSelectNavInfo(int id, Class<? extends Fragment> fname, Bundle args) {
        super(id, fname, args);
    }

    @Override
    public boolean onNavigate(Navigator navigator) {
        return !"miuix.secondaryContent".equals(navigator.getTag());
    }
}
