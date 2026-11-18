/* SPDX-License-Identifier: AGPL-3.0-or-later */
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeExtraGestureState;
import java.util.Random;

public final class Golden {
    private static final HomeExtraGestureState touch=new HomeExtraGestureState(20,300,400);
    private static final HomeExtraGestureState.Shake shake=new HomeExtraGestureState.Shake();
    private static final Object[] owners={null,new Object(),new Object()};
    private static final int[] events=new int[13];
    private record G(int width,int height,int rotation,float density,float top,float corner,
                     HomeExtraGestureState.Region handle,int owner,boolean available) {
        HomeExtraGestureState.Geometry value(){return available?new HomeExtraGestureState.Geometry(width,height,rotation,density,top,corner,handle,owners[owner]):null;}
        String text(){return (available?1:0)+" "+width+" "+height+" "+rotation+" "+bits(density)+" "+bits(top)+" "+bits(corner)+" "+owner+" "+(handle==null?0:1)+" "+(handle==null?"0 0 0 0":bits(handle.left())+" "+bits(handle.top())+" "+bits(handle.right())+" "+bits(handle.bottom()));}
    }
    private static int bits(float value){return Float.floatToRawIntBits(value);}
    private static void row(String input,HomeExtraGestureState.Result result) {
        if(result.event()>0)events[result.event()]++;
        System.out.println(input+" "+(result.claim()?1:0)+" "+(result.handled()?1:0)+" "+result.event()+" "+(touch.active()?1:0)+" "+(touch.pendingLong()?1:0)+" "+touch.longDeadline());
    }
    private static void sample(int action,int pointers,float x,float y,long time,long down,G g,long revision,int mask) {
        row("S "+action+" "+pointers+" "+bits(x)+" "+bits(y)+" "+time+" "+down+" "+g.text()+" "+revision+" "+mask,
            touch.sample(action,pointers,x,y,time,down,g.value(),revision,mask));
    }
    private static void hold(long time,G g,long revision,int mask) {
        row("L "+time+" "+g.text()+" "+revision+" "+mask,touch.longPress(time,g.value(),revision,mask));
    }
    private static void acceleration(float x,float y,float z,long time) {
        boolean hit=shake.sample(x,y,z,time);
        row("K "+bits(x)+" "+bits(y)+" "+bits(z)+" "+time,new HomeExtraGestureState.Result(false,false,hit?8:0));
    }
    private static void reset(){touch.reset();shake.reset();row("R",new HomeExtraGestureState.Result(false,false,0));}
    public static void main(String[] args) {
        G normal=new G(1080,2400,0,2,2300,80,new HomeExtraGestureState.Region(350,2300,730,2390),1,true);
        Random random=new Random(20261101);
        for(int block=0;block<500;++block) {
            long base=10000L+block*20000L;reset();
            sample(0,1,10,2350,base,base,normal,block,30);
            sample(2,1,40,2320,base+30,base,normal,block,30);
            sample(1,1,210,2150,base+200,base,normal,block,30);
            sample(0,1,1070,2350,base+1000,base+1000,normal,block,30);
            sample(2,1,1040,2320,base+1030,base+1000,normal,block,30);
            sample(1,1,870,2150,base+1200,base+1000,normal,block,30);
            sample(0,1,540,2350,base+2000,base+2000,normal,block,30);
            sample(1,1,540,2350,base+2050,base+2000,normal,block,30);
            sample(0,1,540,2350,base+2100,base+2100,normal,block,30);
            sample(1,1,540,2350,base+2150,base+2100,normal,block,30);
            sample(0,1,540,2350,base+4000,base+4000,normal,block,30);
            hold(base+4400,normal,block,30);
            sample(1,1,540,2350,base+4500,base+4000,normal,block,30);
            acceleration(0,0,0,base);acceleration(6,6,10,base+50);acceleration(0,0,0,base+100);
            for(int step=0;step<60;++step) {
                long down=base+6000+(step/6)*600;
                int variant=random.nextInt(10);
                G g=new G(variant==0?0:1080,2400,random.nextInt(2),variant==1?Float.NaN:2,
                    variant==2?-0.f:2300,80,variant==3?null:normal.handle,variant==4?2:1,variant!=5);
                int action=step%6==0?0:random.nextInt(7),pointers=variant==6?2:1;
                float x=variant==7?Float.POSITIVE_INFINITY:random.nextInt(1100)-10;
                float y=variant==8?Float.NaN:2100+random.nextInt(310);
                sample(action,pointers,x,y,down+step%6*50,down,g,variant==9?block+1:block,random.nextInt(32));
                if(step%5==0)hold(down+400,g,block,30);
            }
        }
        for(int event:new int[]{8,9,10,11,12})if(events[event]==0)throw new AssertionError("Missing event "+event);
        System.err.println("Java original gesture oracle PASS, all five events emitted across 500 sessions");
    }
}
