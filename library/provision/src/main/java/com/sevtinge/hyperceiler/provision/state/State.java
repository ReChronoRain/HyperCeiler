package com.sevtinge.hyperceiler.provision.state;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;

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
        Log.d(TAG, "targetClass is " + mTargetClass);
        Intent intent = getIntent();
        intent.putExtra("extra_disable_back", !z);
        intent.putExtra("extra_to_next", z2);
        ((Activity) mContext).startActivityForResult(intent, 0);
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
        return intent;
    }

    public String getPageTag() {
        return "";
    }
}
