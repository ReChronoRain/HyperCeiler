package com.sevtinge.hyperceiler.home;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.dashboard.SubSettings;
import com.sevtinge.hyperceiler.home.adapter.ProxyHeaderViewAdapter;
import com.sevtinge.hyperceiler.home.banner.HomePageBannerHelper;
import com.sevtinge.hyperceiler.home.helper.CantSeeAppsFragment;
import com.sevtinge.hyperceiler.home.tips.HomePageTipHelper;
import com.sevtinge.hyperceiler.home.utils.HeaderManager;
import com.sevtinge.hyperceiler.utils.SettingLauncherHelper;

import java.util.List;

public class HomePageHeaderHelper {

    public static void refreshAll(Context context, ProxyHeaderViewAdapter adapter, View.OnClickListener listener) {
        if (context == null || adapter == null) {
            return;
        }

        refreshHeaderViews(context, adapter, listener);
        refreshFooterGuide(context, adapter);
    }

    private static void refreshHeaderViews(Context context, ProxyHeaderViewAdapter adapter, View.OnClickListener listener) {
        View oldView = adapter.getRemoveHintView();
        LinearLayout masterContainer;
        boolean isNewContainer = false;
        if (oldView instanceof LinearLayout linearLayout) {
            masterContainer = linearLayout;
            masterContainer.removeAllViews();
        } else {
            masterContainer = new LinearLayout(context);
            masterContainer.setOrientation(LinearLayout.VERTICAL);
            masterContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            isNewContainer = true;
        }

        List<View> bannerViews = HomePageBannerHelper.getBannerViews(context, listener);
        for (View view : bannerViews) {
            removeFromParent(view);
            masterContainer.addView(view);
        }

        View tipView = HomePageTipHelper.getTipView(context);
        removeFromParent(tipView);
        masterContainer.addView(tipView);

        if (masterContainer.getChildCount() > 0) {
            if (isNewContainer) {
                if (oldView != null) {
                    adapter.removeRemovableHintView(oldView);
                }
                adapter.addRemovableHintView(masterContainer);
            } else {
                masterContainer.requestLayout();
                masterContainer.invalidate();
            }
            return;
        }

        if (oldView != null) {
            adapter.removeRemovableHintView(oldView);
        }
    }

    private static void refreshFooterGuide(Context context, ProxyHeaderViewAdapter adapter) {
        View oldFooter = adapter.getFooterHintView();
        if (!HeaderManager.shouldShowCantSeeAppsGuide(context)) {
            if (oldFooter != null) {
                adapter.removeFooterHintView(oldFooter);
            }
            return;
        }

        View footer = createCantSeeAppsGuideView(context);
        removeFromParent(footer);
        if (oldFooter != null) {
            adapter.removeFooterHintView(oldFooter);
        }
        adapter.addFooterHintView(footer);
    }

    private static View createCantSeeAppsGuideView(Context context) {
        Context themedContext = new ContextThemeWrapper(context, com.sevtinge.hyperceiler.R.style.HomeNavigatorContentTheme);
        LayoutInflater inflater = LayoutInflater.from(themedContext);
        View view = inflater.inflate(com.sevtinge.hyperceiler.core.R.layout.preference_recommend, null, false);
        LinearLayout container = view.findViewById(com.sevtinge.hyperceiler.core.R.id.line_layout);
        View itemView = inflater.inflate(com.sevtinge.hyperceiler.core.R.layout.preference_recommend_item, container, false);
        TextView itemTextView = itemView.findViewById(com.sevtinge.hyperceiler.core.R.id.recommend_item);

        if (itemTextView != null) {
            itemTextView.setText(com.sevtinge.hyperceiler.core.R.string.help_cant_see_apps_guide);
        }
        if (container != null) {
            container.removeAllViews();
            container.addView(itemView);
        }

        View.OnClickListener clickListener = v -> SettingLauncherHelper.onStartSettingsForArguments(
            context,
            SubSettings.class,
            CantSeeAppsFragment.class.getName(),
            com.sevtinge.hyperceiler.core.R.string.help_cant_see_apps_title
        );
        view.setOnClickListener(clickListener);
        itemView.setOnClickListener(clickListener);
        return view;
    }

    private static void removeFromParent(View view) {
        if (view != null && view.getParent() instanceof ViewGroup group) {
            group.removeView(view);
        }
    }
}
