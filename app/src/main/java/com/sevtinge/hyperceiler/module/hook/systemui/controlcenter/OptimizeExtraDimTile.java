package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;

public class OptimizeExtraDimTile extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {

        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04be", R.drawable.systemui_a_a_qs_extra_dim_icon_off__0__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04bf", R.drawable.systemui_a_a_qs_extra_dim_icon_off__1__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04c0", R.drawable.systemui_a_a_qs_extra_dim_icon_off__2__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04c1", R.drawable.systemui_a_a_qs_extra_dim_icon_off__3__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04c2", R.drawable.systemui_a_a_qs_extra_dim_icon_off__4__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04c3", R.drawable.systemui_a_a_qs_extra_dim_icon_off__5__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04c4", R.drawable.systemui_a_a_qs_extra_dim_icon_off__6__0);

        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04c5", R.drawable.systemui_a_a_qs_extra_dim_icon_on__0__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04c6", R.drawable.systemui_a_a_qs_extra_dim_icon_on__1__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04c7", R.drawable.systemui_a_a_qs_extra_dim_icon_on__2__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04c8", R.drawable.systemui_a_a_qs_extra_dim_icon_on__3__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04c9", R.drawable.systemui_a_a_qs_extra_dim_icon_on__4__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04ca", R.drawable.systemui_a_a_qs_extra_dim_icon_on__5__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable04cb", R.drawable.systemui_a_a_qs_extra_dim_icon_on__6__0);

        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09d2", R.drawable.systemui_a_qs_extra_dim_icon_off__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09d3", R.drawable.systemui_a_qs_extra_dim_icon_off__1);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09d4", R.drawable.systemui_a_qs_extra_dim_icon_off__2);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09d5", R.drawable.systemui_a_qs_extra_dim_icon_off__3);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09d6", R.drawable.systemui_a_qs_extra_dim_icon_off__4);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09d7", R.drawable.systemui_a_qs_extra_dim_icon_off__5);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09d8", R.drawable.systemui_a_qs_extra_dim_icon_off__6);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09d9", R.drawable.systemui_a_qs_extra_dim_icon_off__7);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09da", R.drawable.systemui_a_qs_extra_dim_icon_off__8);

        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09db", R.drawable.systemui_a_qs_extra_dim_icon_on__0);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09dc", R.drawable.systemui_a_qs_extra_dim_icon_on__1);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09dd", R.drawable.systemui_a_qs_extra_dim_icon_on__2);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09de", R.drawable.systemui_a_qs_extra_dim_icon_on__3);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09df", R.drawable.systemui_a_qs_extra_dim_icon_on__4);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09e0", R.drawable.systemui_a_qs_extra_dim_icon_on__5);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09e1", R.drawable.systemui_a_qs_extra_dim_icon_on__6);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09e2", R.drawable.systemui_a_qs_extra_dim_icon_on__7);
        mResHook.setResReplacement("com.android.systemui", "drawable", "drawable09e3", R.drawable.systemui_a_qs_extra_dim_icon_on__8);

        mResHook.setResReplacement("com.android.systemui", "drawable", "qs_extra_dim_icon_on", R.drawable.systemui_qs_extra_dim_icon_on);
        mResHook.setResReplacement("com.android.systemui", "drawable", "qs_extra_dim_icon_off", R.drawable.systemui_qs_extra_dim_icon_off);

    }
}
