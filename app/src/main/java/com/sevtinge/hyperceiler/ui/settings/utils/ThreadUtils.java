package com.sevtinge.hyperceiler.ui.settings.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadUtils {

    private static volatile Thread sMainThread;
    private static volatile Handler sMainThreadHandler;
    private static volatile ExecutorService sThreadExecutor;

    /**
     * Returns true if the current thread is the UI thread.
     */
    public static boolean isMainThread() {
        if (sMainThread == null) {
            sMainThread = Looper.getMainLooper().getThread();
        }
        return Thread.currentThread() == sMainThread;
    }

    /**
     * Returns a shared UI thread handler.
     */
    @NonNull
    public static Handler getUiThreadHandler() {
        if (sMainThreadHandler == null) {
            sMainThreadHandler = new Handler(Looper.getMainLooper());
        }

        return sMainThreadHandler;
    }

    /**
     * Checks that the current thread is the UI thread. Otherwise throws an exception.
     */
    public static void ensureMainThread() {
        if (!isMainThread()) {
            throw new RuntimeException("Must be called on the UI thread");
        }
    }

    /**
     * Posts runnable in background using shared background thread pool.
     *
     * @return A future of the task that can be monitored for updates or cancelled.
     */
    public static Future<?> postOnBackgroundThread(@NonNull Runnable runnable) {
        return getThreadExecutor().submit(runnable);
    }

    /**
     * Posts callable in background using shared background thread pool.
     *
     * @return A future of the task that can be monitored for updates or cancelled.
     */
    public static <T> Future<T> postOnBackgroundThread(@NonNull Callable<T> callable) {
        return getThreadExecutor().submit(callable);
    }

    /**
     * Posts the runnable on the main thread.
     *
     * @deprecated moving work to the main thread should be done via the main executor provided to
     * {@link android.content.Context#getMainExecutor()} or by calling an SDK method such as
     * {@link android.app.Activity#runOnUiThread(Runnable)} or
     * {@link android.content.Context#getMainThreadHandler()} where appropriate.
     */
    @Deprecated
    public static void postOnMainThread(@NonNull Runnable runnable) {
        getUiThreadHandler().post(runnable);
    }

    /**
     * Provides a shared {@link ExecutorService} created using a fixed thread pool executor
     */
    @NonNull
    private static synchronized ExecutorService getThreadExecutor() {
        synchronized (ThreadUtils.class) {
            if (sThreadExecutor == null) {
                sThreadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            }
        }
        return sThreadExecutor;
    }
}