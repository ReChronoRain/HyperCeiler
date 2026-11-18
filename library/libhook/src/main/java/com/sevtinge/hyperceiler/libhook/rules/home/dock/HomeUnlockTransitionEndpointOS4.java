/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/** Window signals and renderer admission, no preference query or frame clock. */
public final class HomeUnlockTransitionEndpointOS4 implements AutoCloseable {
    // Module-owned versioned protocol, never a vendor transaction/address guess.
    public static final int TRANSACTION = 0x00484355;
    public static final int ACK = 0x48435531;
    public static final int VERSION = 1;
    public static final String LISTENER = "com.sevtinge.hyperceiler.home.UnlockTransition";
    private static final String WINDOW = "android.view.IWindowManager";
    private final Object lock = new Object();
    private final Consumer<String> diagnostic;
    private final ExecutorService dispatcher = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "hc-home-unlock");thread.setDaemon(true);return thread;
    });
    private volatile WeakReference<Object> window = new WeakReference<>(null);
    private int uid = -1, pid = -1;
    private Registration registration;
    private boolean visibilityKnown, appVisible;
    private volatile boolean closed, sourceReady, sourceUnavailable;
    private int visibilityReports;

    private final class Registration implements IBinder.DeathRecipient {
        final IBinder target;
        final int uid, pid;
        HomeDockBackendOS4.Lease dockLease;
        long dockEpoch;
        Registration(IBinder target, int uid, int pid) { this.target=target;this.uid=uid;this.pid=pid; }
        @Override public void binderDied() {
            synchronized(lock) { if(registration==this)registration=null; }
            releaseDock();
        }
        void releaseDock() {
            HomeDockBackendOS4.Lease old;
            synchronized(lock) {old=dockLease;dockLease=null;}
            if(old!=null)old.close();
        }
        void unlink() { releaseDock();target.unlinkToDeath(this, 0); }
        void dockOwnership(long epoch,boolean nativeOwner) {
            dispatch(() -> {
                synchronized(lock) {
                    if(closed||registration!=this||dockEpoch!=epoch)return;
                    if(!nativeOwner)dockLease=null;
                }
                Parcel data=Parcel.obtain();
                try {
                    data.writeInterfaceToken(LISTENER);data.writeLong(epoch);data.writeBoolean(nativeOwner);
                    target.transact(IBinder.FIRST_CALL_TRANSACTION+3,data,null,IBinder.FLAG_ONEWAY);
                } catch(RemoteException dead) {binderDied();}
                finally {data.recycle();}
            });
        }
        void visibility(boolean known,boolean visible) {
            Parcel data=Parcel.obtain();
            try {
                data.writeInterfaceToken(LISTENER);data.writeBoolean(known);data.writeBoolean(visible);
                target.transact(IBinder.FIRST_CALL_TRANSACTION+2,data,null,IBinder.FLAG_ONEWAY);
            } catch(RemoteException dead) {binderDied();}
            finally {data.recycle();}
        }
        void retired() {
            Parcel data=Parcel.obtain();
            try { data.writeInterfaceToken(LISTENER);target.transact(IBinder.FIRST_CALL_TRANSACTION+1,data,null,IBinder.FLAG_ONEWAY); }
            catch(RemoteException dead) { /* The native process already released its endpoint. */ }
            finally { data.recycle();unlink(); }
        }
    }

    public HomeUnlockTransitionEndpointOS4(Consumer<String> diagnostic) { this.diagnostic=diagnostic; }

    private void dispatch(Runnable task) {
        try { dispatcher.execute(task); }
        catch (RejectedExecutionException stopped) { if(!closed)throw stopped; }
    }

    public void ready() { sourceReady=true; }
    public void unavailable() { sourceUnavailable=true; }

    /** Called by traversal, possibly under WM locks. All Binder cleanup is deferred. */
    public void bind(Object owner, int newUid, int newPid) {
        if(newUid<10000||newPid<=0)return;
        Registration old;
        synchronized(lock) {
            if(closed||window.get()==owner&&uid==newUid&&pid==newPid)return;
            window=new WeakReference<>(owner);uid=newUid;pid=newPid;visibilityKnown=false;appVisible=false;
            old=registration;registration=null;
        }
        if(old!=null)dispatch(old::retired);
    }

    public void removed(Object owner) {
        Registration old;
        synchronized(lock) {
            if(window.get()!=owner)return;
            window.clear();uid=-1;pid=-1;old=registration;registration=null;
        }
        if(old!=null)dispatch(old::retired);
    }

    public boolean owns(Object owner) {return !closed&&window.get()==owner;}

    /** Platform visibility edge only, no traversal polling or renderer work. */
    public void visibility(Object owner,boolean visible) {
        Registration current;
        synchronized(lock) {
            if(closed||window.get()!=owner||visibilityKnown&&appVisible==visible)return;
            visibilityKnown=true;appVisible=visible;current=registration;
        }
        if(current!=null)dispatch(() -> {
            synchronized(lock) {if(closed||registration!=current)return;}
            current.visibility(true,visible);
            if(visibilityReports++<8)diagnostic.accept("window visibility published visible="+visible);
        });
    }

    /** Synchronous registration only, never called by a traversal or frame callback. */
    public void receive(Parcel data, Parcel reply, int flags) throws RemoteException {
        if((flags&IBinder.FLAG_ONEWAY)!=0)throw new IllegalArgumentException("Synchronous registration required");
        data.enforceInterface(WINDOW);
        int operation=data.readInt();
        if(operation<0||operation>4)throw new IllegalArgumentException("Unknown unlock operation");
        IBinder target=operation==1?data.readStrongBinder():null;
        long dockEpoch=operation>=3?data.readLong():0;
        if(operation>=3&&dockEpoch<=0)throw new IllegalArgumentException("Positive renderer epoch required");
        if(data.dataAvail()!=0)throw new IllegalArgumentException("Unexpected unlock payload");
        int callerUid=Binder.getCallingUid(),callerPid=Binder.getCallingPid();
        synchronized(lock) {
            boolean authorized=!closed&&sourceReady&&window.get()!=null&&uid==callerUid&&pid==callerPid;
            if(operation==0) {
                reply.writeNoException();reply.writeInt(ACK);reply.writeInt(VERSION);reply.writeInt(sourceUnavailable?-1:authorized?1:0);return;
            }
            if(!authorized)throw new SecurityException("Bound launcher window required");
            // Optional HCU1 operations, old requests and callback bodies stay
            // unchanged. A registered listener is the admission lifetime.
            if(operation==3||operation==4) {
                Registration current=registration;
                int available=0;
                if(current!=null) {
                    if(operation==4) {
                        if(current.dockEpoch==dockEpoch){current.releaseDock();available=1;}
                    }
                    else {
                        if(current.dockLease==null) {
                            current.dockEpoch=dockEpoch;
                            current.dockLease=HomeDockBackendOS4.INSTANCE.acquire(
                                window.get(),owned->current.dockOwnership(dockEpoch,owned));
                        }
                        if(current.dockEpoch==dockEpoch&&current.dockLease!=null&&current.dockLease.ready())available=1;
                    }
                }
                reply.writeNoException();reply.writeInt(ACK);reply.writeInt(VERSION);reply.writeInt(available);reply.writeLong(dockEpoch);return;
            }
        }
        Registration candidate=null;
        if(operation==1) {
            if(target==null||!LISTENER.equals(target.getInterfaceDescriptor()))
                throw new IllegalArgumentException("Unlock listener descriptor mismatch");
            candidate=new Registration(target,callerUid,callerPid);target.linkToDeath(candidate,0);
        }
        Registration old=null;boolean accepted;
        synchronized(lock) {
            accepted=!closed&&sourceReady&&window.get()!=null&&uid==callerUid&&pid==callerPid;
            if(accepted&&operation!=0){old=registration;registration=candidate;}
        }
        if(!accepted&&candidate!=null)candidate.unlink();
        if(old!=null)old.unlink();
        if(accepted&&candidate!=null) {
            Registration current=candidate;
            dispatch(() -> {
                boolean known,visible;
                synchronized(lock) {
                    if(closed||registration!=current)return;
                    known=visibilityKnown;visible=appVisible;
                }
                current.visibility(known,visible);
            });
        }
        reply.writeNoException();reply.writeInt(ACK);reply.writeInt(VERSION);reply.writeInt(accepted?1:0);
    }

    /** Capture the epoch before posting, so queue latency never changes animation phase. */
    public void transition(long uptimeMs, int flags) {
        Registration current;
        synchronized(lock) { if(closed||window.get()==null)return;current=registration; }
        if(current==null)return;
        dispatch(() -> {
            synchronized(lock) { if(closed||registration!=current||uid!=current.uid||pid!=current.pid)return; }
            Parcel data=Parcel.obtain();
            try {
                data.writeInterfaceToken(LISTENER);data.writeLong(uptimeMs);data.writeInt(flags);
                if(!current.target.transact(IBinder.FIRST_CALL_TRANSACTION,data,null,IBinder.FLAG_ONEWAY))
                    diagnostic.accept("early unlock listener rejected transition");
            } catch(RemoteException dead) { current.binderDied();diagnostic.accept("early unlock listener disconnected"); }
            finally { data.recycle(); }
        });
    }

    @Override public void close() {
        Registration old;
        synchronized(lock) { closed=true;window.clear();old=registration;registration=null; }
        if(old!=null)dispatch(old::retired);
        dispatcher.shutdown();
    }
}
