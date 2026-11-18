/* SPDX-License-Identifier: AGPL-3.0-or-later */
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeExtraGestureHandoff;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeExtraGestureWire;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeExtraGestureState;
public final class HandoffTest {
    private static HomeExtraGestureWire.Snapshot snapshot(long at,long revision) {
        return new HomeExtraGestureWire.Snapshot(90,revision,at,8,12,300,400,
            new HomeExtraGestureState.Geometry(1080,2400,0,3,2320,72,
                new HomeExtraGestureState.Region(350,2360,730,2400),Long.valueOf(1)));
    }
    private static HomeExtraGestureHandoff ready() {
        var state=new HomeExtraGestureHandoff(90);state.update(snapshot(1000,1),true,1000);
        assert state.blocked(1000)&&state.begin(new Object(),90,1,1000)==0;
        state.update(snapshot(3000,1),true,3000);assert !state.blocked(3000);return state;
    }
    public static void main(String[] args) {
        Object owner=new Object(),other=new Object();var state=ready();
        assert state.begin(owner,89,1,3000)==0&&state.begin(owner,90,2,3000)==0;
        long serial=state.begin(owner,90,1,3000);assert serial>0&&state.blocked(3000);
        assert state.begin(other,90,1,3000)==0&&state.renew(owner,serial,1,3000)==null;
        assert state.drained(other,serial,3000)==null;
        var grant=state.drained(owner,serial,3001);assert grant!=null&&grant.until()==5001;
        assert state.renew(owner,serial,2,3100)==null&&state.release(other,serial,1)==false;
        state.update(snapshot(3500,1),true,3500);grant=state.renew(owner,serial,1,3500);assert grant.until()==5500;
        assert state.blocked(5499)&&!state.blocked(5500)&&state.renew(owner,serial,1,5500)==null;
        for(int change=0;change<7;++change) {
            state=ready();serial=state.begin(owner,90,1,3000);assert state.drained(owner,serial,3001)!=null;
            var next=snapshot(3500,change==0?2:1);
            if(change==1)next=null;
            if(change==2)next=new HomeExtraGestureWire.Snapshot(90,1,3500,0,12,300,400,next.geometry());
            if(change==3)next=new HomeExtraGestureWire.Snapshot(90,1,3500,8,12,300,401,next.geometry());
            if(change==5||change==6) {
                var g=next.geometry();
                next=new HomeExtraGestureWire.Snapshot(90,1,3500,8,12,300,400,
                    new HomeExtraGestureState.Geometry(g.width(),g.height(),change==5?1:0,g.density(),g.navTop(),g.corner(),g.handle(),Long.valueOf(change==6?2:1)));
            }
            state.update(next,change!=4,3500);assert state.blocked(3500);
            assert state.renew(owner,serial,1,3500)==null;
            assert state.release(owner,serial,1)&&!state.blocked(3500);
        }
        state=ready();serial=state.begin(owner,90,1,3000);state.cancel(serial);
        long next=state.begin(other,90,1,3001);assert next>serial;
        state.cancel(serial);assert state.blocked(3001);assert state.drained(owner,serial,3001)==null;
        state.died(owner);assert state.blocked(3001);state.died(other);assert !state.blocked(3001);
        state=ready();serial=state.begin(owner,90,1,3000);assert state.drained(owner,serial,4000)==null&&!state.blocked(4000);
        state=ready();serial=state.begin(owner,90,1,3000);state.update(snapshot(3001,2),true,3001);
        assert state.drained(owner,serial,3001)==null&&!state.blocked(3001);
        state=ready();serial=state.begin(owner,90,1,3000);assert state.drained(owner,serial,3001)!=null;
        state.update(snapshot(3500,1),false,3500);assert state.blocked(5000)&&!state.blocked(5001);
        var replacement=new HomeExtraGestureHandoff(91);replacement.update(null,true,3500);
        assert replacement.blocked(5001)&&replacement.blocked(5499)&&!replacement.blocked(5500);
        System.out.println("PASS actual handoff warmup, drain barrier, renewal, revision/geometry loss, timeout quarantine, stale completion and owner death");
    }
}
