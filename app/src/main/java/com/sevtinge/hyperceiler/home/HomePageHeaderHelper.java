package com.sevtinge.hyperceiler.home;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.sevtinge.hyperceiler.home.banner.HomePageBannerHelper;
import com.sevtinge.hyperceiler.home.tips.HomePageTipHelper;
import com.sevtinge.hyperceiler.ui.adapter.ProxyHeaderViewAdapter;

import java.util.List;

public class HomePageHeaderHelper {

    public static void refreshAll(Context context, ProxyHeaderViewAdapter adapter, View.OnClickListener listener) {
        if (context == null || adapter == null) return;

        // 1. 每次都创建新的容器，彻底解决 Parent 冲突导致的 FC
        LinearLayout masterContainer = new LinearLayout(context);
        masterContainer.setOrientation(LinearLayout.VERTICAL);
        masterContainer.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

        // 2. 从 BannerHelper 获取 Banner 视图列表
        List<View> bannerViews = HomePageBannerHelper.getBannerViews(context, listener);
        for (View v : bannerViews) {
            // 确保 View 没有被重复添加（针对从其他 Helper 获取的情况）
            removeFromParent(v);
            masterContainer.addView(v);
        }

        // 3. 从 TipHelper 获取小贴士视图
        View tipView = HomePageTipHelper.getTipView(context);
        removeFromParent(tipView);
        masterContainer.addView(tipView);

        // 4. 将全新的容器挂载到适配器
        if (masterContainer.getChildCount() > 0) {
            adapter.addRemovableHintView(masterContainer);
        } else {
            adapter.removeRemovableHintView(masterContainer);
        }
    }

    /**
     * 安全剥离：确保一个 View 在加入新容器前没有父布局
     */
    private static void removeFromParent(View view) {
        if (view != null && view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }
}
