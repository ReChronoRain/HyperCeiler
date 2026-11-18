/* SPDX-License-Identifier: AGPL-3.0-or-later */
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeExtraGestureHandoff;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeExtraGestureWire;
public final class LeaseGolden {
    public static void main(String[] args) {
        var grant=new HomeExtraGestureHandoff.Grant(90,7,3,5000);
        for(int mask=0;mask<32;mask++)for(int event=0;event<=14;event++)
            for(long producer:new long[]{90,91})for(long revision:new long[]{7,8})for(long serial:new long[]{3,4})
                for(long age:new long[]{-1,0,749,750,751}) {
                    long now=2000,time=now-age;
                    boolean expected=HomeExtraGestureWire.allows(grant,mask,producer,revision,serial,event,time,now);
                    System.out.printf("%d %d %d %d %d %d %d %d%n",mask,producer,revision,serial,event,time,now,expected?1:0);
                }
    }
}
