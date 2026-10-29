// SPDX-License-Identifier: AGPL-3.0-or-later
import com.sevtinge.hyperceiler.libhook.rules.home.dock.HomeRecentsBarState;
public class StateTest {
    static int checks;
    static void c(boolean b) { if(!b)throw new AssertionError("check "+checks);checks++; }
    public static void main(String[] args) {
        long now=20_000_000_000L;
        HomeRecentsBarState s=new HomeRecentsBarState();
        c(!s.overview(now)); c(!s.accept(10123,20,1,now,9,1,1,now));
        s.bind(10123,20);c(s.identity(10123,20));c(!s.identity(10124,20));c(!s.identity(10123,21));
        long seq=1,rev=1;
        for(int mask:new int[]{8,9,10,12,11,13,14,15,8,0}) {
            c(s.accept(10123,20,seq++,now,mask,rev,rev,now));
            c(s.overview(now)==((mask&8)!=0&&(mask&7)!=0));rev++;
        }
        c(s.accept(10123,20,seq++,now,9,rev,rev,now));
        c(s.overview(now+HomeRecentsBarState.MAX_AGE_NS-1));
        c(!s.overview(now+HomeRecentsBarState.MAX_AGE_NS));
        c(s.accept(10123,20,seq++,now+1,9,rev,rev,now+1)); // same revision keepalive
        c(!s.accept(10123,20,seq,now,8,rev,rev,now)); // one revision cannot change payload
        c(!s.accept(10123,20,seq-1,now,9,rev,rev,now));
        for(long[] bad:new long[][]{{0,now,9,rev,rev},{seq,0,9,rev,rev},{seq,now+1,9,rev,rev},
            {seq,now-HomeRecentsBarState.MAX_AGE_NS,9,rev,rev},{seq,now,16,rev,rev},
            {seq,now,-1,rev,rev},{seq,now,1,rev,rev},{seq,now,7,rev,rev},
            {seq,now,9,0,0},{seq,now,9,rev,rev+1},{seq,now,9,rev-1,rev-1}})
            c(!s.accept(10123,20,bad[0],bad[1],bad[2],bad[3],bad[4],now));
        c(!s.accept(10124,20,seq,now,9,rev,rev,now));
        c(!s.accept(10123,21,seq,now,9,rev,rev,now));
        s.bind(10123,21);c(!s.overview(now)); // process restart never reuses the previous source
        c(s.accept(10123,21,1,now,10,1,1,now));c(s.overview(now));
        s.clear();c(!s.overview(now));c(!s.identity(10123,21));
        System.out.println("recents bar identity/lease "+checks+" checks passed");
    }
}
