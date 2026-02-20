package com.sevtinge.hyperceiler.home.banner;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.R;

import java.util.ArrayList;
import java.util.List;

import fan.animation.Folme;
import fan.animation.base.AnimConfig;

public class HomePageBannerHelper {

    public static List<View> getBannerViews(Context context, View.OnClickListener listener) {
        List<View> views = new ArrayList<>();
        List<BannerBean> beans = HomePageBannerManager.getLocalBannerBeans(context);
        for (BannerBean bean : beans) {
            views.add(createViewFromBean(context, bean, listener));
        }
        return views;
    }

    public static Drawable getBannerIcon(Context context, BannerBean bannerBean) throws NumberFormatException {
        if (context == null) {
            return null;
        }
        if (bannerBean.getIconResId() != -1) {
            return context.getDrawable(bannerBean.getIconResId());
        }
        return null;
    }

    /**
     * 将 BannerBean 转换为 View
     */
    /**
     * 4. 统一渲染 View 的方法
     */
    private static View createViewFromBean(Context context, BannerBean bean, View.OnClickListener listener) {
        // 关键：attachToRoot 传 false
        View v = LayoutInflater.from(context).inflate(R.layout.settings_banner_main_layout, null, false);

        TextView title = v.findViewById(android.R.id.title);
        TextView summary = v.findViewById(android.R.id.summary);
        ImageView iconView = v.findViewById(android.R.id.icon);
        ImageView arrowRightView = v.findViewById(R.id.arrow_right);

        ViewGroup containerView = v.findViewById(R.id.container);

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
        v.setTag(bean); // 将数据存在 tag 里方便回调获取
        v.setOnClickListener(listener);

        //Folme.useAt(v).touch().setScale(0.95f).handleTouchOf(v, new AnimConfig[0]);

        return v;
    }
}
