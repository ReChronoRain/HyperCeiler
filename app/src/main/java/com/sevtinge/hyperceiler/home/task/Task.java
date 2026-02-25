package com.sevtinge.hyperceiler.home.task;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * 任务抽象基类：支持依赖项阻塞和线程切换
 */
public abstract class Task implements Runnable {

    public final String id;
    private final boolean isAsync;
    private final List<String> depends = new ArrayList<>();
    private final CountDownLatch latch;

    public Task(String id, boolean isAsync, String... dependsOn) {
        this.id = id;
        this.isAsync = isAsync;
        if (dependsOn != null) {
            this.depends.addAll(Arrays.asList(dependsOn));
        }
        // 计数器大小为依赖任务的数量
        this.latch = new CountDownLatch(this.depends.size());
    }

    public boolean isAsync() {
        return isAsync;
    }

    public List<String> getDepends() {
        return depends;
    }

    // 依赖完成时减少计数
    public void satisfy() {
        latch.countDown();
    }

    @Override
    public final void run() {
        try {
            latch.await(); // 阻塞等待所有依赖项完成
            execute();
            TaskRunner.getInstance().notifyFinished(id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public abstract void execute();
}
