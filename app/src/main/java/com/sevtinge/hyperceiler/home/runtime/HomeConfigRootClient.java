/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.home.runtime;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ParcelFileDescriptor;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.system.Os;
import org.lsposed.hiddenapibypass.HiddenApiBypass;
import java.io.FileDescriptor;
import java.io.IOException;
import androidx.annotation.Keep;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import com.sevtinge.hyperceiler.libhook.IHomeConfigAgent;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.Shell;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Keep
public final class HomeConfigRootClient {
    private static final String TAG="HyperCeiler.HomeLocal";
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static final ExecutorService worker=Executors.newSingleThreadExecutor(task -> {
        var thread=new Thread(task,"hc-home-root-client");thread.setDaemon(true);return thread;
    });
    private static final long[] RETRY_MS={500,2000,10000,30000};
    private static Connection current;
    private static Runnable retry;
    private static int failures;
    private static String lastFailure;
    public static final String BACKEND_PROPERTY = "debug.hyperceiler.home_config_backend";
    private HomeConfigRootClient() { }

    public static void start(Context context) {
        main.post(() -> connect(context.getApplicationContext()));
    }

    private static void connect(Context context) {
        String backend=backend();
        if(!"root-socket".equals(backend)&&!"root-probe".equals(backend)) {
            cancel(true);failures=0;lastFailure=null;return;
        }
        if(current!=null&&!current.backend.equals(backend))cancel(true);
        if(Boolean.FALSE.equals(Shell.isAppGrantedRoot())) {
            cancel(true);
            report("Root permission unavailable, automatic startup paused");return;
        }
        if(current!=null||retry!=null)return;
        var connection=new Connection(context,backend);
        current=connection;
        main.postDelayed(connection.timeout,15000);
        try {
            RootService.bind(connection.intent,connection);
        } catch (RuntimeException | LinkageError failure) {
            failed(connection,"Root startup failed: "+failure.getClass().getSimpleName(),true);
        }
    }

    private static String backend(){return android.os.SystemProperties.get(BACKEND_PROPERTY,"wms");}

    private static final class Connection implements ServiceConnection,IBinder.DeathRecipient {
        final Context context;
        final String backend;
        final Intent intent;
        final long startedAt=SystemClock.uptimeMillis();
        volatile String stage="awaiting-binder";
        IBinder binder;
        final Runnable timeout=() -> {
            if(current!=this)return;
            String reason="Root connection timed out, stage="+stage
                +" elapsed_ms="+(SystemClock.uptimeMillis()-startedAt);
            if(binder==null) {
                // libsu keeps both its pending bind and DAEMON_EN_ROUTE until the
                // bootstrap arrives. unbind cannot cancel either; rebinding here
                // only grows pendingTasks and cannot launch a second process.
                report(reason+", bootstrap still pending, duplicate launch suppressed");return;
            }
            failed(this,reason,true);
        };
        IHomeConfigAgent service;
        boolean deathLinked;
        volatile boolean compatible;
        volatile boolean cancelled;

        Connection(Context context,String backend) {
            this.context=context;this.backend=backend;
            intent=new Intent(context,HomeConfigRootService.class).addCategory(RootService.CATEGORY_DAEMON_MODE);
        }
        @Override public void onServiceConnected(ComponentName name,IBinder value) {
            if(current!=this) {
                // A cancelled libsu pending bind can still be delivered. Release
                // it after libsu's callback iteration without touching a new owner.
                main.post(() -> RootService.unbind(this));return;
            }
            main.removeCallbacks(timeout);main.postDelayed(timeout,15000);
            stage="protocol";
            binder=value;service=IHomeConfigAgent.Stub.asInterface(value);
            try {value.linkToDeath(this,0);deathLinked=true;}
            catch(RemoteException dead){main.post(() -> failed(this,"Root process died during connection",true));return;}
            worker.execute(() -> {
                try {
                    if(cancelled)return;
                    int version=service.protocolVersion();
                    if(version!=HomeLocalWire.CONTROL_VERSION) {
                        if(version>=5&&version<=9) {
                            // V5 through V9 have the same typed stop transaction. Retire the
                            // previous APK's daemon rather than rebinding it forever.
                            service.stop();
                            main.post(() -> failed(this,"Previous Root helper stopped for protocol upgrade",true));return;
                        }
                        main.post(() -> failed(this,"Root control protocol mismatch, automatic retry paused",false));return;
                    }
                    compatible=true;
                    stage="framework";
                    if(cancelled)return;
                    boolean framework=false;
                    try {framework=initializeFramework(service);}
                    catch(ReflectiveOperationException|IOException|android.system.ErrnoException|RuntimeException failure) {
                        ModuleLog.w(TAG,"Root framework initialization unavailable, feedback remains disabled",failure);
                    }
                    if(cancelled)return;
                    stage="refresh";
                    service.refresh();
                    final boolean initialized=framework;
                    main.post(() -> {
                        if(current!=this)return;
                        if(!backend.equals(backend())){cancel(true);return;}
                        stage="connected";main.removeCallbacks(timeout);failures=0;lastFailure=null;
                        ModuleLog.i(TAG,"Root runtime connected, framework_ready="+initialized);
                    });
                } catch(RemoteException dead) {main.post(() -> failed(this,"Root connection lost",true));}
                catch(RuntimeException|LinkageError failure) {
                    main.post(() -> failed(this,"Root initialization failed: "+failure.getClass().getSimpleName(),false));
                }
            });
        }
        @Override public void binderDied(){main.post(() -> failed(this,"Root process died",true));}
        // libsu iterates its connection map while delivering these callbacks.
        // Queue cleanup after it finishes; synchronous unbind invalidates its iterator.
        @Override public void onServiceDisconnected(ComponentName name){main.post(() -> failed(this,"Root service disconnected",true));}
        @Override public void onBindingDied(ComponentName name){main.post(() -> failed(this,"Root binding died",true));}
        @Override public void onNullBinding(ComponentName name){main.post(() -> failed(this,"Root returned no service, automatic retry paused",false));}
    }

    private static void failed(Connection connection,String reason,boolean recoverable) {
        if(current!=connection)return;
        cancel(false);
        report(reason);
        if(!recoverable||!"root-socket".equals(connection.backend)||!connection.backend.equals(backend())
            ||!Boolean.TRUE.equals(Shell.isAppGrantedRoot()))return;
        long delay=RETRY_MS[Math.min(failures++,RETRY_MS.length-1)];
        retry=() -> {retry=null;connect(connection.context);};
        main.postDelayed(retry,delay);
    }

    private static void cancel(boolean stop) {
        if(retry!=null){main.removeCallbacks(retry);retry=null;}
        Connection connection=current;current=null;
        if(connection==null)return;
        connection.cancelled=true;
        main.removeCallbacks(connection.timeout);
        if(connection.deathLinked)connection.binder.unlinkToDeath(connection,0);
        if(stop)worker.execute(() -> {
            if(connection.compatible)try {connection.service.stop();}catch(RemoteException ignored){}
        });
        try {RootService.unbind(connection);}
        catch(IllegalArgumentException ignored) { /* The bind task may not have registered yet. */ }
    }

    private static void report(String reason) {
        if(reason.equals(lastFailure))return;
        lastFailure=reason;ModuleLog.w(TAG,reason);
    }

    private static boolean initializeFramework(IHomeConfigAgent service)
            throws ReflectiveOperationException,IOException,RemoteException,android.system.ErrnoException {
        FileDescriptor descriptor=null;
        try {
            var type=Class.forName("com.android.internal.os.ApplicationSharedMemory");
            var instance=HiddenApiBypass.invoke(type,null,"getInstance");
            try {descriptor=(FileDescriptor)HiddenApiBypass.invoke(type,instance,"getReadOnlyFileDescriptor");}
            catch(java.lang.reflect.InvocationTargetException closed) {
                if(!(closed.getCause() instanceof IllegalStateException))throw closed;
                // ActivityThread may close its fd after mapping. The framework's
                // nonce address identifies that existing read-only mapping, not an offset.
                long nonce=((Number)HiddenApiBypass.invoke(type,instance,"getSystemNonceBlock")).longValue();
                return service.initializeFromCallerMap(nonce);
            }
            try(var shared=ParcelFileDescriptor.dup(descriptor)){return service.initializeFramework(shared);}
        } catch(ClassNotFoundException unsupported) {return service.initializeFramework(null);}
        finally {if(descriptor!=null)Os.close(descriptor);}
    }
}
