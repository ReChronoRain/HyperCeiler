/* SPDX-License-Identifier: AGPL-3.0-or-later */
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockNativeMotion;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockRecentsMotion;
import java.io.PrintWriter;
import java.util.Random;

public final class MotionGolden {
    public static void main(String[] args) throws Exception {
        Random random=new Random(0x53434cL);
        try(PrintWriter out=new PrintWriter(args[0])) {
            for(int session=0;session<200;session++) {
                DockNativeMotion nativeMotion=new DockNativeMotion();
                DockRecentsMotion spring=new DockRecentsMotion();
                DockNativeMotion.Sample previous=null;
                long now=1_000_000_000L,sequence=0;
                out.println("R");
                for(int step=0;step<400;step++) {
                    now+=random.nextInt(100_000_000);
                    long next=sequence+random.nextInt(4)-1;
                    long timestamp=now-random.nextInt(700_000_000);
                    double scale=switch(random.nextInt(25)) {
                        case 0 -> Double.NaN;case 1 -> Double.POSITIVE_INFINITY;case 2 -> -0.0;
                        case 3 -> -.05;case 4 -> 2.1;case 5 -> 1;default -> random.nextDouble()*1.3;
                    };
                    long packed=(Double.doubleToRawLongBits(scale)&~3L)|random.nextInt(4);
                    long entries=step+1,published=step/2;
                    DockNativeMotion.Sample sample=DockNativeMotion.validate(next,timestamp,packed,entries,published,sequence,now);
                    if(sample!=null)sequence=next;
                    boolean overview=random.nextBoolean();
                    boolean changed=DockNativeMotion.hasPublishedProgress(previous,sample);
                    boolean accepted=nativeMotion.accept(sample,overview);
                    float density=(random.nextInt(12)-1)/2f;
                    long epoch=now/1_000_000L-random.nextInt(200);
                    Double aim=DockNativeMotion.autoAimScale(sample,epoch,now);
                    out.println("N "+next+" "+timestamp+" "+Long.toUnsignedString(packed)+" "+entries+" "+published+" "+(sample!=null)+" "+now+" "+overview+" "+changed+" "+accepted+" "+density+" "+nativeMotion.progress()+" "+nativeMotion.scale()+" "+nativeMotion.relativeOffsetY(density)+" "+epoch+" "+(aim!=null)+" "+(aim==null?0:aim));
                    if(sample!=null)previous=sample;
                    long ms=now/1_000_000+(step%47==0?3000:step%13==0?-40:0);
                    int action=random.nextInt(8);boolean target=random.nextBoolean();float resume=random.nextFloat()*1.6f-.3f;
                    boolean set=false;
                    if(action==0)set=spring.setOverview(target,ms);
                    if(action==1)spring.resumeFrom(resume,target,ms);
                    if(action==2)spring.finish();
                    int base=random.nextInt(2500);
                    out.println("S "+action+" "+target+" "+resume+" "+ms+" "+set+" "+density+" "+base+" "+spring.progress(ms)+" "+spring.velocity(ms)+" "+spring.offsetY(density,base,ms)+" "+spring.isRunning(ms));
                    // The same immutable native sample can be reinterpreted exactly once
                    // when overview authentication arrives after the native scale.
                    if(sample!=null&&step%9==0) {
                        boolean replay=nativeMotion.accept(sample,true);
                        out.println("P "+replay+" "+nativeMotion.progress()+" "+nativeMotion.scale());
                    }
                }
            }
        }
    }
}
