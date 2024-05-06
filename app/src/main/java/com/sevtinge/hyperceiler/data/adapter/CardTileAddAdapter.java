package com.sevtinge.hyperceiler.data.adapter;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.data.viewholder.CardTileViewHolder;

import java.util.List;

public class CardTileAddAdapter extends CardTileBaseAdapter {

    public CardTileAddAdapter(List<String> data) {
        super(data);
    }

    @Override
    public void onBindViewHolder(@NonNull CardTileViewHolder holder, int position) {
        if (!mData.isEmpty()) {
            String tile = mData.get(position);
            holder.mIcon.setBackgroundResource(getCardIcon(holder.itemView.getContext(), tile));
            holder.mTitle.setText(getCardTitle(holder.itemView.getContext(), tile));
            holder.mTileMark.setChecked(false);
            holder.mTileMark.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if (isChecked) {
                    onDataChangeListener.onDataSetChanged(isChecked, tile);
                    notifyDataSetChanged();
                }
            });
        }
    }
}
