package com.sevtinge.hyperceiler.search;

import static androidx.core.content.ContextCompat.getDrawable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.dashboard.SubSettings;
import com.sevtinge.hyperceiler.search.data.ModEntity;
import com.sevtinge.hyperceiler.utils.AppIconCache;
import com.sevtinge.hyperceiler.utils.SettingLauncherHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fan.recyclerview.card.CardGroupAdapter;

public class SearchResultAdapter extends CardGroupAdapter<SearchResultAdapter.ModSearchViewHolder> {

    private final List<ModEntity> modsList = new ArrayList<>();
    private String filterString = "";
    private boolean isChina;
    private OnItemClickListener mItemClickListener;
    private final Map<String, Drawable> iconCache = new HashMap<>();

    public interface OnItemClickListener {
        void onItemClick(View view, ModEntity ad);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh(List<ModEntity> newData, String query, boolean isChina) {
        this.modsList.clear();
        if (newData != null) {
            this.modsList.addAll(newData);
        }
        this.filterString = query;
        this.isChina = isChina;
        notifyDataSetChanged();
        updateGroupInfo();
    }

    @NonNull
    @Override
    public ModSearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new ModSearchViewHolder(view);
    }

    @Override
    public void setHasStableIds() {

    }

    @Override
    public int getItemViewGroup(int position) {
        if (position < 0 || position >= modsList.size()) return 0;
        String group = modsList.get(position).groupName;
        if (group == null || group.isEmpty()) return 0;
        return group.hashCode();
    }

    private boolean isFirstInGroup(int position) {
        if (position == 0) return true;
        return getItemViewGroup(position) != getItemViewGroup(position - 1);
    }

    @Override
    public void onBindViewHolder(@NonNull ModSearchViewHolder holder, int position) {
        ModEntity ad = modsList.get(position);
        boolean firstInGroup = isFirstInGroup(position);
        holder.bind(ad, isChina, filterString, iconCache, firstInGroup, v -> {
            onItemClickListener(v.getContext(), ad);
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(v, ad);
            }
        });
    }

    private void onItemClickListener(Context context, ModEntity ad) {
        if (ad == null) return;
        Bundle args = new Bundle();
        args.putString(":settings:fragment_args_key", ad.key);
        args.putInt(":settings:fragment_resId", ad.xmlResId);
        SettingLauncherHelper.onStartSettingsForArguments(
            context,
            SubSettings.class,
            ad.fragment,
            args,
            ad.catTitleResId
        );
    }

    @Override
    public int getItemCount() {
        return modsList.size();
    }



    public static class ModSearchViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIcon;
        private final TextView mName;
        private final TextView mPackageName;

        public ModSearchViewHolder(View itemView) {
            super(itemView);
            mIcon = itemView.findViewById(android.R.id.icon);
            mName = itemView.findViewById(android.R.id.title);
            mPackageName = itemView.findViewById(android.R.id.summary);
        }

        public void bind(ModEntity ad, boolean isChina, String query,
                         Map<String, Drawable> iconCache, boolean isFirstInGroup,
                         View.OnClickListener listener) {
            mName.setText(getHighlightedText(ad.title, query, isChina));

            String bc = ad.breadcrumbs;
            if (bc != null && bc.contains("/")) {
                mPackageName.setText(bc);
                mPackageName.setVisibility(View.VISIBLE);
            } else {
                mPackageName.setVisibility(View.GONE);
            }

            if (isFirstInGroup) {
                String groupName = ad.groupName;
                if (groupName != null && !groupName.isEmpty()) {
                    mIcon.setTag(groupName);
                    Drawable icon = iconCache.get(groupName);
                    if (icon == null) {
                        // 有自定义图标的（非 ic_default）直接用定义的
                        icon = loadCustomIcon(groupName);
                        if (icon == null) {
                            String pkg = SearchHelper.getGroupPackageMap().get(groupName);
                            if (pkg != null) {
                                // 没有自定义图标时，优先复用按目标尺寸缓存过的应用图标
                                icon = AppIconCache.getCached(itemView.getContext(), pkg, getTargetIconSize());
                                if (icon == null) {
                                    // 异步加载真正的应用图标，先给一个占位避免列表闪烁
                                    mIcon.setImageResource(android.R.drawable.sym_def_app_icon);
                                    mIcon.setVisibility(View.VISIBLE);
                                    AppIconCache.loadIconAsync(itemView.getContext(), pkg, getTargetIconSize(), loadedIcon -> {
                                        Object currentTag = mIcon.getTag();
                                        if (!TextUtils.equals(groupName, currentTag instanceof CharSequence ? (CharSequence) currentTag : null)) {
                                            return;
                                        }
                                        if (loadedIcon != null) {
                                            iconCache.put(groupName, loadedIcon);
                                            mIcon.setImageDrawable(loadedIcon);
                                            mIcon.setVisibility(View.VISIBLE);
                                        } else {
                                            Drawable fallbackIcon = loadFallbackIcon(groupName);
                                            if (fallbackIcon != null) {
                                                iconCache.put(groupName, fallbackIcon);
                                                mIcon.setImageDrawable(fallbackIcon);
                                                mIcon.setVisibility(View.VISIBLE);
                                            } else {
                                                mIcon.setVisibility(View.INVISIBLE);
                                            }
                                        }
                                    });
                                }
                            }
                        }
                        // 兜底用分组默认图标
                        if (icon == null) {
                            icon = loadFallbackIcon(groupName);
                        }
                        if (icon != null) iconCache.put(groupName, icon);
                    }
                    if (icon != null) {
                        mIcon.setImageDrawable(icon);
                        mIcon.setVisibility(View.VISIBLE);
                    } else {
                        mIcon.setVisibility(View.INVISIBLE);
                    }
                } else {
                    mIcon.setTag(null);
                    mIcon.setVisibility(View.INVISIBLE);
                }
            } else {
                mIcon.setTag(null);
                mIcon.setVisibility(View.INVISIBLE);
            }

            itemView.setOnClickListener(listener);
        }

        private Drawable loadCustomIcon(String groupName) {
            Integer iconResId = SearchHelper.getGroupIconMap().get(groupName);
            if (iconResId != null && iconResId > 0 && iconResId != com.sevtinge.hyperceiler.core.R.drawable.ic_default) {
                try {
                    return getDrawable(itemView.getContext(), iconResId);
                } catch (Exception ignored) {}
            }
            return null;
        }

        private Drawable loadFallbackIcon(String groupName) {
            Integer iconResId = SearchHelper.getGroupIconMap().get(groupName);
            if (iconResId != null && iconResId > 0) {
                try {
                    return getDrawable(itemView.getContext(), iconResId);
                } catch (Exception ignored) {}
            }
            return null;
        }

        private int getTargetIconSize() {
            ViewGroup.LayoutParams params = mIcon.getLayoutParams();
            int size = params != null ? Math.max(params.width, params.height) : 0;
            if (size > 0) {
                return size;
            }
            return Math.round(itemView.getResources().getDisplayMetrics().density * 36);
        }

        private Spannable getHighlightedText(String text, String query, boolean isChina) {
            if (TextUtils.isEmpty(text)) {
                return new SpannableString("");
            }
            Spannable spannable = new SpannableString(text);
            if (TextUtils.isEmpty(query)) return spannable;

            String lText = text.toLowerCase(Locale.ROOT);
            String lQuery = query.toLowerCase(Locale.ROOT);

            if (isChina) {
                for (char c : lQuery.toCharArray()) {
                    int start = lText.indexOf(String.valueOf(c));
                    if (start >= 0) {
                        spannable.setSpan(new ForegroundColorSpan(SearchHelper.MARK_COLOR_VIBRANT),
                            start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            } else {
                int start = lText.indexOf(lQuery);
                if (start >= 0) {
                    spannable.setSpan(new ForegroundColorSpan(SearchHelper.MARK_COLOR_VIBRANT),
                        start, start + lQuery.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            return spannable;
        }
    }
}
