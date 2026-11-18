/* SPDX-License-Identifier: AGPL-3.0-or-later */
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockUnlockReveal;
import java.io.PrintWriter;
import java.util.Random;
public class RevealGolden {
    static void container(PrintWriter out,DockUnlockReveal.ContainerPose p){
        out.print(" "+p.scaleX+" "+p.scaleY+" "+p.cropWidth+" "+p.cropHeight+" "+p.cornerProgress+" "+p.active+" "+p.cornerRadius(31,319));
    }
    public static void main(String[] args)throws Exception {
        try(var out=new PrintWriter(args[0])) {
            var random=new Random(80211);
            for(var style:DockUnlockReveal.Style.values())for(int session=0;session<24;session++) {
                out.println("S "+style.ordinal());var r=new DockUnlockReveal();r.setStyle(style);
                long now=10000;
                for(int n=0;n<900;n++) {
                    now+=random.nextInt(40);int command=random.nextInt(14);
                    out.print("E "+now+" "+command+" ");
                    out.print(switch(command){case 0->r.arm(now);case 1->r.startIfArmed(now);case 2->{r.cancel();yield true;}case 3->{r.onPoseCommitted(now);yield true;}default->r.needsFrame(now);});
                    float density=session%7==0?Float.NaN:(session%7==1?-1:3.2f),width=session%5==0?0:1128f;
                    float start=DockUnlockReveal.projectedStartScale(session%4==0?Double.NaN:session*72.25);
                    out.print(" "+r.progress(now));container(out,r.autoAimProjectedPose(now,start));
                    out.print(" "+r.alpha(now)+" "+r.risePx(density,now)+" "+r.slidePx(width,now));
                    container(out,r.containerPose(now));container(out,DockUnlockReveal.containerPose(style,now-session*100,now));
                    out.println(" "+r.eventEpochMillis()+" "+r.isRunning()+" "+r.hasPendingPose()+" "+r.isStalled(now));
                }
            }
        }
    }
}
