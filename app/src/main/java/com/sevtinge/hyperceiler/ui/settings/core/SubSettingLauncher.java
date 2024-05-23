package com.sevtinge.hyperceiler.ui.settings.core;

import static com.sevtinge.hyperceiler.ui.settings.SettingsActivity.EXTRA_IS_SECOND_LAYER_PAGE;
import static com.sevtinge.hyperceiler.ui.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT;
import static com.sevtinge.hyperceiler.ui.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS;
import static com.sevtinge.hyperceiler.ui.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE;
import static com.sevtinge.hyperceiler.ui.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID;
import static com.sevtinge.hyperceiler.ui.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME;
import static com.sevtinge.hyperceiler.ui.settings.core.SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE;
import static com.sevtinge.hyperceiler.ui.settings.core.SettingsBaseActivity.EXTRA_SOURCE_METRICS_CATEGORY;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.sevtinge.hyperceiler.ui.settings.SubSettings;

public class SubSettingLauncher {

    private final Context mContext;
    private final LaunchRequest mLaunchRequest;
    private boolean mLaunched;

    public SubSettingLauncher(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must be non-null.");
        }
        mContext = context;
        mLaunchRequest = new LaunchRequest();
        mLaunchRequest.mTransitionType = 0;
    }

    public SubSettingLauncher setDestination(String fragmentName) {
        mLaunchRequest.mDestinationName = fragmentName;
        return this;
    }

    /**
     * Set title with resource string id.
     *
     * @param titleResId res id of string
     */
    public SubSettingLauncher setTitleRes(@StringRes int titleResId) {
        return setTitleRes(null /*titlePackageName*/, titleResId);
    }

    /**
     * Set title with resource string id, and package name to resolve the resource id.
     *
     * @param titlePackageName package name to resolve resource
     * @param titleResId       res id of string, will use package name to resolve
     */
    public SubSettingLauncher setTitleRes(String titlePackageName, @StringRes int titleResId) {
        mLaunchRequest.mTitleResPackageName = titlePackageName;
        mLaunchRequest.mTitleResId = titleResId;
        mLaunchRequest.mTitle = null;
        return this;
    }

    /**
     * Set title with text,
     * This method is only for user generated string,
     * display text will not update after locale change,
     * if title string is from resource id, please use setTitleRes.
     *
     * @param title text title
     */
    public SubSettingLauncher setTitleText(CharSequence title) {
        mLaunchRequest.mTitle = title;
        return this;
    }

    public SubSettingLauncher setArguments(Bundle arguments) {
        mLaunchRequest.mArguments = arguments;
        return this;
    }

    public SubSettingLauncher setExtras(Bundle extras) {
        mLaunchRequest.mExtras = extras;
        return this;
    }

    public SubSettingLauncher setSourceMetricsCategory(int sourceMetricsCategory) {
        mLaunchRequest.mSourceMetricsCategory = sourceMetricsCategory;
        return this;
    }

    public SubSettingLauncher setResultListener(Fragment listener, int resultRequestCode) {
        mLaunchRequest.mRequestCode = resultRequestCode;
        mLaunchRequest.mResultListener = listener;
        return this;
    }

    public SubSettingLauncher addFlags(int flags) {
        mLaunchRequest.mFlags |= flags;
        return this;
    }

    public SubSettingLauncher setUserHandle(UserHandle userHandle) {
        mLaunchRequest.mUserHandle = userHandle;
        return this;
    }

    public SubSettingLauncher setTransitionType(int transitionType) {
        mLaunchRequest.mTransitionType = transitionType;
        return this;
    }

    /** Decide whether the next page is second layer page or not. */
    public SubSettingLauncher setIsSecondLayerPage(boolean isSecondLayerPage) {
        mLaunchRequest.mIsSecondLayerPage = isSecondLayerPage;
        return this;
    }

    public void launch() {
        if (mLaunched) {
            throw new IllegalStateException(
                    "This launcher has already been executed. Do not reuse");
        }
        mLaunched = true;
        final Intent intent = toIntent();

        boolean launchAsUser = false;
        boolean launchForResult = mLaunchRequest.mResultListener != null;
        if (!launchAsUser && launchForResult) {
            launchForResult(mLaunchRequest.mResultListener, intent, mLaunchRequest.mRequestCode);
        } else {
            launch(intent);
        }
    }

    public Intent toIntent() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        copyExtras(intent);
        intent.setClass(mContext, SubSettings.class);
        if (TextUtils.isEmpty(mLaunchRequest.mDestinationName)) {
            throw new IllegalArgumentException("Destination fragment must be set");
        }
        intent.putExtra(EXTRA_SHOW_FRAGMENT, mLaunchRequest.mDestinationName);

        if (mLaunchRequest.mSourceMetricsCategory < 0) {
            throw new IllegalArgumentException("Source metrics category must be set");
        }
        intent.putExtra(EXTRA_SOURCE_METRICS_CATEGORY,
                mLaunchRequest.mSourceMetricsCategory);

        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, mLaunchRequest.mArguments);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME,
                mLaunchRequest.mTitleResPackageName);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID,
                mLaunchRequest.mTitleResId);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE, mLaunchRequest.mTitle);
        intent.addFlags(mLaunchRequest.mFlags);
        intent.putExtra(EXTRA_PAGE_TRANSITION_TYPE,
                mLaunchRequest.mTransitionType);
        intent.putExtra(EXTRA_IS_SECOND_LAYER_PAGE,
                mLaunchRequest.mIsSecondLayerPage);

        return intent;
    }

    @VisibleForTesting
    void launch(Intent intent) {
        mContext.startActivity(intent);
    }

    @VisibleForTesting
    void launchForResult(Fragment listener, Intent intent, int requestCode) {
        listener.startActivityForResult(intent, requestCode);
    }

    private void copyExtras(Intent intent) {
        if (mLaunchRequest.mExtras != null) {
            intent.replaceExtras(mLaunchRequest.mExtras);
        }
    }

    /**
     * Simple container that has information about how to launch a subsetting.
     */
    static class LaunchRequest {
        String mDestinationName;
        int mTitleResId;
        String mTitleResPackageName;
        CharSequence mTitle;
        int mSourceMetricsCategory = 100;
        int mFlags;
        Fragment mResultListener;
        int mRequestCode;
        UserHandle mUserHandle;
        int mTransitionType;
        Bundle mArguments;
        Bundle mExtras;
        boolean mIsSecondLayerPage;
    }
}