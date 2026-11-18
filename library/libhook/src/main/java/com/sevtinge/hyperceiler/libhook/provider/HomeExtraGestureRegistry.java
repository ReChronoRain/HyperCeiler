/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.provider;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeExtraGestureWire;

/** Discovery only, no input, action, geometry cache or arbitrary Binder forwarding. */
final class HomeExtraGestureRegistry {
    private final Binder lifetime=new Binder();
    private IBinder endpoint;
    private IBinder.DeathRecipient died;
    synchronized Bundle call(Context context,String method,Bundle input) {
        int uid=Binder.getCallingUid();
        if("hc_home_extra_register".equals(method)) {
            if(uid!=1000)throw new SecurityException("System UI publisher required");
            IBinder value=input==null?null:input.getBinder("endpoint");
            if(value==null)throw new IllegalArgumentException("Snapshot endpoint required");
            try {
                if(!HomeExtraGestureWire.DESCRIPTOR.equals(value.getInterfaceDescriptor()))
                    throw new IllegalArgumentException("Unexpected snapshot interface");
                if(value!=endpoint) {
                    IBinder.DeathRecipient next=()->retire(value);
                    value.linkToDeath(next,0);
                    if(endpoint!=null)endpoint.unlinkToDeath(died,0);
                    endpoint=value;died=next;
                }
            } catch(RemoteException unavailable) {
                Bundle result=new Bundle();result.putBoolean("available",false);return result;
            }
            Bundle result=new Bundle();result.putBoolean("available",true);result.putBinder("lifetime",lifetime);return result;
        }
        if(!"hc_home_extra_endpoint".equals(method))throw new IllegalArgumentException("Unsupported gesture discovery");
        try {
            if(uid!=0&&uid!=context.getPackageManager().getApplicationInfo("com.miui.home",0).uid)
                throw new SecurityException("Launcher reader required");
        } catch(android.content.pm.PackageManager.NameNotFoundException missing) {
            throw new IllegalStateException("Launcher identity unavailable",missing);
        }
        Bundle result=new Bundle();result.putBoolean("available",endpoint!=null);
        if(endpoint!=null)result.putBinder("endpoint",endpoint);return result;
    }
    private synchronized void retire(IBinder value) {
        if(endpoint==value){endpoint=null;died=null;}
    }
}
