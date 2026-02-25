package com.sevtinge.hyperceiler.home;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;

import fan.core.utils.HyperMaterialUtils;
import fan.core.utils.RomUtils;

public class RestoreButtonViewHolder extends RecyclerView.ViewHolder {

    public TextView restoreButton;

    public RestoreButtonViewHolder(@NonNull View itemView) {
        super(itemView);
        restoreButton = itemView.findViewById(R.id.restore_button);
        setBackground(itemView);
    }

    public final void setBackground(View view) {
        if (view != null) {
            int bgResId = HyperMaterialUtils.isFeatureEnable(view.getContext()) && RomUtils.getHyperOsVersion() >= 2 ? R.drawable.item_card_bg_blur : R.drawable.item_card_bg;
            view.setBackground(view.getContext().getResources().getDrawable(bgResId));
        }
    }
}
