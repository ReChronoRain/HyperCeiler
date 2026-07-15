/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.model.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.model.viewholder.CardTileViewHolder;

import java.util.List;

public abstract class CardTileBaseAdapter extends RecyclerView.Adapter<CardTileViewHolder> {

    protected final List<String> mData;
    private OnDataChangeListener mOnDataChangeListener;

    public CardTileBaseAdapter(@NonNull List<String> data) {
        mData = data;
    }

    @NonNull
    @Override
    public CardTileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CardTileViewHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_card_tile, parent, false));
    }

    protected void bindTile(@NonNull CardTileViewHolder holder, int position, boolean selected) {
        String tile = mData.get(position);
        Context context = holder.itemView.getContext();
        holder.mIcon.setBackgroundResource(getCardIcon(context, tile));
        holder.mTitle.setText(getCardTitle(context, tile));
        holder.mTileMark.setOnCheckedChangeListener(null);
        holder.mTileMark.setChecked(selected);
        holder.mTileMark.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked == selected || mOnDataChangeListener == null) {
                return;
            }
            mOnDataChangeListener.onDataSetChanged(isChecked, tile);
            notifyDataSetChanged();
        });
    }

    private int getCardIcon(Context context, String tile) {
        String[] cardTileList = context.getResources().getStringArray(R.array.card_tile_list);
        for (int i = 0; i < cardTileList.length; i++) {
            if (cardTileList[i].equals(tile)) {
                return obtainTypedArray(context, R.array.card_tile_icon_list, i);
            }
        }
        return 0;
    }

    private String getCardTitle(Context context, String tile) {
        String[] cardTileList = context.getResources().getStringArray(R.array.card_tile_list);
        String[] cardTileTitleList = context.getResources().getStringArray(R.array.card_tile_title_list);
        for (int i = 0; i < cardTileList.length; i++) {
            if (cardTileList[i].equals(tile) && i < cardTileTitleList.length) {
                return cardTileTitleList[i];
            }
        }
        return tile;
    }

    private int obtainTypedArray(Context context, @ArrayRes int id, int position) {
        TypedArray typedArray = context.getResources().obtainTypedArray(id);
        try {
            return typedArray.getResourceId(position, 0);
        } finally {
            typedArray.recycle();
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setOnDataChangeListener(OnDataChangeListener listener) {
        mOnDataChangeListener = listener;
    }

    public interface OnDataChangeListener {
        void onDataSetChanged(boolean changed, String tile);
    }
}
