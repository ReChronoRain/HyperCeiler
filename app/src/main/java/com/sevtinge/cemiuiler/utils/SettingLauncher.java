package com.sevtinge.cemiuiler.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;

public class SettingLauncher {

    private final Context mContext;
    private boolean mLaunched;

    Bundle mExtras;
    Bundle mArguments;
    CharSequence mTitle;
    String mDestinationName;
    Class<?> mClass;

    int mTitleResId;

    public SettingLauncher(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must be non-null.");
        }
        mContext = context;
    }

    public SettingLauncher setClass(Class<?> cls) {
        mClass = cls;
        return this;
    }

    public SettingLauncher setDestination(String name) {
        mDestinationName = name;
        return this;
    }

    public SettingLauncher setTitleRes(int titleResId) {
        mTitleResId = titleResId;
        return this;
    }

    public SettingLauncher setTitleText(CharSequence title) {
        mTitle = title;
        return this;
    }

    public SettingLauncher setArguments(Bundle args) {
        mArguments = args;
        return this;
    }

    public SettingLauncher setExtras(Bundle extras) {
        mExtras = extras;
        return this;
    }

    public void launch() {
        if (mLaunched) {
            throw new IllegalStateException("This launcher has already been executed. Do not reuse");
        }
        mLaunched = true;
        Intent intent = toIntent();
        launch(intent);
    }

    public Intent toIntent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        copyExtras(intent);
        intent.setClass(mContext, mClass);
        if (TextUtils.isEmpty(mDestinationName)) {
            throw new IllegalArgumentException("Destination fragment must be set");
        }
        intent.putExtra(":settings:show_fragment", mDestinationName);
        intent.putExtra(":settings:show_fragment_args", mArguments);
        intent.putExtra(":settings:show_fragment_title", mTitle);
        intent.putExtra(":settings:show_fragment_title_resid", mTitleResId);

        /*intent.putExtra(":settings:show_fragment_contentResId", mContentResId);*/
        return intent;
    }

    void launch(Intent intent) {
        mContext.startActivity(intent);
    }

    void launchForResult(Fragment fragment, Intent intent, int i) {
        fragment.startActivityForResult(intent, i);
    }

    private void copyExtras(Intent intent) {
        if (mExtras != null) {
            intent.replaceExtras(mExtras);
        }
    }

}
