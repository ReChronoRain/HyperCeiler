package com.sevtinge.cemiuiler.module.securitycenter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.util.ArrayList;
import java.util.Collections;

public class AppLockPinScramble extends BaseHook {


    @Override
    public void init() {

        hookAllConstructors("com.miui.applicationlock.widget.MiuiNumericInputView", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                LinearLayout keys = (LinearLayout) param.thisObject;
                ArrayList<View> mRandomViews = new ArrayList<View>();
                View bottom0 = null;
                View bottom2 = null;
                for (int row = 0; row <= 3; row++) {
                    ViewGroup cols = (ViewGroup) keys.getChildAt(row);
                    for (int col = 0; col <= 2; col++) {
                        if (row == 3)
                            if (col == 0) {
                                bottom0 = cols.getChildAt(col);
                                continue;
                            } else if (col == 2) {
                                bottom2 = cols.getChildAt(col);
                                continue;
                            }
                        mRandomViews.add(cols.getChildAt(col));
                    }
                    cols.removeAllViews();
                }

                Collections.shuffle(mRandomViews);

                int cnt = 0;
                for (int row = 0; row <= 3; row++)
                    for (int col = 0; col <= 2; col++) {
                        ViewGroup cols = (ViewGroup) keys.getChildAt(row);
                        if (row == 3)
                            if (col == 0) {
                                cols.addView(bottom0);
                                continue;
                            } else if (col == 2) {
                                cols.addView(bottom2);
                                continue;
                            }
                        cols.addView(mRandomViews.get(cnt));
                        cnt++;
                    }
            }
        });
    }
}
