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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
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
