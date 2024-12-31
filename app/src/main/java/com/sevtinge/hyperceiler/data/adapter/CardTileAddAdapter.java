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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
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
