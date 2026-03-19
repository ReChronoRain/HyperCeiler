package com.sevtinge.hyperceiler.provision.state;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.provision.activity.DefaultActivity;

import java.lang.reflect.InvocationTargetException;

import fan.provision.OobeUtils;

public class State {

    private static final String TAG = "State";
    public static final String PREFIX = "com.sevtinge.hyperceiler.provision.state.";

    protected Context mContext;

    protected StateMachine mStateMachine;
    protected String mPackageName;
    public String mClassName;
    public Class<?> mTargetClass;

    protected Handler mHandler = new Handler(Looper.getMainLooper());

    public boolean canBackTo() {
        return true;
    }

    public State getNextState() {
        return null;
    }

    public void onLeave() {}

    public static State create(String name) {
        try {
            return (State) Class.forName(PREFIX + name).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            Log.e(TAG, String.valueOf(e));
            return null;
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public State setPackageName(String packageName) {
        mPackageName = packageName;
        return this;
    }

    public State setClassName(String className) {
        mClassName = className;
        return this;
    }

    public State setTargetClass(Class<?> targetClass) {
        mTargetClass = targetClass;
        return this;
    }

    public State setStateMachine(StateMachine stateMachine) {
        mStateMachine = stateMachine;
        mContext = stateMachine.getContext();
        return this;
    }

    public void setStateContext(Context context) {
        mContext = context;
    }

    public void onEnter(boolean z, boolean z2) {
        onEnter(z, z2, null);
    }

    public void onEnter(boolean z, boolean z2, @Nullable Bundle activityOptions) {
        Log.d(TAG, "targetClass is " + mTargetClass);
        Intent intent = createEnterIntent(z, z2);
        launchIntent(intent, activityOptions);
    }

    protected Intent createEnterIntent(boolean canBack, boolean toNext) {
        Intent intent = getIntent();
        intent.putExtra("extra_disable_back", !canBack);
        intent.putExtra("extra_to_next", toNext);
        return intent;
    }

    protected void launchIntent(@NonNull Intent intent, @Nullable Bundle activityOptions) {
        if (mContext instanceof DefaultActivity activity) {
            activity.launchStateActivityForResult(intent, 0, activityOptions);
            return;
        }
        if (mContext instanceof Activity activity) {
            activity.startActivity(intent, activityOptions);
            return;
        }
        mContext.startActivity(intent);
    }

    public boolean isAvailable(boolean available) {
        return mContext.getPackageManager().resolveActivity(getIntent(), 0) != null;
    }

    protected Intent getIntent() {
        Intent intent = new Intent();
        if (TextUtils.isEmpty(mPackageName)) {
            intent.setClass(mContext, mTargetClass);
        } else {
            intent.setClassName(mPackageName, mClassName);
        }
        if (OobeUtils.isDebugOobeMode(mContext)) {
            intent.putExtra(OobeUtils.EXTRA_DEBUG_OOBE, true);
        }
        return intent;
    }

    public String getPageTag() {
        return "";
    }
}
