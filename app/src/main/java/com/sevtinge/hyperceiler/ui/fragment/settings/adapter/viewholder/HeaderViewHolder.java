package com.sevtinge.hyperceiler.ui.fragment.settings.adapter.viewholder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.settings.utils.SettingsFeatures;

public class HeaderViewHolder extends RecyclerView.ViewHolder {

    public ImageView icon;
    public TextView title;
    public TextView summary;
    public TextView value;
    public ImageView arrowRight;

    public HeaderViewHolder(@NonNull View itemView) {
        super(itemView);
        int viewType = (Integer) itemView.getTag();
        if (viewType == 0) {
            title = itemView.findViewById(android.R.id.title);
        } else {
            arrowRight = itemView.findViewById(R.id.arrow_right);
            if (viewType == 1 || viewType == 2 || viewType == 3 || viewType == 5 || viewType == 6) {
                if (arrowRight != null) {
                    if (SettingsFeatures.isSplitTablet(itemView.getContext())) {
                        arrowRight.setVisibility(View.INVISIBLE);
                    } else {
                        arrowRight.setVisibility(View.VISIBLE);
                    }
                }
                icon = itemView.findViewById(android.R.id.icon);
                title = itemView.findViewById(android.R.id.title);
                summary = itemView.findViewById(android.R.id.summary);
                value = itemView.findViewById(R.id.text_right);
                if (icon != null && icon.getParent() != null) {
                    LinearLayout linearLayout = (LinearLayout) icon.getParent();
                    icon.setMinimumWidth(icon.getContext().getResources().getDimensionPixelSize(R.dimen.header_icon_size));
                    linearLayout.setMinimumWidth(0);
                    ((ViewGroup.MarginLayoutParams) icon.getLayoutParams()).setMarginEnd(0);
                    ((ViewGroup.MarginLayoutParams) linearLayout.getLayoutParams()).setMarginEnd(0);
                }
            } else {
                if (arrowRight != null && SettingsFeatures.isSplitTablet(itemView.getContext())) {
                    arrowRight.setVisibility(View.INVISIBLE);
                }
            }
        }
    }
}
