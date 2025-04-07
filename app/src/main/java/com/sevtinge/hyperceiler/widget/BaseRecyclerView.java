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
