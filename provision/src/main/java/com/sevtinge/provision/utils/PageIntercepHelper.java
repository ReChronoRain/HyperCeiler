package com.sevtinge.provision.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;

import com.sevtinge.provision.R;
import com.sevtinge.provision.activity.BaseActivity;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class PageIntercepHelper {

    private static final String TAG = "PageIntercepHelper";

    private Callback mCallback;
    private BroadcastReceiver mReceiver;

    public static final Map<Class, Integer> mActivityCode = new HashMap<>();

    static {
        //mActivityCode.put(LanguagePickerActivity.class, 10000);
    }

    public static PageIntercepHelper getInstance() {
        return new PageIntercepHelper();
    }

    public interface Callback {
        void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);
    }

    public void register(Context context) {
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive");
                int requestCode = intent.getIntExtra("placeholderCode", 0);
                int resultCode = intent.getIntExtra("resultCode", 0);
                Log.d(TAG, "placeHoderCodde:" + requestCode);
                if (mCallback != null) {
                    mCallback.onActivityResult(requestCode, resultCode, intent);
                }
                Class<?> adapterClass = getAdapterClass(requestCode);
                Log.d(TAG, "adapterClass:" + adapterClass);
                if (adapterClass != null) {
                    Activity activity = LifecycleHandler.getInstance().getActivity(adapterClass);
                    Log.d(TAG, "activity:" + activity);
                    if (activity != null) {
                        activity.overridePendingTransition(R.anim.provision_slide_in_right, R.anim.provision_slide_out_left);
                    }
                    new Handler().postDelayed(() -> {
                        if (activity instanceof BaseActivity) {
                            ((BaseActivity) activity).setCheck(false);
                        }
                        LifecycleHandler.getInstance().finishActivity(adapterClass);
                    }, resultCode == 0 ? 0L : 400L);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ProvisionJump");
        LocalBroadcastHelper.getInstance(context).registerReceiver(mReceiver, intentFilter);
    }

    public void unregisterReceiver(Context context) {
        if (mReceiver != null) {
            LocalBroadcastHelper.getInstance(context).unregisterReceiver(mReceiver);
        }
    }


    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public boolean isIngoreCode(int code) {
        for (Class<?> clazz : mActivityCode.keySet()) {
            if (code == mActivityCode.get(clazz).intValue()) {
                return true;
            }
        }
        return false;
    }

    public Class<?> getAdapterClass(int requestCode) {
        for (Class<?> clazz : mActivityCode.keySet()) {
            if (requestCode == mActivityCode.get(clazz).intValue()) {
                return clazz;
            }
        }
        return null;
    }

    public boolean isAdapterNewJump(Activity activity) {
        if (activity == null) return false;
        int resultCode = getActivityResultCode(activity);
        boolean isAdapterNewJump = resultCode != -1073741823 && resultCode != 0 && mActivityCode != null && mActivityCode.containsKey(activity.getClass());
        Log.d("PageIntercepHelper", "isAdapterNewJump:" + isAdapterNewJump);
        return isAdapterNewJump;

    }

    public void sendFinish(Activity activity) {
        Log.d("PageIntercepHelper", "sendFinish:" + activity);
        activity.overridePendingTransition(R.anim.provision_slide_in_right, R.anim.provision_slide_out_left);
    }

    public void finish(Activity activity) {
        activity.overridePendingTransition(R.anim.provision_slide_in_left, R.anim.provision_slide_out_right);
    }

    public int getPlaceHolderCode(Activity activity) {
        return mActivityCode.get(activity.getClass()).intValue();
    }

    private int getActivityResultCode(Activity activity) {
        try {
            Field field = Activity.class.getDeclaredField("mResultCode");
            field.setAccessible(true);
            return ((Integer) field.get(activity)).intValue();
        } catch (Exception e) {
            Log.e("PageIntercepHelper", "mResultCode get error", e);
            return -1073741823;
        }
    }
}
