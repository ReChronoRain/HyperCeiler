/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import java.util.function.Consumer;

/** One launcher renderer, with native admission after legacy surface retirement commits. */
public final class HomeDockBackendOS4 {
    public static final HomeDockBackendOS4 INSTANCE = new HomeDockBackendOS4();

    public interface Legacy {
        void retire(Object window, Consumer<Boolean> committed);
        void resume(Object window);
    }

    private final Object lock = new Object();
    private Legacy legacy;
    private Lease current;

    public final class Lease implements AutoCloseable {
        private final Object window;
        private final Legacy renderer;
        private final Consumer<Boolean> changed;
        private boolean ready;

        private Lease(Object window, Legacy renderer, Consumer<Boolean> changed) {
            this.window=window;this.renderer=renderer;this.changed=changed;
        }

        public boolean ready() { synchronized(lock) {return current==this&&ready;} }

        private void committed(boolean success) {
            synchronized(lock) {
                if(current!=this)return;
                if(success)ready=true;
                else current=null;
            }
            if(!success)renderer.resume(window);
            changed.accept(success);
        }

        @Override public void close() {
            synchronized(lock) {
                if(current!=this)return;
                current=null;
            }
            renderer.resume(window);
            changed.accept(false);
        }
    }

    public void install(Legacy renderer) {
        synchronized(lock) {
            if(legacy!=null)throw new IllegalStateException("Legacy Dock controller already installed");
            legacy=renderer;
        }
    }

    public void uninstall(Legacy renderer) {
        Lease old;
        synchronized(lock) {
            if(legacy!=renderer)return;
            legacy=null;old=current;
        }
        if(old!=null)old.close();
    }

    /** Pending admission also suppresses legacy creation while retirement is queued. */
    public boolean suppressed() { synchronized(lock) {return current!=null;} }

    public Object window() { synchronized(lock) {return current==null?null:current.window;} }

    public Lease acquire(Object window, Consumer<Boolean> changed) {
        Lease lease;
        synchronized(lock) {
            if(legacy==null||current!=null)return null;
            lease=new Lease(window,legacy,changed);current=lease;
        }
        // Retirement can enqueue WMS work; never call it under our state lock.
        lease.renderer.retire(window,lease::committed);
        return lease;
    }
}
