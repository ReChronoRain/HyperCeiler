package com.sevtinge.provision.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.sevtinge.provision.utils.IAnimCallback;
import com.sevtinge.provision.utils.IProvisionAnim;

import java.util.HashMap;

public class ProvisionAnimService extends Service {

    private static final String TAG = "ProvisionAnimService";
    public static HashMap<String, Integer> FAST_ANIM_MAP = new HashMap<>();
    IProvisionAnim.Stub stub = new IProvisionAnim.Stub() {

        private RemoteCallbackList<IAnimCallback> mListeners = new RemoteCallbackList<>();

        @Override
        public IBinder asBinder() {
            Log.d(TAG, "stub asBinder no anim");
            return super.asBinder();
        }

        @Override
        public boolean isAnimEnd() throws RemoteException {
            return true;
        }

        @Override
        public void playNextAnim(int i) throws RemoteException {
            Log.i(TAG, " without aim playNextAnim");
            dispatchVideoPlay(false);
        }

        @Override
        public void playBackAnim(int i) throws RemoteException {
            dispatchVideoPlay(true);
        }

        @Override
        public void registerRemoteCallback(IAnimCallback callback) throws RemoteException {
            if (callback != null) {
                mListeners.register(callback);
            }
        }

        @Override
        public void unregisterRemoteCallback(IAnimCallback callback) throws RemoteException {
            if (callback != null) {
                mListeners.unregister(callback);
            }
        }

        private void dispatchVideoPlay(boolean z) {
            try {
                int beginBroadcast = mListeners.beginBroadcast();
                for (int i = 0; i < beginBroadcast; i++) {
                    IAnimCallback callback = mListeners.getBroadcastItem(i);
                    if (z) {
                        callback.onBackAnimStart();
                    } else {
                        callback.onNextAminStart();
                    }
                }
                mListeners.finishBroadcast();
            } catch (RemoteException e) {
                Log.e(TAG, "Can not call IAnimCallback:", e);
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return stub.asBinder();
    }
}
