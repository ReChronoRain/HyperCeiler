/* SPDX-License-Identifier: AGPL-3.0-or-later */
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.SystemClock;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeUnlockTransitionEndpointOS4;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeDockBackendOS4;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class EndpointDeviceTest {
    private interface Checked { void run() throws Exception; }
    private static void rejects(Class<? extends Exception> expected, Checked action) throws Exception {
        try { action.run(); } catch (Exception actual) { if(expected.isInstance(actual))return;throw actual; }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }
    private static int request(HomeUnlockTransitionEndpointOS4 endpoint,int operation,IBinder listener,int flags,boolean extra) throws Exception {
        return request(endpoint,operation,listener,flags,extra,1);
    }
    private static int request(HomeUnlockTransitionEndpointOS4 endpoint,int operation,IBinder listener,int flags,boolean extra,long claim) throws Exception {
        Parcel data=Parcel.obtain(),reply=Parcel.obtain();
        try {
            data.writeInterfaceToken("android.view.IWindowManager");data.writeInt(operation);
            if(operation==1)data.writeStrongBinder(listener);
            if(operation>=3)data.writeLong(claim);
            if(extra)data.writeInt(42);
            data.setDataPosition(0);endpoint.receive(data,reply,flags);reply.setDataPosition(0);reply.readException();
            if(reply.readInt()!=HomeUnlockTransitionEndpointOS4.ACK||reply.readInt()!=HomeUnlockTransitionEndpointOS4.VERSION)throw new AssertionError("Protocol marker");
            int accepted=reply.readInt();if(operation>=3&&reply.readLong()!=claim)throw new AssertionError("Renderer epoch");
            if(reply.dataAvail()!=0)throw new AssertionError("Extra reply");return accepted;
        } finally { data.recycle();reply.recycle(); }
    }
    public static void main(String[] args) throws Exception {
        int uid=Process.myUid(),pid=Process.myPid();if(uid<10000)throw new AssertionError("Run under actual application UID");
        AtomicInteger notifications=new AtomicInteger();CountDownLatch delivered=new CountDownLatch(1),retired=new CountDownLatch(1),visible=new CountDownLatch(1),hidden=new CountDownLatch(1);
        CountDownLatch admitted=new CountDownLatch(1),released=new CountDownLatch(1);
        long epoch=SystemClock.uptimeMillis();
        Binder listener=new Binder() {
            { attachInterface(null,HomeUnlockTransitionEndpointOS4.LISTENER); }
            @Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags) {
                if(code==IBinder.FIRST_CALL_TRANSACTION+3) {
                    data.enforceInterface(HomeUnlockTransitionEndpointOS4.LISTENER);
                    long claim=data.readLong();boolean owned=data.readBoolean();if(claim<=0||data.dataAvail()!=0)throw new AssertionError("Ownership payload");
                    if(owned)admitted.countDown();else released.countDown();return true;
                }
                if(code==IBinder.FIRST_CALL_TRANSACTION+2) {
                    data.enforceInterface(HomeUnlockTransitionEndpointOS4.LISTENER);
                    boolean known=data.readBoolean(),shown=data.readBoolean();
                    if((!known&&shown)||data.dataAvail()!=0)throw new AssertionError("Visibility payload");
                    if(known){if(shown)visible.countDown();else hidden.countDown();}
                    return true;
                }
                if(code==IBinder.FIRST_CALL_TRANSACTION+1) {
                    data.enforceInterface(HomeUnlockTransitionEndpointOS4.LISTENER);
                    if(data.dataAvail()!=0)throw new AssertionError("Unexpected retirement payload");
                    retired.countDown();return true;
                }
                if(code!=IBinder.FIRST_CALL_TRANSACTION)return false;
                data.enforceInterface(HomeUnlockTransitionEndpointOS4.LISTENER);
                if(data.readLong()!=epoch||data.readInt()!=18||data.dataAvail()!=0)throw new AssertionError("Transition fields/order");
                notifications.incrementAndGet();delivered.countDown();return true;
            }
        };
        Object first=new Object(),second=new Object();
        AtomicReference<Consumer<Boolean>> commit=new AtomicReference<>();
        AtomicInteger resumed=new AtomicInteger();
        HomeDockBackendOS4.Legacy legacy=new HomeDockBackendOS4.Legacy() {
            public void retire(Object window,Consumer<Boolean> done) {commit.set(done);}
            public void resume(Object window) {resumed.incrementAndGet();}
        };
        HomeDockBackendOS4.INSTANCE.install(legacy);
        try(var endpoint=new HomeUnlockTransitionEndpointOS4(System.out::println)) {
            endpoint.bind(first,uid,pid);
            if(request(endpoint,0,null,0,false)!=0)throw new AssertionError("Source not ready");
            endpoint.ready();
            if(request(endpoint,0,null,0,false)!=1)throw new AssertionError("Bound identity");
            if(request(endpoint,3,null,0,false)!=0||HomeDockBackendOS4.INSTANCE.suppressed())throw new AssertionError("Admission without registered peer");
            rejects(IllegalArgumentException.class,()->request(endpoint,1,listener,IBinder.FLAG_ONEWAY,false));
            rejects(IllegalArgumentException.class,()->request(endpoint,0,null,0,true));
            if(request(endpoint,1,listener,0,false)!=1)throw new AssertionError("Register");
            rejects(IllegalArgumentException.class,()->request(endpoint,3,null,0,true));
            if(request(endpoint,3,null,0,false)!=0||admitted.await(60,TimeUnit.MILLISECONDS)||!HomeDockBackendOS4.INSTANCE.suppressed())
                throw new AssertionError("Admission before surface commit");
            commit.get().accept(true);
            if(!admitted.await(2,TimeUnit.SECONDS)||request(endpoint,3,null,0,false)!=1)throw new AssertionError("Committed admission");
            if(request(endpoint,4,null,0,false)!=1||!released.await(2,TimeUnit.SECONDS)||HomeDockBackendOS4.INSTANCE.suppressed())throw new AssertionError("Admission release");
            if(request(endpoint,3,null,0,false,2)!=0)throw new AssertionError("Second admission");
            commit.get().accept(true);
            if(request(endpoint,4,null,0,false,1)!=0||!HomeDockBackendOS4.INSTANCE.suppressed()
                ||request(endpoint,3,null,0,false,2)!=1)throw new AssertionError("Old release revoked new admission");
            rejects(IllegalArgumentException.class,()->request(endpoint,1,new Binder(),0,false));
            endpoint.visibility(first,true);
            if(!visible.await(2,TimeUnit.SECONDS))throw new AssertionError("Shown edge");
            endpoint.visibility(first,false);
            if(!hidden.await(2,TimeUnit.SECONDS))throw new AssertionError("Hidden edge");
            endpoint.transition(epoch,18);
            if(!delivered.await(2,TimeUnit.SECONDS)||notifications.get()!=1)throw new AssertionError("One transition");
            if(request(endpoint,2,null,0,false)!=1)throw new AssertionError("Unregister");
            if(HomeDockBackendOS4.INSTANCE.suppressed()||resumed.get()!=2)throw new AssertionError("Unregister retained renderer ownership");
            endpoint.transition(epoch,18);Thread.sleep(80);
            if(notifications.get()!=1)throw new AssertionError("Unregistered receiver notified");
            endpoint.bind(second,uid,pid);endpoint.removed(first);
            if(request(endpoint,0,null,0,false)!=1)throw new AssertionError("Old window removed new identity");
            endpoint.bind(second,uid+1,pid);
            rejects(SecurityException.class,()->request(endpoint,1,listener,0,false));
            endpoint.bind(second,uid,pid);
            if(request(endpoint,1,listener,0,false)!=1)throw new AssertionError("Re-register");
            endpoint.removed(second);
            if(!retired.await(2,TimeUnit.SECONDS))throw new AssertionError("Removed window did not retire receiver");
            if(request(endpoint,0,null,0,false)!=0)throw new AssertionError("Removed window accepted");
        } finally {HomeDockBackendOS4.INSTANCE.uninstall(legacy);}
        try(var unavailable=new HomeUnlockTransitionEndpointOS4(System.out::println)) {
            unavailable.unavailable();if(request(unavailable,0,null,0,false)!=-1)throw new AssertionError("Unsupported source must stop retries");
        }
        if(args.length!=0) {
            String[] names={"daybreak","depth_flip","gale","orbit_sweep","ripple","perspective_fold","capsule_fission","auto_aim"};
            if(args.length!=names.length)throw new AssertionError("Eight style wires required");
            for(int n=0;n<args.length;n++) {
                String hex=args[n];byte[] bytes=new byte[hex.length()/2];
                for(int i=0;i<bytes.length;i++)bytes[i]=(byte)Integer.parseInt(hex.substring(i*2,i*2+2),16);
                Parcel parcel=Parcel.obtain();
                try {
                    parcel.unmarshall(bytes,0,bytes.length);parcel.setDataPosition(0);
                    android.os.Bundle value=android.os.Bundle.CREATOR.createFromParcel(parcel);
                    if(value.size()!=2||value.getLong("startedAtMs")!=2147483981L||!names[n].equals(value.getString("style"))||parcel.dataAvail()!=0)
                        throw new AssertionError("Native epoch/style Bundle wire "+n);
                } finally {parcel.recycle();}
            }
            System.out.println("PASS eight NDK typed unlock requests decoded by real Android Bundle, signed long epoch and complete consumption");
        }
        System.out.println("PASS actual Android Binder/Parcel unlock endpoint, real UID authentication, ready/bind/replace/remove/unregister, async epoch/flags and invalid payload refusal");
    }
}
