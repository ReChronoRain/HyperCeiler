/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.net.Uri;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import java.util.Objects;

/** Real view geometry stays with its owner, recognition can consume one coherent snapshot elsewhere. */
final class HomeExtraGestureEndpointOS4 extends Binder {
    private static final Uri PROVIDER=Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs");
    private final Context context;
    private final Handler worker;
    private final Handler main;
    private final java.util.function.Consumer<Boolean> block;
    private final HomeExtraGestureHandoff handoff;
    interface Action {boolean send(int event,long time);}
    private final Action action;
    private final java.util.function.BooleanSupplier configCurrent;
    private final java.util.concurrent.atomic.AtomicReference<Permit> permit=new java.util.concurrent.atomic.AtomicReference<>();
    private Notice notice;
    private static final class Notice {
        final java.util.concurrent.atomic.AtomicBoolean revoked=new java.util.concurrent.atomic.AtomicBoolean();
    }
    private record Permit(IBinder token,HomeExtraGestureHandoff.Grant grant,int mask,Notice notice) {}
    private boolean wasBlocked;
    private IBinder owner;
    private IBinder.DeathRecipient ownerDied;
    private final Runnable expiry=this::syncOwner;
    private final int homeUid;
    private final long producer=SystemClock.elapsedRealtimeNanos();
    private volatile HomeExtraGestureWire.Snapshot snapshot;
    private volatile boolean closed;
    private volatile boolean requested;
    private Object lastOwner;
    private long ownerGeneration;
    private float slop;
    private int doubleTimeout,longTimeout,failures;
    private boolean invalidSnapshot;
    private IBinder registry;
    private final Runnable register=this::register;
    private final IBinder.DeathRecipient died;

    HomeExtraGestureEndpointOS4(Context context,Handler worker,Handler main,java.util.function.Consumer<Boolean> block,java.util.function.BooleanSupplier configCurrent,Action action) throws android.content.pm.PackageManager.NameNotFoundException {
        this.context=context;this.worker=worker;this.main=new Handler(main.getLooper());this.block=block;this.configCurrent=configCurrent;this.action=action;handoff=new HomeExtraGestureHandoff(producer);
        died=()->worker.post(()->{registry=null;if(!closed&&requested){failures=0;register();}});
        homeUid=context.getPackageManager().getApplicationInfo("com.miui.home",0).uid;
        attachInterface(null,HomeExtraGestureWire.DESCRIPTOR);
    }
    static boolean residentSelected() {
        return "1".equals(android.os.SystemProperties.get("debug.hyperceiler.home_extra_resident"));
    }
    static boolean handoffSelected() {
        return residentSelected()||android.os.SystemProperties.getBoolean("debug.hyperceiler.home_extra_handoff",false);
    }
    void request(boolean value) {
        if(requested==value)return;requested=value;if(!value)snapshot=null;
        worker.post(()->{worker.removeCallbacks(register);if(requested)register();});
    }
    void timing(float slop,int doubleTimeout,int longTimeout) {
        this.slop=slop;this.doubleTimeout=doubleTimeout;this.longTimeout=longTimeout;
    }
    void publish(HomeExtraGestureState.Geometry geometry,int mask,long revision,boolean ready) {
        if(closed)return;
        if(!ready||revision<=0||slop<=0){snapshot=null;updateOwner();return;}
        HomeExtraGestureState.Geometry value=null;
        if(geometry==null)lastOwner=null;
        else {
            if(!Objects.equals(lastOwner,geometry.owner())){lastOwner=geometry.owner();ownerGeneration++;}
            value=new HomeExtraGestureState.Geometry(geometry.width(),geometry.height(),geometry.rotation(),geometry.density(),
                geometry.navTop(),geometry.corner(),geometry.handle(),ownerGeneration);
        }
        try {
            snapshot=new HomeExtraGestureWire.Snapshot(producer,revision,SystemClock.uptimeMillis(),mask,slop,doubleTimeout,longTimeout,value);
            invalidSnapshot=false;
        } catch(IllegalArgumentException unsupported) {
            snapshot=null;
            if(!invalidSnapshot)ModuleLog.w("HyperCeiler.ExtraGesture","navigation snapshot unavailable",unsupported);
            invalidSnapshot=true;
        }
        updateOwner();
    }
    void close() {
        closed=true;requested=false;snapshot=null;lastOwner=null;updateOwner();
        worker.post(()->{
            worker.removeCallbacks(register);
            if(registry!=null){registry.unlinkToDeath(died,0);registry=null;}
        });
    }
    @Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags) throws RemoteException {
        if(code==HomeExtraGestureWire.ACTION)return action(data,reply,flags);
        if(code==HomeExtraGestureWire.CONTROL)return control(data,reply,flags);
        if(code!=HomeExtraGestureWire.QUERY)return super.onTransact(code,data,reply,flags);
        if((flags&IBinder.FLAG_ONEWAY)!=0||reply==null)return false;
        int uid=Binder.getCallingUid();if(uid!=homeUid&&uid!=0)throw new SecurityException("Launcher reader required");
        data.enforceInterface(HomeExtraGestureWire.DESCRIPTOR);
        if(data.readInt()!=HomeExtraGestureWire.VERSION)return false;
        data.enforceNoDataAvail();
        var current=snapshot;
        HomeExtraGestureWire.write(writer(reply),current,closed);
        return true;
    }
    private void updateOwner() {
        boolean enabled=!closed&&requested&&handoffSelected();
        handoff.update(snapshot,enabled,SystemClock.uptimeMillis());syncOwner();
    }
    private void syncOwner() {
        long now=SystemClock.uptimeMillis();boolean blocked=handoff.blocked(now);
        if(blocked!=wasBlocked){wasBlocked=blocked;block.accept(blocked);}
        var current=handoff.current(now);
        if(current!=null&&owner!=null&&notice!=null&&!notice.revoked.get()&&configCurrent.getAsBoolean())
            permit.set(new Permit(owner,current,snapshot.mask(),notice));
        else {
            invalidate();var held=handoff.held();
            if(held!=null&&owner!=null&&notice!=null)revoke(new Permit(owner,held,0,notice));
        }
        if(owner!=null&&!handoff.owns(owner)) {owner.unlinkToDeath(ownerDied,0);owner=null;ownerDied=null;notice=null;}
        main.removeCallbacks(expiry);long at=handoff.expiry(now);if(at>0)main.postAtTime(expiry,at);
    }
    // Called on the preference worker before the corresponding Main publication.
    void invalidate() {
        revoke(permit.getAndSet(null));
    }
    private void revoke(Permit previous) {
        if(previous==null||!previous.notice().revoked.compareAndSet(false,true))return;
        Parcel data=Parcel.obtain();
        try {
            data.writeInterfaceToken(HomeExtraGestureWire.OWNER_DESCRIPTOR);data.writeInt(HomeExtraGestureWire.CONTROL_VERSION);
            data.writeLong(producer);data.writeLong(previous.grant().serial());
            previous.token().transact(HomeExtraGestureWire.REVOKE,data,null,IBinder.FLAG_ONEWAY);
        } catch(RemoteException unavailable) {
            ModuleLog.w("HyperCeiler.ExtraGesture","native owner invalidation delivery unavailable",unavailable);
        } finally {data.recycle();}
    }
    private boolean action(Parcel data,Parcel reply,int flags) {
        if(reply==null||(flags&IBinder.FLAG_ONEWAY)!=0)return false;
        if(Binder.getCallingUid()!=homeUid)throw new SecurityException("Launcher owner required");
        data.enforceInterface(HomeExtraGestureWire.DESCRIPTOR);
        if(data.readInt()!=HomeExtraGestureWire.CONTROL_VERSION)return false;
        IBinder token=data.readStrongBinder();long source=data.readLong(),revision=data.readLong(),serial=data.readLong();
        int event=data.readInt();long time=data.readLong();data.enforceNoDataAvail();
        if(token==null||event<9||event>12)return false;
        java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean result=new java.util.concurrent.atomic.AtomicBoolean();
        worker.post(()->{
            Permit current=permit.get();long now=SystemClock.uptimeMillis();
            if(!closed&&configCurrent.getAsBoolean()&&current!=null&&current.token()==token&&!current.notice().revoked.get()
                &&HomeExtraGestureWire.allows(current.grant(),current.mask(),source,revision,serial,event,time,now))
                result.set(action.send(event,time));
            done.countDown();
        });
        boolean completed=false;
        try {completed=done.await(HomeExtraGestureHandoff.DRAIN_MS,java.util.concurrent.TimeUnit.MILLISECONDS);}
        catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
        reply.writeInt(HomeExtraGestureWire.ACTION_ACK);reply.writeInt(completed&&result.get()?1:0);return true;
    }
    private static final class Request {
        final java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);
        volatile boolean cancelled;
        volatile Response response=new Response(HomeExtraGestureWire.DENIED,null);
        long attempt;
        void finish(int status,HomeExtraGestureHandoff.Grant grant) {
            if(!cancelled)response=new Response(status,grant);
            done.countDown();
        }
    }
    private record Response(int status,HomeExtraGestureHandoff.Grant grant) {}
    private boolean control(Parcel data,Parcel reply,int flags) {
        if(reply==null||(flags&IBinder.FLAG_ONEWAY)!=0)return false;
        if(Binder.getCallingUid()!=homeUid)throw new SecurityException("Launcher owner required");
        data.enforceInterface(HomeExtraGestureWire.DESCRIPTOR);
        if(data.readInt()!=HomeExtraGestureWire.CONTROL_VERSION)return false;
        int operation=data.readInt();IBinder token=data.readStrongBinder();
        long expected=data.readLong(),revision=data.readLong(),serial=data.readLong();data.enforceNoDataAvail();
        if(token==null||operation<HomeExtraGestureWire.TAKE||operation>HomeExtraGestureWire.RELEASE)return false;
        Request request=new Request();
        main.post(()->{
            if(request.cancelled){request.done.countDown();return;}
            updateOwner();long now=SystemClock.uptimeMillis();
            if(expected!=producer){request.done.countDown();return;}
            if(operation==HomeExtraGestureWire.RELEASE) {
                boolean released=handoff.release(token,serial,revision);
                syncOwner();request.finish(released?HomeExtraGestureWire.RELEASED:HomeExtraGestureWire.DENIED,null);return;
            }
            if(operation==HomeExtraGestureWire.RENEW) {
                var grant=handoff.renew(token,serial,revision,now);
                syncOwner();request.finish(grant==null?HomeExtraGestureWire.DENIED:HomeExtraGestureWire.OWNED,grant);return;
            }
            if(serial!=0){request.done.countDown();return;}
            request.attempt=handoff.begin(token,expected,revision,now);
            if(request.attempt==0){request.done.countDown();return;}
            IBinder.DeathRecipient died=()->main.post(()->{handoff.died(token);syncOwner();});
            try {token.linkToDeath(died,0);owner=token;ownerDied=died;notice=new Notice();}
            catch(RemoteException dead){handoff.cancel(request.attempt);syncOwner();request.done.countDown();return;}
            syncOwner(); // Retire the old receiver and invalidate queued touch actions on Main.
            worker.post(()->main.post(()->{
                // All previously enqueued old-backend IPC has returned before this marker.
                HomeExtraGestureHandoff.Grant grant=null;
                if(request.cancelled)handoff.cancel(request.attempt);
                else grant=handoff.drained(token,request.attempt,SystemClock.uptimeMillis());
                syncOwner();request.finish(grant==null?HomeExtraGestureWire.DENIED:HomeExtraGestureWire.OWNED,grant);
            }));
        });
        boolean complete=false;
        try {complete=request.done.await(HomeExtraGestureHandoff.DRAIN_MS,java.util.concurrent.TimeUnit.MILLISECONDS);}
        catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
        if(!complete) {
            request.cancelled=true;
            main.post(()->{handoff.cancel(request.attempt);syncOwner();});
        }
        Response response=complete?request.response:new Response(HomeExtraGestureWire.DENIED,null);
        HomeExtraGestureWire.writeGrant(writer(reply),response.status(),response.grant());return true;
    }
    private static HomeExtraGestureWire.Writer writer(Parcel reply) {
        return new HomeExtraGestureWire.Writer(){
            @Override public void integer(int value){reply.writeInt(value);}
            @Override public void wide(long value){reply.writeLong(value);}
            @Override public void scalar(float value){reply.writeFloat(value);}
        };
    }
    private void register() {
        if(closed||!requested||registry!=null)return;
        try {
            Bundle input=new Bundle();input.putBinder("endpoint",this);
            Bundle result=context.getContentResolver().call(PROVIDER,"hc_home_extra_register",null,input);
            IBinder lifetime=result==null?null:result.getBinder("lifetime");
            if(lifetime==null||!result.getBoolean("available"))throw new IllegalStateException("Gesture registry unavailable");
            lifetime.linkToDeath(died,0);registry=lifetime;failures=0;
            ModuleLog.i("HyperCeiler.ExtraGesture","navigation snapshot endpoint registered");
        } catch(RuntimeException|RemoteException unavailable) {
            if(closed||!requested)return;
            int[] delays={500,2000,10000,30000};
            if(failures<3)ModuleLog.w("HyperCeiler.ExtraGesture","navigation snapshot registry pending",unavailable);
            int delay=delays[Math.min(failures++,delays.length-1)];
            worker.removeCallbacks(register);worker.postDelayed(register,delay);
        }
    }
}
