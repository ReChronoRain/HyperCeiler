/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.home.runtime;

import android.content.Intent;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import com.sevtinge.hyperceiler.libhook.IHomeConfigAgent;
import com.topjohnwu.superuser.internal.IRootServiceManager;
import com.topjohnwu.superuser.internal.Utils;
import com.topjohnwu.superuser.ipc.RootService;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;

/** Real pinned libsu manager and production client, with remote-process Binder fixtures. */
public final class ManagerIntegrationTest {
    static class Peer implements IBinder {
        final ArrayList<DeathRecipient> recipients=new ArrayList<>();
        public void linkToDeath(DeathRecipient d,int flags){recipients.add(d);}
        public boolean unlinkToDeath(DeathRecipient d,int flags){return recipients.remove(d);}
        void die(){for(var d:new ArrayList<>(recipients))d.binderDied();}
    }
    static class Agent extends Peer implements IHomeConfigAgent {
        volatile int refreshes,initializations,stops;
        public int protocolVersion(){return HomeLocalWire.CONTROL_VERSION;}
        public boolean initializeFramework(ParcelFileDescriptor fd){++initializations;return true;}
        public boolean initializeFromCallerMap(long a){throw new AssertionError();}
        public boolean isReady(){return true;}
        public void refresh(){++refreshes;}
        public void stop(){++stops;}
    }
    static class Manager extends Peer implements IRootServiceManager {
        final Agent agent=new Agent();int binds,unbinds;
        public IBinder asBinder(){return this;}
        public void connect(IBinder client){}
        public IBinder bind(Intent intent){++binds;return agent;}
        public void unbind(ComponentName name){++unbinds;}
        public void stop(ComponentName name,int uid){throw new AssertionError();}
    }
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    static void start(){HomeConfigRootClient.start(Utils.context);Handler.drain();}
    static void deliver(Manager manager){
        var bundle=new Bundle();bundle.putBinder("binder",manager);
        var intent=new Intent("com.topjohnwu.superuser.RECEIVER_BROADCAST").putExtra("extra.bundle",bundle).putExtra("extra.daemon",true);
        Utils.context.receiver.onReceive(Utils.context,intent);Handler.drain();
    }
    static void await(BooleanSupplier condition)throws Exception{
        for(int i=0;i<1000;++i){Handler.drain();if(condition.getAsBoolean())return;Thread.sleep(1);}
        throw new AssertionError("Worker did not complete");
    }
    public static void main(String[] args)throws Exception {
        start();check(RootService.launches==1);var manager=new Manager();
        switch(args[0]) {
            case "pending" -> {
                Handler.advance(15000);Handler.advance(600000);start();check(RootService.binds.size()==1&&RootService.launches==1);
                deliver(manager);await(()->manager.agent.refreshes==1);check(manager.binds==1);
                SystemProperties.backend="wms";start();await(()->manager.agent.stops==1);check(manager.unbinds==1);
            }
            case "late-disabled" -> {
                SystemProperties.backend="wms";start();deliver(manager);
                check(manager.binds==1&&manager.unbinds==1&&manager.agent.initializations==0);
            }
            case "late-new-owner" -> {
                SystemProperties.backend="wms";start();SystemProperties.backend="root-socket";start();
                check(RootService.launches==1&&RootService.binds.size()==2);deliver(manager);await(()->manager.agent.refreshes==1);
                check(manager.binds==1&&manager.unbinds==0&&manager.agent.stops==0);
                SystemProperties.backend="wms";start();await(()->manager.agent.stops==1);check(manager.unbinds==1);
            }
            case "death" -> {
                deliver(manager);await(()->manager.agent.refreshes==1);manager.die();manager.agent.die();Handler.drain();Handler.advance(500);
                check(RootService.launches==2&&RootService.binds.size()==2);var next=new Manager();deliver(next);await(()->next.agent.refreshes==1);
            }
            default -> throw new AssertionError(args[0]);
        }
        System.out.println("Production client + pinned libsu manager PASS "+args[0]);
    }
}
