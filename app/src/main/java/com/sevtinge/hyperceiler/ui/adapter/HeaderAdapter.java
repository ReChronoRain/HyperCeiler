package com.sevtinge.hyperceiler.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.SettingsFeatures;
import com.sevtinge.hyperceiler.home.Header;
import com.sevtinge.hyperceiler.home.IconTitleLoader;
import com.sevtinge.hyperceiler.ui.page.BasePreferenceFragment;
import com.sevtinge.hyperceiler.utils.LargeFontUtils;

import java.util.List;

import fan.os.Build;

public class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder> {

    private Context mContext;
    private BasePreferenceFragment mFragment;

    private List<Header> mHeaders;

    private LayoutInflater mInflater;

    public HeaderAdapter(BasePreferenceFragment fragment, List<Header> headers) {
        mHeaders = headers;
        mFragment = fragment;
        mContext = fragment.getContext();
        mInflater = LayoutInflater.from(fragment.getContext());
    }

    public List<Header> getHeaders() {
        return mHeaders;
    }

    @NonNull
    @Override
    public HeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;
        switch (viewType) {
            case 0 ->
                view = mInflater.inflate(fan.preference.R.layout.miuix_preference_category_layout, parent, false);
            case 1 -> {
                view = mInflater.inflate(R.layout.miuix_preference_settings_main_layout, parent, false);
                setExtraParams(parent, view);
            }
        }
        view.setTag(Integer.valueOf(viewType));
        return new HeaderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HeaderViewHolder holder, int position) {
        if (position < 0 || position >= mHeaders.size()) return;

        Header header = getItem(position);
        View itemView = holder.itemView;
        Resources res = mContext.getResources();
        int headerType = getHeaderType(header);
        switch (headerType) {
            case 0 -> {
                holder.title.setText(header.getTitle(res));
                if (TextUtils.isEmpty(holder.title.getText())) {
                    holder.title.setVisibility(View.GONE);
                    itemView.setImportantForAccessibility(4);
                    itemView.setBackgroundResource(R.drawable.settings_preference_category_bg_no_title);
                } else {
                    holder.title.setVisibility(View.VISIBLE);
                    itemView.setBackgroundResource(
                        position == 0 ? R.drawable.settings_preference_category_bg_first :
                            R.drawable.settings_preference_category_bg
                    );
                }
            }
            case 1 -> {
                holder.title.setText(header.getTitle(res));
                CharSequence summary = header.getSummary(res);
                if (!TextUtils.isEmpty(summary)) {
                    holder.summary.setVisibility(View.VISIBLE);
                    holder.summary.setText(summary);
                } else {
                    holder.summary.setVisibility(View.GONE);
                }
            }
        }
        setIcon(holder, header);
        setEnable(holder, header);
        setClick(holder, header, position);
    }

    private void setExtraParams(ViewGroup parent, View view) {
        View arrowRight = view.findViewById(R.id.arrow_right);
        if (arrowRight != null) {
            arrowRight.setVisibility(View.VISIBLE);
        }
        view.setForeground(view.getContext().getTheme().obtainStyledAttributes(new int[]{fan.appcompat.R.attr.cardGroupItemForegroundEffect}).getDrawable(0));
    }

    public void setIcon(HeaderViewHolder holder, Header header) {
        if (holder != null) {
            ImageView iconView = holder.icon;
            if (iconView != null && iconView.getVisibility() != View.GONE) {
                if (header.iconRes != 0) {
                    iconView.setVisibility(View.VISIBLE);
                    iconView.setImageResource(header.iconRes);
                    iconView.setScaleType(ImageView.ScaleType.FIT_XY);
                    setIconAndTitle(holder, header);
                } else {
                    holder.icon.setVisibility(View.INVISIBLE);
                }
                if ((holder.icon.getDrawable() instanceof BitmapDrawable)) {
                    int headerIconSize = holder.icon.getResources().getDimensionPixelSize(R.dimen.header_icon_size);
                    iconView.setImageBitmap(createBitmap(iconView.getDrawable(), headerIconSize, headerIconSize));
                }
            }
        }
    }

    private void setEnable(HeaderViewHolder holder, Header header) {
        if (holder != null) {
            if (holder.title != null) {
                holder.title.setEnabled(true);
            }
            if (holder.summary != null) {
                holder.summary.setEnabled(true);
            }
        }
    }

    public void setClick(HeaderViewHolder holder, Header header, int position) {
        holder.itemView.setOnClickListener(v -> mFragment.onHeaderClick(header, position));
    }

    public static Bitmap createBitmap(Drawable drawable, int width, int height) {
        Bitmap createBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return createBitmap;
    }

    @Override
    public long getItemId(int position) {
        return mHeaders.get(position).hashCode();
    }

    @Override
    public int getItemCount() {
        if (mHeaders != null) {
            return mHeaders.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return getHeaderType(getItem(position));
    }

    public Header getItem(int position) {
        return mHeaders.get(position);
    }

    private int getHeaderType(Header header) {
        if (header.fragment == null && header.intent == null) {
            return 0;
        }
        return 1;
    }

    public void updateHeaderViewInfo() {}

    public void start() {}

    public void resume() {}

    public void pause() {}

    public void stop() {}

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView title;
        public TextView summary;
        public TextView value;
        public ImageView arrowRight;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            int viewType = (Integer) itemView.getTag();
            switch (viewType) {
                case 0 -> title = itemView.findViewById(android.R.id.title);
                case 1 -> {
                    arrowRight = itemView.findViewById(R.id.arrow_right);
                    if (arrowRight != null) {
                        arrowRight.setVisibility(View.VISIBLE);
                    }
                    icon = itemView.findViewById(android.R.id.icon);
                    title = itemView.findViewById(android.R.id.title);
                    summary = itemView.findViewById(android.R.id.summary);
                    value = itemView.findViewById(fan.preference.R.id.text_right);
                    if (icon != null && icon.getParent() != null) {
                        LinearLayout parent = (LinearLayout) icon.getParent();
                        icon.setMaxHeight(icon.getContext().getResources().getDimensionPixelSize(R.dimen.header_icon_size));
                        parent.setMinimumWidth(0);
                    }
                }
            }
        }
    }

    private void setIconAndTitle(HeaderViewHolder holder, Header header) {
        long id = header.id;
        if (holder == null || id == R.id.various || TextUtils.isEmpty(header.summary)) return;
        String packageName = header.summary.toString();
        // 精准更新 UI
        IconTitleLoader.load(mContext, packageName, (info) -> {
            holder.icon.setImageDrawable(info.icon);
            if (id != R.id.system_framework) {
                holder.title.setText(info.label);
            }
        });
    }
}
