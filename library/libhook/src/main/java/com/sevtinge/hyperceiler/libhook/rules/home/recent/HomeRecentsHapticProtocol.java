/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.recent;

/** HCV1 request boundary, synchronous only, with a short interaction deadline. */
public final class HomeRecentsHapticProtocol {
    public static final int TRANSACTION=0x00484356,ACK=0x48435631;
    public static final int TRANSACTION_V2=0x00484357,ACK_V2=0x48435632;
    private HomeRecentsHapticProtocol() {}
    public static boolean validRequest(int operation,long timestamp,int count,int flags,int remaining,long now) {
        long age=now-timestamp;
        return (flags&1)==0&&remaining==0&&age>=0&&age<=200&&operation>=0&&operation<=1
            &&(operation==0?count==0:count>=1&&count<=8);
    }
    public static boolean validExtendedRequest(int operation,long timestamp,int count,int flags,int remaining,long now) {
        return operation==2?validRequest(1,timestamp,count,flags,remaining,now)&&count==1
            :validRequest(operation,timestamp,count,flags,remaining,now);
    }
}
