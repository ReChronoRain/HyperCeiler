package com.android.systemui.util.concurrency;
public class FakeExecutor implements DelayableExecutor {public final java.util.ArrayList<ExecutorImpl.ExecutionToken>tasks=new java.util.ArrayList<>();public ExecutorImpl.ExecutionToken executeDelayed(Runnable r,long delay){ExecutorImpl.ExecutionToken t=new ExecutorImpl.ExecutionToken(r,delay);tasks.add(t);return t;} }
