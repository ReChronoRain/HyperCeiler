package com.sevtinge.hyperceiler.home;

import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;

import fan.core.utils.HyperMaterialUtils;
import fan.core.utils.RomUtils;

public class CustomOrderViewHolder extends RecyclerView.ViewHolder {

    public ImageButton moveButton;
    public TextView titleTextView;
    public CheckBox checkBox;

    public View mRootView;

    public CustomOrderViewHolder(@NonNull View itemView) {
        super(itemView);
        mRootView = itemView;
        titleTextView = itemView.findViewById(R.id.title_text_view);
        moveButton = itemView.findViewById(R.id.move_button);
        checkBox = itemView.findViewById(R.id.CheckBox);
        //moveButton.setAccessibilityDelegate(new DraggableCardAdapter$ViewHolder$1(this));
        /*if (FeaturedUiUtils.isFixedFontSize(this.mRootView.getContext())) {
            this.titleTextView.post(new DraggableCardAdapter$ViewHolder$2(this));
        }*/
    }

    public void bind(Header header) {
        titleTextView.setText(header.titleRes);
        checkBox.setChecked(header.displayStatus);
        setBackground(mRootView);
    }

    public final void setBackground(View view) {
        if (view != null) {
            int bgResId = HyperMaterialUtils.isFeatureEnable(view.getContext()) && RomUtils.getHyperOsVersion() >= 2 ? R.drawable.item_card_bg_blur : R.drawable.item_card_bg;
            view.setBackground(view.getContext().getResources().getDrawable(bgResId));
        }
    }
}
