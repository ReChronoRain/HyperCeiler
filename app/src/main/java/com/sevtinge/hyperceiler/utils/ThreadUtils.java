package com.sevtinge.hyperceiler.utils;


import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadUtils {
    private static volatile ExecutorService sExecutorService;
    private static volatile Thread sMainThread;
    private static volatile Handler sMainThreadHandler;

    public static boolean isMainThread() {
        if (sMainThread == null) {
            sMainThread = Looper.getMainLooper().getThread();
        }
        return Thread.currentThread() == sMainThread;
    }

    public static Handler getUiThreadHandler() {
        if (sMainThreadHandler == null) {
            sMainThreadHandler = new Handler(Looper.getMainLooper());
        }
        return sMainThreadHandler;
    }

    public static void ensureMainThread() {
        if (!isMainThread()) {
            throw new RuntimeException("Must be called on the UI thread");
        }
    }

    // 使用 CompletableFuture
    public static CompletableFuture<Void> postOnBackgroundThread(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, getBackgroundExecutor());
    }

    // 支持有返回值的异步调用
    public static <T> CompletableFuture<T> postOnBackgroundThread(Callable<T> callable) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, getBackgroundExecutor());
    }

    public static void postOnMainThread(Runnable runnable) {
        getUiThreadHandler().post(runnable);
    }

    public static ExecutorService getBackgroundExecutor() {
        if (sExecutorService == null) {
            synchronized (ThreadUtils.class) {
                if (sExecutorService == null) {
                    sExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                }
            }
        }
        return sExecutorService;
    }

    public static void postOnMainThreadDelayed(Runnable runnable, long delayMillis) {
        getUiThreadHandler().postDelayed(runnable, delayMillis);
    }
}
