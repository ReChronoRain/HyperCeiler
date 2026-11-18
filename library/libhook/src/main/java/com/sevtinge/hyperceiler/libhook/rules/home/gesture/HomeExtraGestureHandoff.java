/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

/** Main-thread authority, independent of Android so the real transitions can be tested. */
public final class HomeExtraGestureHandoff {
    public static final int LEASE_MS=2000, DRAIN_MS=1000;
    public record Grant(long producer,long revision,long serial,long until) {}
    private final long producer;
    private HomeExtraGestureWire.Snapshot snapshot;
    private HomeExtraGestureWire.Snapshot prepared;
    private Object owner;
    private long serial,warmUntil,until;
    private boolean armed,pending,valid;

    public HomeExtraGestureHandoff(long producer){this.producer=producer;}
    public void update(HomeExtraGestureWire.Snapshot value,boolean enabled,long now) {
        if(enabled&&!armed)warmUntil=now+LEASE_MS;
        armed=enabled;snapshot=value;
        if(owner!=null&&(!enabled||!same(prepared,value))) {
            valid=false;
            if(pending)clear(); // No grant was sent, so the old backend can resume.
        }
        expire(now);
    }
    private static boolean same(HomeExtraGestureWire.Snapshot a,HomeExtraGestureWire.Snapshot b) {
        return a!=null&&b!=null&&a.producer()==b.producer()&&a.revision()==b.revision()&&a.mask()==b.mask()
            &&Float.compare(a.slop(),b.slop())==0&&a.doubleTimeout()==b.doubleTimeout()&&a.longTimeout()==b.longTimeout()
            &&java.util.Objects.equals(a.geometry(),b.geometry());
    }
    public boolean blocked(long now){expire(now);return owner!=null||(armed&&now<warmUntil);}
    public long expiry(long now){expire(now);return owner!=null?until:armed&&now<warmUntil?warmUntil:0;}
    public long begin(Object token,long expectedProducer,long revision,long now) {
        expire(now);
        if(!armed||now<warmUntil||owner!=null||token==null||snapshot==null||snapshot.geometry()==null
            ||snapshot.producer()!=producer||expectedProducer!=producer||revision!=snapshot.revision()
            ||now<snapshot.published()||now-snapshot.published()>1500)return 0;
        owner=token;prepared=snapshot;pending=valid=true;until=now+DRAIN_MS;return ++serial;
    }
    public Grant drained(Object token,long attempt,long now) {
        expire(now);
        if(owner!=token||serial!=attempt||!pending||!valid||!armed||!same(prepared,snapshot)||!fresh(now))return null;
        pending=false;until=now+LEASE_MS;return grant();
    }
    public Grant renew(Object token,long attempt,long revision,long now) {
        expire(now);
        if(owner!=token||serial!=attempt||prepared.revision()!=revision||pending||!valid||!armed||!same(prepared,snapshot)||!fresh(now))return null;
        until=now+LEASE_MS;return grant();
    }
    public Grant held(){return owner!=null&&!pending?grant():null;}
    public Grant current(long now) {
        expire(now);return owner!=null&&!pending&&valid&&armed&&same(prepared,snapshot)&&fresh(now)?grant():null;
    }
    public boolean release(Object token,long attempt,long revision) {
        if(owner!=token||serial!=attempt||prepared.revision()!=revision)return false;
        clear();return true;
    }
    public void cancel(long attempt){if(pending&&serial==attempt)clear();}
    public void died(Object token){if(owner==token)clear();}
    public boolean owns(Object token){return owner==token;}
    private boolean fresh(long now){return snapshot!=null&&now>=snapshot.published()&&now-snapshot.published()<=1500;}
    private Grant grant(){return new Grant(producer,prepared.revision(),serial,until);}
    private void expire(long now){if(owner!=null&&now>=until)clear();}
    private void clear(){owner=null;prepared=null;pending=valid=false;until=0;}
}
