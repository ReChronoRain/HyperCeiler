package com.sevtinge.hyperceiler.home.task;

import java.util.*;
import java.util.concurrent.*;

/**
 * 调度引擎：负责任务分发、去重和依赖通知
 */
public class TaskRunner {
    private static final TaskRunner INSTANCE = new TaskRunner();
    private final Map<String, Task> mTaskMap = new ConcurrentHashMap<>();
    private final Map<String, List<String>> mDependedBy = new ConcurrentHashMap<>();
    private final Set<String> mStartedTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ExecutorService mPool = Executors.newCachedThreadPool();

    private TaskRunner() {
    }

    public static TaskRunner getInstance() {
        return INSTANCE;
    }

    public void addTask(Task task) {
        if (mTaskMap.containsKey(task.id)) return;
        mTaskMap.put(task.id, task);
        for (String depId : task.getDepends()) {
            mDependedBy.computeIfAbsent(depId, k -> new ArrayList<>()).add(task.id);
        }
    }

    public void start() {
        for (Task task : mTaskMap.values()) {
            if (mStartedTasks.add(task.id)) { // 确保任务只启动一次
                if (task.isAsync()) mPool.execute(task);
                else task.run();
            }
        }
    }

    public void notifyFinished(String taskId) {
        List<String> dependents = mDependedBy.get(taskId);
        if (dependents != null) {
            for (String nextId : dependents) {
                Task next = mTaskMap.get(nextId);
                if (next != null) next.satisfy();
            }
        }
    }
}
