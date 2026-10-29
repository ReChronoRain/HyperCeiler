/* SPDX-License-Identifier: AGPL-3.0-or-later */
import com.sevtinge.hyperceiler.libhook.rules.home.recent.HomeRecentsHapticPattern;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.HomeRecentsHapticProtocol;
import java.util.Arrays;
public final class PatternTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        check(HomeRecentsHapticProtocol.TRANSACTION==0x00484356&&HomeRecentsHapticProtocol.ACK==0x48435631);
        check(HomeRecentsHapticProtocol.validRequest(0,1000,0,0,0,1000));
        check(HomeRecentsHapticProtocol.validRequest(1,800,8,16,0,1000));
        check(!HomeRecentsHapticProtocol.validRequest(1,799,1,0,0,1000));
        check(!HomeRecentsHapticProtocol.validRequest(1,1001,1,0,0,1000));
        check(!HomeRecentsHapticProtocol.validRequest(1,1000,1,1,0,1000));
        check(!HomeRecentsHapticProtocol.validRequest(1,1000,1,0,4,1000));
        check(!HomeRecentsHapticProtocol.validRequest(0,1000,1,0,0,1000));
        check(!HomeRecentsHapticProtocol.validRequest(1,1000,0,0,0,1000));
        check(!HomeRecentsHapticProtocol.validRequest(1,1000,9,0,0,1000));
        check(HomeRecentsHapticProtocol.TRANSACTION_V2==0x00484357&&HomeRecentsHapticProtocol.ACK_V2==0x48435632);
        check(!HomeRecentsHapticProtocol.validRequest(2,1000,1,0,0,1000));
        for(int operation=0;operation<4;++operation)for(int count=0;count<=9;++count)
            for(int flags:new int[]{0,1,16})for(int remaining:new int[]{0,4})for(long time:new long[]{799,800,1000,1001}){
                boolean expected=operation==2?count==1&&flags!=1&&remaining==0&&time>=800&&time<=1000
                    :HomeRecentsHapticProtocol.validRequest(operation,time,count,flags,remaining,1000);
                check(HomeRecentsHapticProtocol.validExtendedRequest(operation,time,count,flags,remaining,1000)==expected);
            }
        int cases=0;
        for(boolean amplitude:new boolean[]{false,true})for(int type=0;type<4;++type)
            for(int strength=10;strength<=100;++strength)for(int count=1;count<=8;++count){
                var p=HomeRecentsHapticPattern.create(type,strength,count,amplitude);
                check(p.timings().length==p.amplitudes().length);
                check(p.timings().length==count*(type==3?4:2));check(p.timings()[0]==0);
                for(int i=0;i<p.timings().length;i+=2){
                    check(p.amplitudes()[i]==0);
                    check(p.timings()[i+1]==(type==0?7:type==1?15:type==2?32:11));
                    check(amplitude?p.amplitudes()[i+1]>=1&&p.amplitudes()[i+1]<=255:p.amplitudes()[i+1]==-1);
                    if(i>0)check(p.timings()[i]==(type==3&&(i/2&1)==1?42:24));
                }
                ++cases;
            }
        for(int a=0;a<4;++a)for(int b=a+1;b<4;++b)
            check(!Arrays.equals(HomeRecentsHapticPattern.create(a,100,1,false).timings(),
                HomeRecentsHapticPattern.create(b,100,1,false).timings()));
        for(int type=0;type<4;++type){int previous=0;
            for(int strength=10;strength<=100;++strength){int next=HomeRecentsHapticPattern.create(type,strength,1,true).amplitudes()[1];
                check(next>=previous);previous=next;}
        }
        for(int[] invalid:new int[][]{{-1,50,1},{4,50,1},{0,9,1},{0,101,1},{0,50,0},{0,50,9}}){
            boolean rejected=false;try{HomeRecentsHapticPattern.create(invalid[0],invalid[1],invalid[2],true);}
            catch(IllegalArgumentException expected){rejected=true;}check(rejected);
        }
        System.out.println("recents vibration patterns/protocol PASS: "+cases+" cases, four distinct rhythms without primitives/amplitude support, strength monotonicity, request deadline/flags/count/trailing-data checks");
    }
}
