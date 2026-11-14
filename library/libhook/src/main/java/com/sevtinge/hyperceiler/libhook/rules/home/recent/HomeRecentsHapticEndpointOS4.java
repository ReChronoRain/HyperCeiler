/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.recent;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Binder;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.SystemClock;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import com.sevtinge.hyperceiler.libhook.provider.HomeRecentsHapticPrefs;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeLayoutNativeEndpointOS4;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeQuickSwitchOS4;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;

/** HCV1 scroll compatibility and HCV2 independent close channel, both use one provider snapshot. */
public final class HomeRecentsHapticEndpointOS4 {
    public static final int TRANSACTION=HomeRecentsHapticProtocol.TRANSACTION,ACK=HomeRecentsHapticProtocol.ACK;
    public static final int TRANSACTION_V2=HomeRecentsHapticProtocol.TRANSACTION_V2;
    private static int reported;
    private HomeRecentsHapticEndpointOS4() {}
    public static void receive(Parcel data,Parcel reply,int flags) {receive(data,reply,flags,false);}
    public static void receive(Parcel data,Parcel reply,int flags,boolean extended) {
        int available=0,enabled=0,close=0,type=0,strength=50,result=0;
        try {
            data.enforceInterface("android.view.IWindowManager");
            int operation=data.readInt();long timestamp=data.readLong();int count=data.readInt();
            var context=ContextUtils.getContextNoError(ContextUtils.FlAG_ONLY_ANDROID);
            if(!(extended?HomeRecentsHapticProtocol.validExtendedRequest(operation,timestamp,count,flags,data.dataAvail(),SystemClock.uptimeMillis())
                :HomeRecentsHapticProtocol.validRequest(operation,timestamp,count,flags,data.dataAvail(),SystemClock.uptimeMillis()))
                ||context==null||!HomeQuickSwitchOS4.isLauncher(context,Binder.getCallingUid(),Binder.getCallingPid())){
                writeReply(reply,extended,0,0,0,0,50,0);return;
            }
            long token=Binder.clearCallingIdentity();
            try {
                var profile=HomeLayoutNativeEndpointOS4.recentsHapticProfile();
                if(profile!=null){
                    enabled=profile.enabled()?1:0;type=profile.type();strength=profile.strength();
                    close=profile.close()?1:0;
                    available=1;
                    if(operation==0)result=1;
                    else if((operation==2?close:enabled)==1&&SystemClock.uptimeMillis()-timestamp<=200
                        &&context.getSystemService(PowerManager.class).isInteractive()
                        &&!context.getSystemService(KeyguardManager.class).isKeyguardLocked())
                        result=HomeRecentsHapticOS4.perform(context,profile,count)?1:0;
                }
            } finally {Binder.restoreCallingIdentity(token);}
        } catch(Exception failure){
            if(reported++<3)ModuleLog.w("HyperCeiler.HomeLayout","recents haptic endpoint unavailable",failure);
        }
        writeReply(reply,extended,available,enabled,close,type,strength,result);
    }
    private static void writeReply(Parcel reply,boolean extended,int available,int enabled,int close,int type,int strength,int result) {
        if(available==0){enabled=close=type=result=0;strength=50;}
        reply.writeInt(extended?HomeRecentsHapticProtocol.ACK_V2:ACK);reply.writeInt(available);reply.writeInt(enabled);
        if(extended)reply.writeInt(close);
        reply.writeInt(type);reply.writeInt(strength);reply.writeInt(result);
    }
}
