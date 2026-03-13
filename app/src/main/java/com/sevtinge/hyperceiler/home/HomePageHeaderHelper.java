package com.sevtinge.hyperceiler.home;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.sevtinge.hyperceiler.home.adapter.ProxyHeaderViewAdapter;
import com.sevtinge.hyperceiler.home.banner.HomePageBannerHelper;
import com.sevtinge.hyperceiler.home.tips.HomePageTipHelper;

import java.util.List;

public class HomePageHeaderHelper {

    public static void refreshAll(Context context, ProxyHeaderViewAdapter adapter, View.OnClickListener listener) {
        if (context == null || adapter == null) return;

        LinearLayout masterContainer = new LinearLayout(context);
        masterContainer.setOrientation(LinearLayout.VERTICAL);
        masterContainer.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

        List<View> bannerViews = HomePageBannerHelper.getBannerViews(context, listener);
        for (View v : bannerViews) {
            removeFromParent(v);
            masterContainer.addView(v);
        }

        View tipView = HomePageTipHelper.getTipView(context);
        removeFromParent(tipView);
        masterContainer.addView(tipView);

        if (masterContainer.getChildCount() > 0) {
            adapter.addRemovableHintView(masterContainer);
        } else {
            View oldView = adapter.getRemoveHintView();
            if (oldView != null) {
                adapter.removeRemovableHintView(oldView);
            }
        }
    }

    private static void removeFromParent(View view) {
        if (view != null && view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }
}
