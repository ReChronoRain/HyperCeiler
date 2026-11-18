/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

/** Module-owned, versioned snapshot protocol, independent of the frozen HG1 mask. */
public final class HomeExtraGestureWire {
    public static final String DESCRIPTOR="com.sevtinge.hyperceiler.home.IExtraGestureSnapshot";
    public static final int ACK=0x48474531, VERSION=1, QUERY=1;
    public static final int UNAVAILABLE=0, READY=1, CLOSED=2;
    public static final int CONTROL=2, CONTROL_ACK=0x4847454c, CONTROL_VERSION=2;
    public static final int TAKE=1, RENEW=2, RELEASE=3;
    public static final String OWNER_DESCRIPTOR="com.sevtinge.hyperceiler.home.IExtraGestureOwner";
    public static final int REVOKE=1, ACTION=3, ACTION_ACK=0x48474541;
    public static boolean allows(HomeExtraGestureHandoff.Grant grant,int mask,long producer,long revision,long serial,int event,long time,long now) {
        return grant!=null&&grant.producer()==producer&&grant.revision()==revision&&grant.serial()==serial
            &&now<grant.until()&&event>=9&&event<=12&&(mask&(1<<(event-8)))!=0&&time>=0&&time<=now&&now-time<=750;
    }
    public static final int DENIED=0, OWNED=1, RELEASED=2;
    public static void writeGrant(Writer output,int status,HomeExtraGestureHandoff.Grant grant) {
        output.integer(CONTROL_ACK);output.integer(CONTROL_VERSION);output.integer(status);
        output.wide(grant==null?0:grant.producer());output.wide(grant==null?0:grant.revision());
        output.wide(grant==null?0:grant.serial());output.wide(grant==null?0:grant.until());
    }
    private HomeExtraGestureWire() {}
    public record Snapshot(long producer,long revision,long published,int mask,float slop,
                           int doubleTimeout,int longTimeout,HomeExtraGestureState.Geometry geometry) {
        public Snapshot {
            if(producer<=0||revision<=0||published<0||mask<0||mask>31||!Float.isFinite(slop)||slop<=0
                ||doubleTimeout<=0||longTimeout<=0)throw new IllegalArgumentException("Invalid gesture snapshot");
            if(geometry!=null&&(!geometry.valid()||geometry.rotation()<0||geometry.rotation()>3
                ||!(geometry.owner() instanceof Long owner)||owner<=0
                ||geometry.width()>32768||geometry.height()>32768
                ||(geometry.handle()!=null&&!geometry.handle().valid())))
                throw new IllegalArgumentException("Invalid navigation geometry");
        }
    }
    public interface Writer {
        void integer(int value);
        void wide(long value);
        void scalar(float value);
    }
    public static void write(Writer output,Snapshot snapshot,boolean closed) {
        output.integer(ACK);output.integer(VERSION);
        output.integer(closed?CLOSED:snapshot==null?UNAVAILABLE:READY);
        if(closed||snapshot==null)return;
        output.wide(snapshot.producer());output.wide(snapshot.revision());output.wide(snapshot.published());
        output.integer(snapshot.mask());output.scalar(snapshot.slop());
        output.integer(snapshot.doubleTimeout());output.integer(snapshot.longTimeout());
        var geometry=snapshot.geometry();output.integer(geometry==null?0:1);
        if(geometry==null)return;
        output.wide((Long)geometry.owner());output.integer(geometry.width());output.integer(geometry.height());
        output.integer(geometry.rotation());output.scalar(geometry.density());output.scalar(geometry.navTop());output.scalar(geometry.corner());
        var handle=geometry.handle();output.integer(handle==null?0:1);
        if(handle==null)return;
        output.scalar(handle.left());output.scalar(handle.top());output.scalar(handle.right());output.scalar(handle.bottom());
    }
}
