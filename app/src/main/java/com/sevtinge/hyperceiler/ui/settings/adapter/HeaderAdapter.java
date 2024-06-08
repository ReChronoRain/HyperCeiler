package com.sevtinge.hyperceiler.ui.settings.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.controller.TipsInfoController;
import com.sevtinge.hyperceiler.ui.CeilerTabActivity;
import com.sevtinge.hyperceiler.ui.settings.utils.SettingsFeatures;
import com.sevtinge.hyperceiler.ui.settings.adapter.viewholder.HeaderViewHolder;
import com.sevtinge.hyperceiler.ui.settings.core.BaseSettingsController;
import com.sevtinge.hyperceiler.ui.settings.utils.BitmapUtils;

import java.util.HashMap;
import java.util.List;

import fan.animation.Folme;
import fan.animation.ITouchStyle;
import fan.animation.base.AnimConfig;
import fan.appcompat.app.AppCompatActivity;
import fan.core.utils.AttributeResolver;
import fan.core.utils.DisplayUtils;

public class HeaderAdapter extends RecyclerView.Adapter<HeaderViewHolder> {

    private Context mContext;
    private AppCompatActivity mActivity;
    private int mNormalIconSize = 0;

    private List<PreferenceHeader> mHeaders;
    private LayoutInflater mInflater;
    private HashMap<Long, BaseSettingsController> mSettingsControllerMap = new HashMap<>();

    public HeaderAdapter(AppCompatActivity appCompatActivity, List<PreferenceHeader> headers) {
        mActivity = appCompatActivity;
        mContext = appCompatActivity.getApplicationContext();
        mHeaders = headers;
        mInflater = LayoutInflater.from(appCompatActivity);
        mSettingsControllerMap.put((long) R.id.tips, new TipsInfoController(appCompatActivity, null));
    }

    @Override
    public int getItemViewType(int position) {
        return getHeaderType(getItem(position));
    }

    @NonNull
    @Override
    public HeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == 0) {
            itemView = mInflater.inflate(R.layout.miuix_preference_category_layout, parent, false);
        } else {
            if (viewType == 1 || viewType == 2 || viewType == 3 || viewType == 5 || viewType == 6 || viewType == 7) {
                if (viewType == 7) {
                    itemView = mInflater.inflate(R.layout.preference_hyperlink, parent, false);
                } else {
                    itemView = mInflater.inflate(SettingsFeatures.isSplitTablet(mContext) ?
                            R.layout.miuix_preference_navigation_item :
                            R.layout.miuix_preference_main_layout, parent, false);
                    ViewGroup widgetFrame = itemView.findViewById(android.R.id.widget_frame);
                    if (widgetFrame != null) {
                        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                        if (SettingsFeatures.isSplitTablet(mContext)) {
                            inflater.inflate(R.layout.miuix_preference_widget_navigation_text, widgetFrame, true);
                        } else {
                            inflater.inflate(R.layout.miuix_preference_widget_text, widgetFrame, true);
                            TextView textRight = widgetFrame.findViewById(R.id.text_right);
                            textRight.setMaxWidth(mContext.getResources().getDimensionPixelSize(R.dimen.miuix_preference_text_right_max_width));
                        }
                    }
                    View arrowRight = itemView.findViewById(R.id.arrow_right);
                    if (arrowRight != null) arrowRight.setVisibility(View.VISIBLE);
                    if (!SettingsFeatures.isSplitTablet(mContext)) {
                        Folme.useAt(itemView).touch().setScale(1.0f, new ITouchStyle.TouchType[0]).setBackgroundColor(mContext.getResources().getColor(R.color.settings_item_touch_color, mContext.getTheme())).setTintMode(1).handleTouchOf(itemView, new AnimConfig[0]);
                    } else {
                        itemView.setForeground(AttributeResolver.resolveDrawable(mContext, R.attr.preferenceItemForeground));
                    }
                }
            } else {
                itemView = mInflater.inflate(SettingsFeatures.isSplitTablet(mContext) ?
                        R.layout.miuix_preference_navigation_item :
                        R.layout.miuix_preference_main_layout, parent, false);;
                ViewGroup widgetFrame = itemView.findViewById(android.R.id.widget_frame);
                if (widgetFrame != null) {
                    LayoutInflater inflater = LayoutInflater.from(mContext);
                    if (SettingsFeatures.isSplitTablet(mContext)) {
                        inflater.inflate(R.layout.miuix_preference_widget_navigation_text, widgetFrame, true);
                    } else {
                        inflater.inflate(R.layout.miuix_preference_widget_text, widgetFrame, true);
                        TextView textRight = widgetFrame.findViewById(R.id.text_right);
                        textRight.setMaxWidth(mContext.getResources().getDimensionPixelSize(R.dimen.miuix_preference_text_right_max_width));
                    }
                }
            }
        }
        itemView.setTag(viewType);
        return new HeaderViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull HeaderViewHolder holder, int position) {
        if (position >= 0 && position < mHeaders.size()) {
            View itemView = holder.itemView;
            Resources res = mContext.getResources();
            PreferenceHeader header = getItem(position);
            int headerType = getHeaderType(header);
            if (headerType == 0) {
                holder.title.setText(header.getTitle(res));
                if (TextUtils.isEmpty(holder.title.getText())) {
                    holder.title.setVisibility(View.GONE);
                    if (itemView != null) {
                        itemView.setImportantForAccessibility(4);
                        itemView.setBackgroundResource(R.drawable.miuix_preference_category_bg_no_title);
                    }
                } else {
                    holder.title.setVisibility(View.VISIBLE);
                    if (itemView != null) {
                        int backgroundResource;
                        if (position == 0) {
                            backgroundResource = R.drawable.miuix_preference_category_bg_first;
                        } else {
                            backgroundResource = R.drawable.miuix_preference_category_bg;
                        }
                        itemView.setBackgroundResource(backgroundResource);
                    }
                    if (header.id == R.id.tips) {
                        TipsInfoController controller = (TipsInfoController) mSettingsControllerMap.get((long) R.id.tips);
                        if (controller != null) {
                            controller.setUpTextView(holder.title);
                        }
                    }
                }
            } else {
                if (headerType == 2) {
                    TextView value = holder.value;
                    if (value != null) {
                        value.setBackground(null);
                        holder.value.setGravity(8388613);
                        BaseSettingsController controller = mSettingsControllerMap.get(header.id);
                        if (controller != null) {
                            if (isAdapterVerticalSummary(header)) {
                                holder.summary.setVisibility(View.VISIBLE);
                                holder.summary.setTextAppearance(R.style.TextAppearance_PreferenceRight);
                                /*baseSettingsController2.setStatusView(miuiSettings$HeaderViewHolder.summary);*/
                            } else {
                                holder.value.setTextAppearance(R.style.TextAppearance_PreferenceRight);
                                holder.value.setVisibility(View.VISIBLE);
                                /*baseSettingsController2.setStatusView(miuiSettings$HeaderViewHolder.value);*/
                            }
                        }
                    }
                }

                holder.title.setText(header.getTitle(res));
                if (headerType != 7) {
                    CharSequence summary = header.getSummary(res);
                    if (!TextUtils.isEmpty(summary)) {
                        holder.summary.setVisibility(View.VISIBLE);
                        holder.summary.setText(summary);
                    } else {
                        holder.summary.setVisibility(View.GONE);
                    }
                    if (isAdapterVerticalSummary(header)) {
                        holder.value.setVisibility(View.GONE);
                        holder.summary.setVisibility(View.VISIBLE);
                    }
                    if (headerType == 2) {
                        /*((DeviceStatusController)this.mSettingsControllerMap.get(item.id)).setUpTextView(miuiSettings$HeaderViewHolder.value, miuiSettings$HeaderViewHolder.arrowRight);*/
                    }
                    setExtraPadding(holder, itemView, header);
                }
            }
            setSelectedHeaderView(holder, position);
            setIcon(holder, header);
            setEnable(holder, header);
            Bundle extras = header.extras;
            //if (extras != null && extras.getBoolean("admin_disallow")) setRestrictionEnforced(holder, true);
            setClick(holder, header, position);
        }
    }

    private boolean isAdapterVerticalSummary(PreferenceHeader header) {
        return isDeviceAdapterVerticalSummary(mContext);
    }

    public boolean isDeviceAdapterVerticalSummary(Context context) {
        if (SettingsFeatures.isPadDevice()) return true;
        if (SettingsFeatures.isFoldDevice()) {
            if (Build.DEVICE.contains("zizhan") && Build.DEVICE.contains("babylon")) {
                return SettingsFeatures.isScreenLayoutLarge(context);
            }
            return true;
        }
        return false;
    }

    private void setSelectedHeaderView(HeaderViewHolder headerViewHolder, int i) {
        if (headerViewHolder != null) {
            if (SettingsFeatures.isSplitTablet(mContext)) {
                /*if (MiuiSettings.mCurrentSelectedHeaderIndex == i) {
                    setSelectorColor(0);
                    setSelectedView(headerViewHolder.itemView);
                    setSelectorColor(MiuiSettings.SELECTOR_COLOR);
                    return;
                }*/
                headerViewHolder.itemView.setSelected(false);
            }
        }
    }

    public void setIcon(HeaderViewHolder headerViewHolder, PreferenceHeader header) {
        ImageView imageView;
        if (headerViewHolder == null || (imageView = headerViewHolder.icon) == null || imageView.getVisibility() == View.GONE) {
            return;
        }
        Bundle bundle = header.fragmentArguments;
        if (header.iconRes != 0) {
            headerViewHolder.icon.setVisibility(View.VISIBLE);
            headerViewHolder.icon.setImageResource(header.iconRes);
        } else {
            headerViewHolder.icon.setVisibility(View.INVISIBLE);
        }
        if (!(headerViewHolder.icon.getDrawable() instanceof BitmapDrawable)) {
            return;
        }
        int dimensionPixelSize = headerViewHolder.icon.getResources().getDimensionPixelSize(R.dimen.header_icon_size);
        ImageView imageView2 = headerViewHolder.icon;
        imageView2.setImageBitmap(BitmapUtils.createBitmap(imageView2.getDrawable(), dimensionPixelSize, dimensionPixelSize));
    }


    private void setEnable(HeaderViewHolder headerViewHolder, PreferenceHeader header) {
        if (headerViewHolder == null) {
            return;
        }
        TextView textView = headerViewHolder.title;
        if (textView != null) {
            textView.setEnabled(true);
        }
        TextView textView2 = headerViewHolder.summary;
        if (textView2 != null) {
            textView2.setEnabled(true);
        }
    }


    private void setExtraPadding(HeaderViewHolder holder, View view, PreferenceHeader header) {
        if (view != null) {
            if (mNormalIconSize == 0) {
                mNormalIconSize = mContext.getResources().getDimensionPixelSize(R.dimen.header_icon_size);
            }
            int horizontal = DisplayUtils.dip2px(mContext, 28.0f);
            holder.icon.setPaddingRelative(0, 0, DisplayUtils.dip2px(mContext, 16.0f), 0);
            view.setPaddingRelative(horizontal, view.getPaddingTop(), horizontal, view.getPaddingBottom());
        }
    }

    public void setClick(HeaderViewHolder holder, PreferenceHeader header, int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (position >= 0 && position < mHeaders.size()) {
                    /*if (SettingsFeatures.isSplitTablet(mContext) && i != mCurrentSelectedHeaderIndex) {
                        if (mSelectedView != null) {
                            mSelectedView.setSelected(false);
                        }
                        mSelectedView = holder.itemView;
                        if (mSelectedView != null) {
                            mSelectedView.setSelected(true);
                        }
                    }*/
                    try {
                        ((CeilerTabActivity)mActivity).onHeaderClick(mHeaders.get(position), position);
                    } catch (Resources.NotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void updateHeaderViewInfo() {
        if (mSettingsControllerMap != null) {
            TipsInfoController controller = (TipsInfoController) mSettingsControllerMap.get((long) R.id.tips);
            if (controller != null) {
                controller.updateStatus();
            }
        }
    }

    public PreferenceHeader getItem(int position) {
        return mHeaders.get(position);
    }


    @Override
    public int getItemCount() {
        return mHeaders != null ? mHeaders.size() : 0;

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private int getHeaderType(PreferenceHeader header) {
        boolean isClick = header.fragment == null && header.intent == null;
        return isClick ? 0 : header.id == R.id.help_cant_see_app ? 7 : 1;
    }

    public void start() {
        /*for (BaseSettingsController baseSettingsController : this.mSettingsControllerMap.values()) {
            baseSettingsController.start();
        }
        if (!Build.IS_INTERNATIONAL_BUILD || !SettingsFeatures.isSplitTablet(this.mContext) || MiuiSettings.this.mSettingsFragment == null || MiuiSettings.this.mSettingsFragment.getHeaderAdapter() == null) {
            return;
        }
        MiuiSettings.this.mSettingsFragment.getHeaderAdapter().notifyDataSetChanged();*/
    }

    public void resume() {
        /*for (BaseSettingsController baseSettingsController : this.mSettingsControllerMap.values()) {
            baseSettingsController.resume();
        }*/
    }

    public void pause() {
        /*for (BaseSettingsController baseSettingsController : this.mSettingsControllerMap.values()) {
            baseSettingsController.pause();
        }*/
    }

    public void stop() {
        /*for (BaseSettingsController baseSettingsController : this.mSettingsControllerMap.values()) {
            baseSettingsController.stop();
        }*/
    }

}
