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
