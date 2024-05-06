package com.sevtinge.hyperceiler.data.viewholder;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;

public class CardTileViewHolder extends RecyclerView.ViewHolder {

    public ImageView mIcon;
    public TextView mTitle;
    public CheckBox mTileMark;

    public CardTileViewHolder(@NonNull View itemView) {
        super(itemView);
        mIcon = itemView.findViewById(android.R.id.icon);
        mTitle = itemView.findViewById(android.R.id.title);
        mTileMark = itemView.findViewById(R.id.tile_mark);
    }
}
