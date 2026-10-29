/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.recent;

import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.SystemClock;
import android.app.KeyguardManager;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeQuickSwitchOS4;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;

/** HCV1 scroll compatibility and HCV2 independent close channel, both use one provider snapshot. */
public final class HomeRecentsHapticEndpointOS4 {
    public static final int TRANSACTION=HomeRecentsHapticProtocol.TRANSACTION,ACK=HomeRecentsHapticProtocol.ACK;
    public static final int TRANSACTION_V2=HomeRecentsHapticProtocol.TRANSACTION_V2;
    private static final Uri PREFS=Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs/home_recents_haptic");
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
            try(var cursor=context.getContentResolver().query(PREFS,null,null,null,null)){
                if(cursor!=null&&cursor.getColumnCount()==4&&cursor.moveToFirst()&&cursor.getInt(0)==1){
                    enabled=cursor.getInt(1);type=cursor.getInt(2);strength=cursor.getInt(3);
                    var extras=cursor.getExtras();
                    close=extras!=null&&extras.getInt("close_version",0)==1?extras.getInt("close_enabled",0):0;
                    if(enabled<0||enabled>1||close<0||close>1||type<0||type>3||strength<10||strength>100)
                        throw new IllegalArgumentException("Invalid recents haptic snapshot");
                    available=1;
                    if(operation==0)result=1;
                    else if((operation==2?close:enabled)==1&&SystemClock.uptimeMillis()-timestamp<=200
                        &&context.getSystemService(PowerManager.class).isInteractive()
                        &&!context.getSystemService(KeyguardManager.class).isKeyguardLocked())
                        result=HomeRecentsHapticOS4.perform(context,type,strength,count)?1:0;
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
