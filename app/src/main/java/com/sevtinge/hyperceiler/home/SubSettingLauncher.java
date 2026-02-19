package com.sevtinge.hyperceiler.home;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.annotation.XmlRes;
import androidx.fragment.app.Fragment;

import com.sevtinge.hyperceiler.dashboard.SubSettings;

public class SubSettingLauncher {

    private final Context mContext;
    private final LaunchRequest mLaunchRequest;
    private boolean mLaunched;

    class LaunchRequest {
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


    public SubSettingLauncher(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must be non-null.");
        }
        mContext = context;
        mLaunchRequest = new LaunchRequest();
        mLaunchRequest.mTransitionType = 0;
    }

    public SubSettingLauncher setDestination(String destination) {
        mLaunchRequest.mDestinationName = destination;
        return this;
    }

    public SubSettingLauncher setTitleRes(int titleRes) {
        return setTitleRes(null, titleRes);
    }

    public SubSettingLauncher setTitleRes(String packageName, int titleRes) {
        mLaunchRequest.mTitleResPackageName = packageName;
        mLaunchRequest.mTitleResId = titleRes;
        mLaunchRequest.mTitle = null;
        return this;
    }

    public SubSettingLauncher setTitleText(CharSequence title) {
        mLaunchRequest.mTitle = title;
        return this;
    }

    public SubSettingLauncher setArguments(Bundle args) {
        mLaunchRequest.mArguments = args;
        return this;
    }

    public SubSettingLauncher setInflatedXml(@XmlRes int inflatedXml) {
        mLaunchRequest.mInflatedXml = inflatedXml;
        return this;
    }

    public SubSettingLauncher setExtras(Bundle extras) {
        mLaunchRequest.mExtras = extras;
        return this;
    }

    public SubSettingLauncher setSourceMetricsCategory(int category) {
        mLaunchRequest.mSourceMetricsCategory = category;
        return this;
    }

    public SubSettingLauncher setResultListener(Fragment fragment, int requestCode) {
        mLaunchRequest.mRequestCode = requestCode;
        mLaunchRequest.mResultListener = fragment;
        return this;
    }

    public SubSettingLauncher addFlags(int flags) {
        mLaunchRequest.mFlags = flags | mLaunchRequest.mFlags;
        return this;
    }

    public SubSettingLauncher setUserHandle(UserHandle userHandle) {
        mLaunchRequest.mUserHandle = userHandle;
        return this;
    }

    public SubSettingLauncher setTransitionType(int type) {
        mLaunchRequest.mTransitionType = type;
        return this;
    }

    public SubSettingLauncher setIsSecondLayerPage(boolean isSecondLayerPage) {
        mLaunchRequest.mIsSecondLayerPage = isSecondLayerPage;
        return this;
    }

    public void launch() {
        launchWithIntent(toIntent());
    }

    public void launchWithIntent(Intent intent) {
        verifyIntent(intent);
        if (mLaunched) {
            throw new IllegalStateException("This launcher has already been executed. Do not reuse");
        }
        mLaunched = true;
        /*if (SettingsFeatures.isFoldDevice() && (this.mContext instanceof MiuiSettings)) {
            intent.addMiuiFlags(4);
        }*/
        Fragment fragment = mLaunchRequest.mResultListener;
        if (fragment != null) {
            launchForResultAsUser(intent, mLaunchRequest.mUserHandle, fragment, mLaunchRequest.mRequestCode);
            return;
        }
        if (fragment == null) {
            launchAsUser(intent, mLaunchRequest.mUserHandle);
        } else if (fragment != null) {
            launchForResult(fragment, intent, mLaunchRequest.mRequestCode);
        } else {
            launch(intent);
        }
    }

    public void verifyIntent(Intent intent) {
        String name = SubSettings.class.getName();
        ComponentName component = intent.getComponent();
        String stringExtra = intent.getStringExtra(":settings:show_fragment");
        int intExtra = intent.getIntExtra(":settings:source_metrics", -1);
        if (component != null && !TextUtils.equals(name, component.getClassName())) {
            throw new IllegalArgumentException(String.format("Class must be: %s", name));
        }
        if (TextUtils.isEmpty(stringExtra)) {
            throw new IllegalArgumentException("Destination fragment must be set");
        }
        if (intExtra < 0) {
            throw new IllegalArgumentException("Source metrics category must be set");
        }
    }

    public Intent toIntent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        copyExtras(intent);
        intent.setClass(mContext, SubSettings.class);
        if (TextUtils.isEmpty(mLaunchRequest.mDestinationName)) {
            throw new IllegalArgumentException("Destination fragment must be set");
        }
        intent.putExtra(":settings:show_fragment", mLaunchRequest.mDestinationName);
        int category = mLaunchRequest.mSourceMetricsCategory;
        if (category < 0) {
            throw new IllegalArgumentException("Source metrics category must be set");
        }

        if (mLaunchRequest.mInflatedXml != 0) {
            if (mLaunchRequest.mArguments == null) {
                mLaunchRequest.mArguments = new Bundle();
                mLaunchRequest.mArguments.putInt(":settings:fragment_resId", mLaunchRequest.mInflatedXml);
            } else {
                mLaunchRequest.mArguments.putInt(":settings:fragment_resId", mLaunchRequest.mInflatedXml);
            }
        }

        intent.putExtra(":settings:source_metrics", category);
        intent.putExtra(":settings:show_fragment_args", mLaunchRequest.mArguments);
        intent.putExtra(":settings:show_fragment_title_res_package_name", mLaunchRequest.mTitleResPackageName);
        intent.putExtra(":settings:show_fragment_title_resid", mLaunchRequest.mTitleResId);
        intent.putExtra(":settings:show_fragment_title", mLaunchRequest.mTitle);
        intent.addFlags(mLaunchRequest.mFlags);
        intent.putExtra("page_transition_type", mLaunchRequest.mTransitionType);
        intent.putExtra(":settings:is_second_layer_page", mLaunchRequest.mIsSecondLayerPage);
        return intent;
    }

    void launch(Intent intent) {
        mContext.startActivity(intent);
    }

    void launchAsUser(Intent intent, UserHandle userHandle) {
        mContext.startActivity(intent);
    }

    void launchForResultAsUser(Intent intent, UserHandle userHandle, Fragment fragment, int i) {
        fragment.getActivity().startActivityForResult(intent, i);
    }

    void launchForResult(Fragment fragment, Intent intent, int i) {
        fragment.startActivityForResult(intent, i);
    }

    private void copyExtras(Intent intent) {
        Bundle extras = mLaunchRequest.mExtras;
        if (extras != null) {
            intent.replaceExtras(extras);
        }
    }
}
