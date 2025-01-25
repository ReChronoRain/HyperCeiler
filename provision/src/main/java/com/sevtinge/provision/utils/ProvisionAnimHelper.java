package com.sevtinge.provision.utils;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class ProvisionAnimHelper {

    private int mAnimY;
    private int mSkipOrNext = 0;

    private Context mContext;
    private Handler mHandler;
    private AnimListener mAnimListener;

    private IProvisionAnim mProxy;
    private IAnimCallback mCallback = new IAnimCallback.Stub() {
        @Override
        public void onNextAminStart() throws RemoteException {
            Log.d("OobeUtil2", "onNextAminStart: " + mSkipOrNext);
            if (mHandler != null) {
                mHandler.post(() -> {
                    if (mAnimListener != null) {
                        switch (mSkipOrNext) {
                            case 0 -> mAnimListener.onNextAminStart();
                            case 1 -> mAnimListener.onSkipAminStart();
                        }
                    }
                });
            }
        }

        @Override
        public void onBackAnimStart() throws RemoteException {
            Log.d("OobeUtil2", "onBackAnimStart");
            if (mHandler != null) {
                mHandler.postDelayed(() -> {
                    if (mAnimListener != null) {
                        mAnimListener.onBackAnimStart();
                    }
                }, 30L);
            }
        }
    };
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mProxy = IProvisionAnim.Stub.asInterface(service);
            try {
                mProxy.registerRemoteCallback(mCallback);
                mAnimListener.onAminServiceConnected();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equals("fan.action.PROVISION_ANIM_END") &&
                    mAnimListener != null) {
                mAnimListener.onAminEnd();
            }
        }
    };

    public interface AnimListener {

        void onAminServiceConnected();

        void onBackAnimStart();

        void onNextAminStart();

        void onSkipAminStart();

        void onAminEnd();
    }

    public ProvisionAnimHelper(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
    }

    public void setAnimListener(AnimListener listener) {
        mAnimListener = listener;
    }

    public boolean isAnimEnded() {
        try {
            return mProxy.isAnimEnd();
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public boolean goNextStep(int i) {
        try {
            mSkipOrNext = i;
            mProxy.playNextAnim(mAnimY);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean goBackStep() {
        try {
            mProxy.playBackAnim(mAnimY);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setAnimY(int i) {
        mAnimY = i;
    }

    public void registerAnimService() {
        if (mContext != null) {
            mContext.registerReceiver(mReceiver, new IntentFilter("fan.action.PROVISION_ANIM_END"), Context.RECEIVER_EXPORTED);
            Intent intent = new Intent("fan.intent.action.OOBSERVICE");
            intent.setPackage("com.sevtinge.hyperceiler");
            mContext.bindService(intent, mConnection, 1);
        } else {
            Log.e("OobeUtil2", "registerAnimService context is null");
        }
    }

    public void unregisterAnimService() {
        try {
            mProxy.unregisterRemoteCallback(mCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mContext != null) {
            mContext.unbindService(mConnection);
            mContext.unregisterReceiver(mReceiver);
        }
    }
}
