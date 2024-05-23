package com.sevtinge.hyperceiler.ui.fragment.settings.dashboard;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.sevtinge.hyperceiler.ui.fragment.settings.utils.ThreadUtils;

import java.util.concurrent.CountDownLatch;

/**
 * Observer for updating injected dynamic data.
 */
public abstract class DynamicDataObserver extends ContentObserver {

    private Runnable mUpdateRunnable;
    private CountDownLatch mCountDownLatch;
    private boolean mUpdateDelegated;

    protected DynamicDataObserver() {
        super(new Handler(Looper.getMainLooper()));
        mCountDownLatch = new CountDownLatch(1);
        // Load data for the first time
        onDataChanged();
    }

    /** Returns the uri of the callback. */
    public abstract Uri getUri();

    /** Called when data changes. */
    public abstract void onDataChanged();

    /** Calls the runnable to update UI */
    public synchronized void updateUi() {
        mUpdateDelegated = true;
        if (mUpdateRunnable != null) {
            mUpdateRunnable.run();
        }
    }

    /** Returns the count-down latch */
    public CountDownLatch getCountDownLatch() {
        return mCountDownLatch;
    }

    @Override
    public void onChange(boolean selfChange) {
        onDataChanged();
    }

    protected synchronized void post(Runnable runnable) {
        if (mUpdateDelegated) {
            ThreadUtils.postOnMainThread(runnable);
        } else {
            mUpdateRunnable = runnable;
            mCountDownLatch.countDown();
        }
    }
}