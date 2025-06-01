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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;

public class VersionCardClickView extends FrameLayout {

    View mVersionCardClickView;

    public VersionCardClickView(@NonNull Context context) {
        this(context, null);
    }

    public VersionCardClickView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VersionCardClickView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View.inflate(getContext(), R.layout.view_version_card_click, this);
        mVersionCardClickView = findViewById(R.id.version_card_click_view);
        ViewGroup.LayoutParams params = mVersionCardClickView.getLayoutParams();
        params.height = params.height - getContext().getResources().getDimensionPixelSize(fan.appcompat.R.dimen.miuix_appcompat_action_bar_default_height);
        mVersionCardClickView.setLayoutParams(params);
    }
}
