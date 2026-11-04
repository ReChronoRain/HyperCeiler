/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.statusbarfollow;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.Executor;

/** Serializes retirement against callbacks already admitted on another thread. */
final class ServerBarFollowLifecycle {
    interface Restorer {
        /** Run on the native owner; call committed only after Surface restoration commits. */
        void restore(Runnable committed) throws Exception;
    }

    private enum State { ACTIVE, RETIRING, RESTORING, RETIRED }
    private static final class Lifetime {
        State state = State.ACTIVE;
        int entered;
        Executor owner;
        Restorer restorer;
        Throwable failure;
        Object attempt;
        boolean restoreReturned;
        boolean surfaceCommitted;
        Lifetime(Executor owner, Restorer restorer) { this.owner = owner; this.restorer = restorer; }
    }

    private static final class Tag {
        final WeakReference<Object> object;
        final Object controller;
        final Lifetime lifetime;
        Tag(Object object, Object controller, Lifetime lifetime) {
            this.object = new WeakReference<>(object);
            this.controller = controller;
            this.lifetime = lifetime;
        }
    }

    final class Permit implements AutoCloseable {
        private Lifetime lifetime;
        private Permit(Lifetime lifetime) { this.lifetime = lifetime; }
        @Override public void close() {
            Lifetime released;
            synchronized (ServerBarFollowLifecycle.this) {
                released = lifetime;
                lifetime = null;
                if (released == null) return;
                if (--released.entered < 0) throw new IllegalStateException("Unbalanced bar callback");
            }
            drain(released);
        }
    }

    private final IdentityHashMap<Object, Lifetime> controllers = new IdentityHashMap<>();
    private final List<Tag> tags = new ArrayList<>();
    private boolean reloadReserved;
    private long nextTagCleanup;
    private final ThreadLocal<Lifetime> restoring = new ThreadLocal<>();

    /** Gate synchronous native writes as well as listener/Runnable delivery. */
    synchronized Permit enterMutation(Object controller) {
        Lifetime current = controllers.get(controller);
        if (current == null) return new Permit(null);
        if (restoring.get() == current && current.state == State.RESTORING) return new Permit(null);
        if (reloadReserved || current.state != State.ACTIVE) return null;
        current.entered++;
        return new Permit(current);
    }

    synchronized void begin(Object controller, Executor owner, Restorer restorer) {
        if (controller == null || owner == null || restorer == null || reloadReserved)
            throw new IllegalStateException("Bar lifetime prerequisites unavailable");
        Lifetime previous = controllers.get(controller);
        if (previous != null && previous.state != State.RETIRED)
            throw new IllegalStateException("Previous bar lifetime has not committed retirement");
        controllers.put(controller, new Lifetime(owner, restorer));
    }

    /** Tag the actual listener/Runnable at construction, never when it is delivered. */
    synchronized void created(Object callback, Object controller) {
        if (callback == null || controller == null || !controllers.containsKey(controller)) return;
        // Construction runs on animation paths; amortize pruning rather than
        // scanning all live callbacks for every newly allocated listener.
        if (++nextTagCleanup >= 64) {
            tags.removeIf(tag -> tag.object.get() == null);
            nextTagCleanup = 0;
        }
        tags.add(new Tag(callback, controller, controllers.get(controller)));
    }

    /** Null denies a stale owned callback; unmanaged controllers retain stock behavior. */
    synchronized Permit enter(Object callback, Object controller) {
        if (controllers.isEmpty()) return new Permit(null);
        Lifetime current = controllers.get(controller);
        Tag found = null;
        for (int i = tags.size() - 1; i >= 0; i--) {
            Tag tag = tags.get(i);
            Object candidate = tag.object.get();
            if (candidate == null) { tags.remove(i); continue; }
            if (candidate == callback) {
                found = tag;
                break;
            }
        }
        if (current == null) return found != null && controllers.containsKey(found.controller)
                ? null : new Permit(null);
        if (current.state != State.ACTIVE || reloadReserved) return null;
        if (found != null && found.controller == controller && found.lifetime == current) {
            current.entered++;
            return new Permit(current);
        }
        // Includes callbacks created before hook installation. A managed
        // controller must never borrow the new lifetime at delivery time.
        return null;
    }

    void retire(Object controller) {
        Lifetime lifetime;
        synchronized (this) {
            lifetime = controllers.get(controller);
            if (lifetime == null || lifetime.state == State.RETIRED) return;
            if (lifetime.state == State.ACTIVE) lifetime.state = State.RETIRING;
        }
        drain(lifetime);
    }

    private void drain(Lifetime lifetime) {
        Executor executor;
        Object attempt;
        synchronized (this) {
            if (lifetime.state != State.RETIRING || lifetime.entered != 0) return;
            lifetime.state = State.RESTORING;
            executor = lifetime.owner;
            attempt = lifetime.attempt = new Object();
            lifetime.restoreReturned = false;
            lifetime.surfaceCommitted = false;
        }
        // Neither a native callback nor the owner's executor runs under our
        // lock. Retirement never waits while holding WMS's global lock.
        try {
            executor.execute(() -> {
                Lifetime previous = restoring.get();
                restoring.set(lifetime);
                try {
                    lifetime.restorer.restore(() -> {
                        synchronized (ServerBarFollowLifecycle.this) {
                            if (lifetime.state != State.RESTORING || lifetime.attempt != attempt) return;
                            lifetime.surfaceCommitted = true;
                            complete(lifetime);
                        }
                    });
                    synchronized (ServerBarFollowLifecycle.this) {
                        if (lifetime.state != State.RESTORING || lifetime.attempt != attempt) return;
                        lifetime.restoreReturned = true;
                        complete(lifetime);
                    }
                } catch (Throwable failure) { failed(lifetime, attempt, failure); }
                finally {
                    if (previous == null) restoring.remove(); else restoring.set(previous);
                }
            });
        } catch (Throwable failure) { failed(lifetime, attempt, failure); }
    }

    /** Both native bookkeeping and the compositor commit must have succeeded. Holds this lock. */
    private void complete(Lifetime lifetime) {
        if (!lifetime.restoreReturned || !lifetime.surfaceCommitted) return;
        lifetime.state = State.RETIRED;
        lifetime.owner = null;
        lifetime.restorer = null;
        lifetime.failure = null;
        lifetime.attempt = null;
        notifyAll();
    }

    private synchronized void failed(Lifetime lifetime, Object attempt, Throwable failure) {
        if (lifetime.state == State.RESTORING && lifetime.attempt == attempt) {
            lifetime.failure = failure;
            lifetime.state = State.RETIRING;
            lifetime.attempt = null;
            notifyAll();
        }
    }

    boolean reserveReload() {
        List<Object> owned;
        synchronized (this) {
            reloadReserved = true;
            owned = new ArrayList<>(controllers.keySet());
        }
        for (Object controller : owned) retire(controller);
        synchronized (this) {
            for (Lifetime lifetime : controllers.values())
                if (lifetime.state != State.RETIRED) return false;
            return true;
        }
    }

    /** Wait only for observed restore completion; elapsed time is never retirement proof. */
    synchronized boolean awaitReload(long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        for (;;) {
            boolean ready = true;
            for (Lifetime lifetime : controllers.values()) {
                if (lifetime.failure != null) return false;
                if (lifetime.state != State.RETIRED) ready = false;
            }
            if (ready) return reloadReserved;
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) return false;
            java.util.concurrent.TimeUnit.NANOSECONDS.timedWait(this, remaining);
        }
    }

    synchronized void cancelReloadReservation() { reloadReserved = false; }

    synchronized Object[] retiredControllers() {
        for (Lifetime lifetime : controllers.values()) if (lifetime.state != State.RETIRED)
            throw new IllegalStateException("Bar restoration is not committed");
        return controllers.keySet().toArray();
    }

    synchronized void restoreRetiredControllers(Object[] retired) {
        for (Object controller : retired) {
            if (controller == null || controllers.containsKey(controller)) continue;
            Lifetime lifetime = new Lifetime(null, null);
            lifetime.state = State.RETIRED;
            controllers.put(controller, lifetime);
        }
    }

    synchronized Object[][] retiredCallbacks() {
        retiredControllers();
        List<Object[]> retained = new ArrayList<>();
        for (Tag tag : tags) {
            Object callback = tag.object.get();
            if (callback != null && controllers.containsKey(tag.controller))
                retained.add(new Object[]{callback, tag.controller});
        }
        return retained.toArray(new Object[0][]);
    }

    synchronized void restoreRetiredCallbacks(Object[][] callbacks) {
        for (Object[] pair : callbacks) {
            if (pair == null || pair.length != 2 || pair[0] == null || !controllers.containsKey(pair[1])) continue;
            tags.add(new Tag(pair[0], pair[1], null));
        }
    }

    synchronized Throwable failure(Object controller) {
        Lifetime lifetime = controllers.get(controller);
        return lifetime == null ? null : lifetime.failure;
    }

    synchronized boolean isRetired(Object controller) {
        Lifetime lifetime = controllers.get(controller);
        return lifetime != null && lifetime.state == State.RETIRED;
    }
}
