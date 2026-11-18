/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.home.runtime;

import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import com.sevtinge.hyperceiler.libhook.IHomeConfigAgent;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.BooleanSupplier;

public final class RecoveryTest {
    private static final Context context=new Context();
    private static final class Agent extends IHomeConfigAgent.Stub {
        final ArrayList<IBinder.DeathRecipient> deaths=new ArrayList<>();
        volatile int refreshes,stops,initializations;
        int version=HomeLocalWire.CONTROL_VERSION;
        boolean framework=true;
        boolean dieOnLink;
        CountDownLatch entered,release;
        volatile String thread;
        public int protocolVersion(){
            if(entered!=null){entered.countDown();try{release.await();}catch(InterruptedException e){throw new AssertionError(e);}}
            return version;
        }
        public boolean initializeFramework(ParcelFileDescriptor fd){++initializations;thread=Thread.currentThread().getName();return framework;}
        public boolean initializeFromCallerMap(long address){throw new AssertionError();}
        public boolean isReady(){return true;}
        public void refresh(){++refreshes;}
        public void stop(){++stops;}
        public void linkToDeath(IBinder.DeathRecipient r,int flags)throws RemoteException {if(dieOnLink)throw new RemoteException();deaths.add(r);}
        public boolean unlinkToDeath(IBinder.DeathRecipient r,int flags){check(deaths.contains(r));return deaths.remove(r);}
        void die(){for(var death:new ArrayList<>(deaths))death.binderDied();}
    }
    private static void check(boolean ok){if(!ok)throw new AssertionError();}
    private static void start(){HomeConfigRootClient.start(context);Handler.drain();}
    private static ServiceConnection last(){return RootService.binds.getLast();}
    private static void await(BooleanSupplier condition)throws Exception {
        for(int i=0;i<1000;++i){Handler.drain();if(condition.getAsBoolean())return;Thread.sleep(1);}
        throw new AssertionError("Worker result did not arrive");
    }
    private static Agent connected()throws Exception {
        Agent agent=new Agent();last().onServiceConnected(null,agent);await(()->agent.refreshes==1);
        await(()->ModuleLog.messages.stream().anyMatch(m->m.startsWith("Root runtime connected")));
        check(!Thread.currentThread().getName().equals(agent.thread));return agent;
    }
    public static void main(String[] args)throws Exception {
        String test=args[0];
        if(test.equals("denied")) {
            Shell.grant=false;start();Handler.advance(300000);check(RootService.binds.isEmpty());
            Shell.grant=true;start();check(RootService.binds.size()==1);
            System.out.println("Production Root client recovery PASS denied");return;
        }
        if(test.equals("probe"))SystemProperties.backend="root-probe";
        start();check(RootService.binds.size()==1);
        switch(test) {
            case "late-disabled" -> {
                var old=last();SystemProperties.backend="wms";start();int releases=RootService.unbinds;
                var agent=new Agent();RootService.dispatching=true;old.onServiceConnected(null,agent);
                check(RootService.unbinds==releases);RootService.dispatching=false;Handler.drain();
                check(RootService.unbinds==releases+1&&RootService.released.getLast()==old);
                check(agent.initializations==0&&agent.refreshes==0&&agent.deaths.isEmpty());
            }
            case "late-new-session" -> {
                var old=last();old.onServiceDisconnected(null);Handler.drain();Handler.advance(500);
                var active=last();var agent=connected();int releases=RootService.unbinds;
                RootService.dispatching=true;old.onServiceConnected(null,agent);RootService.dispatching=false;Handler.drain();
                check(RootService.unbinds==releases+1&&RootService.released.getLast()==old);
                check(agent.refreshes==1&&agent.stops==0&&agent.deaths.size()==1);
                start();check(RootService.binds.size()==2&&last()==active);
            }
            case "pending-timeout" -> {
                Handler.advance(15000);Handler.advance(300000);start();
                check(RootService.binds.size()==1&&RootService.unbinds==0);
                check(ModuleLog.messages.stream().anyMatch(m->m.contains("awaiting-binder")));
                connected();Handler.advance(300000);check(RootService.binds.size()==1);
            }
            case "late-framework-timeout" -> {
                Handler.advance(15000);Handler.advance(300000);
                var agent=new Agent();agent.entered=new CountDownLatch(1);agent.release=new CountDownLatch(1);
                last().onServiceConnected(null,agent);agent.entered.await();Handler.advance(14999);
                check(RootService.unbinds==0);Handler.advance(1);check(RootService.unbinds==1);
                Handler.advance(500);check(RootService.binds.size()==2);agent.release.countDown();connected();
            }
            case "framework-timeout" -> {
                var agent=new Agent();agent.entered=new CountDownLatch(1);agent.release=new CountDownLatch(1);
                last().onServiceConnected(null,agent);agent.entered.await();Handler.advance(15000);
                check(RootService.unbinds==1&&ModuleLog.messages.stream().anyMatch(m->m.contains("protocol")));
                Handler.advance(500);check(RootService.binds.size()==2);agent.release.countDown();
                connected();check(agent.refreshes==0);
            }
            case "callback-reentrancy" -> {
                connected();var previous=last();RootService.dispatching=true;
                previous.onServiceDisconnected(null);previous.onBindingDied(null);previous.onNullBinding(null);
                check(RootService.unbinds==0);RootService.dispatching=false;Handler.drain();
                check(RootService.unbinds==1);Handler.advance(500);check(RootService.binds.size()==2);
                var dead=new Agent();dead.dieOnLink=true;RootService.dispatching=true;
                last().onServiceConnected(null,dead);check(RootService.unbinds==1);RootService.dispatching=false;Handler.drain();
                check(RootService.unbinds==2);Handler.advance(2000);check(RootService.binds.size()==3);
            }
            case "connected" -> {start();check(RootService.binds.size()==1);connected();Handler.advance(15000);check(RootService.unbinds==0);}
            case "death" -> {
                var agent=connected();var previous=last();agent.die();Handler.drain();previous.onServiceDisconnected(null);
                start();Handler.advance(499);check(RootService.binds.size()==1);Handler.advance(1);check(RootService.binds.size()==2);
                connected();previous.onServiceDisconnected(null);Handler.advance(30000);check(RootService.binds.size()==2);
            }
            case "backoff" -> {
                long[] delays={500,2000,10000,30000,30000};
                for(int i=0;i<delays.length;++i){last().onServiceDisconnected(null);Handler.drain();Handler.advance(delays[i]-1);check(RootService.binds.size()==i+1);Handler.advance(1);check(RootService.binds.size()==i+2);}
                check(ModuleLog.messages.size()==1);
            }
            case "dead-link" -> {var agent=new Agent();agent.dieOnLink=true;last().onServiceConnected(null,agent);Handler.drain();Handler.advance(500);check(RootService.binds.size()==2);}
            case "timeout" -> {Handler.advance(14999);check(ModuleLog.messages.isEmpty());Handler.advance(1);check(RootService.unbinds==0);Handler.advance(500);check(RootService.binds.size()==1);}
            case "revoked" -> {
                var agent=connected();Shell.grant=false;start();await(()->agent.stops==1);agent.die();Handler.drain();Handler.advance(60000);check(RootService.binds.size()==1);
                Shell.grant=true;start();check(RootService.binds.size()==2);
            }
            case "probe" -> {var agent=connected();agent.die();Handler.drain();Handler.advance(60000);check(RootService.binds.size()==1);}
            case "protocol" -> {
                var agent=new Agent();agent.version++;last().onServiceConnected(null,agent);await(()->RootService.unbinds==1);
                Handler.advance(300000);check(RootService.binds.size()==1&&agent.initializations==0);
            }
            case "upgrade", "upgrade6", "upgrade7", "upgrade8", "upgrade9" -> {
                var agent=new Agent();agent.version=switch(args[0]) {case "upgrade9" -> 9;case "upgrade8" -> 8;case "upgrade7" -> 7;case "upgrade6" -> 6;default -> 5;};last().onServiceConnected(null,agent);
                await(()->RootService.unbinds==1);check(agent.stops==1&&agent.initializations==0);
                Handler.advance(499);check(RootService.binds.size()==1);
                Handler.advance(1);check(RootService.binds.size()==2);
            }
            case "null" -> {last().onNullBinding(null);Handler.advance(300000);check(RootService.binds.size()==1);}
            case "disabled" -> {
                var agent=connected();SystemProperties.backend="wms";start();await(()->agent.stops==1);
                agent.die();Handler.drain();Handler.advance(300000);check(RootService.binds.size()==1);
            }
            case "disabled-retry" -> {last().onServiceDisconnected(null);SystemProperties.backend="wms";start();Handler.advance(60000);check(RootService.binds.size()==1);}
            case "stale" -> {
                var agent=new Agent();agent.entered=new CountDownLatch(1);agent.release=new CountDownLatch(1);
                last().onServiceConnected(null,agent);agent.entered.await();SystemProperties.backend="wms";start();agent.release.countDown();
                await(()->agent.stops==1);check(agent.refreshes==0&&agent.initializations==0);
                Handler.advance(300000);check(RootService.binds.size()==1);
            }
            case "partial" -> {
                var agent=new Agent();agent.framework=false;last().onServiceConnected(null,agent);await(()->agent.refreshes==1);
                await(()->ModuleLog.messages.stream().anyMatch(m->m.contains("framework_ready=false")));
                Handler.advance(60000);check(RootService.binds.size()==1);
            }
            default -> throw new AssertionError(test);
        }
        System.out.println("Production Root client recovery PASS "+test);
    }
}
