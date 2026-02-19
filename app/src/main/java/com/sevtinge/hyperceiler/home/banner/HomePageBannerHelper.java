package com.sevtinge.hyperceiler.home.banner;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.R;

import com.sevtinge.hyperceiler.ui.adapter.ProxyHeaderViewAdapter;
import com.sevtinge.hyperceiler.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

import fan.animation.Folme;
import fan.animation.base.AnimConfig;

public class HomePageBannerHelper {

    private static LinearLayout mBannerContainer;

    public interface BannerCallback {
        void onGetBanner(BannerBean bannerBean);
    }

    public static void queryAndSaveBannerOnBg(final Context context, final BannerCallback bannerCallback) {
        ThreadUtils.postOnBackgroundThread(() -> {
            if (context != null) {
                if (bannerCallback != null) {
                    ThreadUtils.postOnMainThread(() -> bannerCallback.onGetBanner(query(context)));
                }
            }
        });
    }

    private static BannerBean query(Context context) {

        return null;
    }


    public static BannerBean readBannerBean(Context context) {
        BannerBean bannerBean = new BannerBean();
        bannerBean.setId("0");
        bannerBean.setAuthority("Test");
        bannerBean.setPkg("Test");
        bannerBean.setPriority(1001);
        bannerBean.setTitle("Test");
        bannerBean.setArrowIcon(0);
        bannerBean.setAction("Test");
        bannerBean.setExtras("Test");
        bannerBean.setSummary("Test");
        bannerBean.setIcon("Test");
        bannerBean.setUrl("Test");
        bannerBean.setIconResId(com.sevtinge.hyperceiler.core.R.drawable.ic_hyperceiler_cartoon);
        return bannerBean;
    }

    public static boolean isEmptyBanner(BannerBean bannerBean) {
        return bannerBean == null || TextUtils.isEmpty(bannerBean.getId()) ||
            TextUtils.isEmpty(bannerBean.getTitle()) ||
            TextUtils.isEmpty(bannerBean.getAuthority()) || bannerBean.getPriority() == 1000 ||
            TextUtils.isEmpty(bannerBean.getPkg()) || bannerBean.getArrowIcon() == -1;
    }







    /**
     * 核心方法：刷新容器并显示在首页
     */
    public static void refreshAllBanners(Context context, ProxyHeaderViewAdapter adapter, BannerBean dynamicBean, View.OnClickListener listener) {
        if (context == null || adapter == null) return;

        ensureContainer(context);
        mBannerContainer.removeAllViews();

        // --- 核心改动：直接从 Manager 获取本地 Beans ---
        List<BannerBean> allBeans = new ArrayList<>();
        allBeans.addAll(HomePageBannerManager.getLocalBannerBeans(context));

        // 加入异步 Bean (去重逻辑)
        if (!isEmptyBanner(dynamicBean)) {
            boolean exists = false;
            for (BannerBean b : allBeans) {
                if (b.getId().equals(dynamicBean.getId())) { exists = true; break; }
            }
            if (!exists) allBeans.add(dynamicBean);
        }

        // 渲染所有 Beans
        for (BannerBean bean : allBeans) {
            mBannerContainer.addView(createViewFromBean(context, bean, listener));
        }

        // 重新挂载到适配器
        if (mBannerContainer.getChildCount() > 0) {
            adapter.addRemovableHintView(mBannerContainer);
        } else {
            adapter.removeRemovableHintView(mBannerContainer);
        }
    }

    /**
     * 1. 确保容器存在 (单例模式)
     */
    private static void ensureContainer(Context context) {
        if (mBannerContainer == null) {
            mBannerContainer = new LinearLayout(context);
            mBannerContainer.setOrientation(LinearLayout.VERTICAL);
            // 宽度占满，高度自适应
            mBannerContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    /**
     * 将 BannerBean 转换为 View
     */
    /**
     * 4. 统一渲染 View 的方法
     */
    private static View createViewFromBean(Context context, BannerBean bean, View.OnClickListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.settings_banner_main_layout, null);

        TextView title = view.findViewById(android.R.id.title);
        TextView summary = view.findViewById(android.R.id.summary);
        ImageView iconView = view.findViewById(android.R.id.icon);
        ImageView arrowRightView = view.findViewById(R.id.arrow_right);

        ViewGroup containerView = view.findViewById(R.id.container);

        if (title != null) {
            if (bean.getTitle() != null) {
                title.setText(bean.getTitle());
            } else {
                title.setVisibility(View.GONE);
            }
            if (bean.getTitleColorResId() != -1) {
                title.setTextColor(bean.getTitleColorResId());
            } else if (!TextUtils.isEmpty(bean.getTitleColor())) {
                title.setTextColor(Color.parseColor(bean.getTitleColor()));
            }
        }
        if (summary != null) {
            if (bean.getSummary() != null) {
                summary.setText(bean.getSummary());
            } else {
                summary.setVisibility(View.GONE);
            }

            if (bean.getSubTitleColorResId() != -1) {
                summary.setTextColor(bean.getSubTitleColorResId());
            } else if (!TextUtils.isEmpty(bean.getSummaryColor())) {
                summary.setTextColor(Color.parseColor(bean.getSummaryColor()));
            }
        }

        if (iconView != null) {
            Drawable icon = getBannerIcon(context, bean);
            if (icon != null) {
                iconView.setImageDrawable(icon);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
        }

        if (bean.getArrowIcon() != -1) {

        } else {
            arrowRightView.setVisibility(View.GONE);
        }

        if (bean.getBackgroundColorResId() != -1) {
            containerView.setBackgroundResource(bean.getBackgroundColorResId());
        } else if (!TextUtils.isEmpty(bean.getBackgroundColor())) {
            containerView.setBackgroundColor(Color.parseColor(bean.getBackgroundColor()));
        }

        // 点击事件处理
        view.setTag(bean); // 将数据存在 tag 里方便回调获取
        view.setOnClickListener(listener);

        // 小米 Folme 动效
        Folme.useAt(view).touch().setScale(1.0f).handleTouchOf(view, new AnimConfig[0]);

        // 设置卡片间距 (8dp)
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = (int) (context.getResources().getDisplayMetrics().density * 8);
        view.setLayoutParams(lp);

        return view;
    }

    private static BannerBean createLocalBean(String id, String title, String summary, int iconRes) {
        BannerBean bean = new BannerBean();
        bean.setId(id);
        bean.setTitle(title);
        bean.setSummary(summary);
        bean.setIconResId(iconRes);
        bean.setPriority(500);
        return bean;
    }

    public static Drawable getBannerIcon(Context context, BannerBean bannerBean) throws NumberFormatException {
        if (context == null) {
            return null;
        }
        /*if (isEmptyBanner(bannerBean)) {
            return context.getDrawable(R.drawable.ic_other_advanced_settings);
        }*/
        if (bannerBean.getIconResId() != -1) {
            return context.getDrawable(bannerBean.getIconResId());
        }
        return null;
    }
}
