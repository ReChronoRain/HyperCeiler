package com.sevtinge.hyperceiler.controller;

import android.content.Context;
import android.widget.TextView;

import com.sevtinge.hyperceiler.ui.settings.core.BaseSettingsController;
import com.sevtinge.hyperceiler.utils.FileUtils;

public class TipsInfoController extends BaseSettingsController {

    private Context mContext;
    private TextView mTitle;

    public TipsInfoController(Context context, TextView textView) {
        super(context, textView);
        mContext = context;
    }

    public void setUpTextView(TextView textView) {
        mTitle = textView;
        updateStatus();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void updateStatus() {
        mTitle.setText("Tip: " + FileUtils.getRandomTip(mContext));
    }
}
