/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.api;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 线程池管理器
 */
public class ThreadPoolManager {
    private static final int NUM_THREADS = 5;
    private static volatile ExecutorService executor;

    public static ExecutorService getInstance() {
        if (executor == null || executor.isShutdown()) {
            synchronized (ThreadPoolManager.class) {
                if (executor == null || executor.isShutdown()) {
                    executor = Executors.newFixedThreadPool(NUM_THREADS);
                }
            }
        }
        return executor;
    }

    public static void execute(Runnable task) {
        getInstance().execute(task);
    }

    public static Future<?> submit(Runnable task) {
        return getInstance().submit(task);
    }

    public static synchronized void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            executor = null;
        }
    }

    /**
     * 结束模块线程池，并明确返回线程是否真的已经退出。
     *
     * <p>热重载不能把仍可能持有旧 module classloader 的线程当作已清理；调用方必须在
     * 返回 {@code false} 时拒绝本次重载，而不是继续切换 generation。</p>
     */
    public static synchronized boolean shutdownAndAwait(long timeout, TimeUnit unit) {
        ExecutorService current = executor;
        if (current == null) {
            return true;
        }
        boolean terminated = false;
        if (!current.isShutdown()) {
            current.shutdown();
        }
        try {
            terminated = current.awaitTermination(timeout, unit);
            if (!terminated) {
                current.shutdownNow();
                terminated = current.awaitTermination(timeout, unit);
            }
        } catch (InterruptedException e) {
            current.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            executor = null;
        }
        return terminated;
    }
}
