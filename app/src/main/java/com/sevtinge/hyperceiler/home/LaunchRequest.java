package com.sevtinge.hyperceiler.home;

import android.os.Bundle;
import android.os.UserHandle;

import androidx.fragment.app.Fragment;

public class LaunchRequest {
    Bundle mArguments;
    String mDestinationName;
    Bundle mExtras;
    int mFlags;
    boolean mIsSecondLayerPage;
    int mRequestCode;
    Fragment mResultListener;
    int mSourceMetricsCategory = 100;
    CharSequence mTitle;
    int mTitleResId;
    String mTitleResPackageName;
    int mTransitionType;
    UserHandle mUserHandle;

    int mInflatedXml;
}
