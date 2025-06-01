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
package com.sevtinge.hyperceiler.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import fan.recyclerview.card.CardDefaultItemAnimator;
import fan.recyclerview.card.CardItemDecoration;
import fan.recyclerview.widget.RecyclerView;
import fan.springback.view.SpringBackLayout;

public class BaseRecyclerView extends RecyclerView {

    Context mContext;

    public BaseRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setHasFixedSize(true);
        setLayoutManager(new LinearLayoutManager(context));
        addItemDecoration(new CardItemDecoration(context));
        setItemAnimator(new CardDefaultItemAnimator());
    }
}
