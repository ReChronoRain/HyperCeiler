/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ThreadUtils {

    private ThreadUtils() {
    }

    public static boolean isMainThread() {
        return Thread.currentThread() == MainThreadHolder.THREAD;
    }

    public static Handler getUiThreadHandler() {
        return MainThreadHolder.HANDLER;
    }

    public static void ensureMainThread() {
        if (!isMainThread()) {
            throw new RuntimeException("Must be called on the UI thread");
        }
    }

    public static CompletableFuture<Void> postOnBackgroundThread(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, getBackgroundExecutor());
    }

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
        MainThreadHolder.HANDLER.post(runnable);
    }

    public static ExecutorService getBackgroundExecutor() {
        return BackgroundExecutorHolder.INSTANCE;
    }

    public static void postOnMainThreadDelayed(Runnable runnable, long delayMillis) {
        MainThreadHolder.HANDLER.postDelayed(runnable, delayMillis);
    }

    private static final class MainThreadHolder {
        private static final Looper LOOPER = Looper.getMainLooper();
        private static final Thread THREAD = LOOPER.getThread();
        private static final Handler HANDLER = new Handler(LOOPER);
    }

    private static final class BackgroundExecutorHolder {
        private static final ExecutorService INSTANCE = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
    }
}
