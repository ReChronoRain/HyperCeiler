/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

/** Platform-independent state. Only an actual live navigation-window DOWN can enter it. */
public final class HomeExtraGestureState {
    public static final int SHAKE=8, LEFT=9, RIGHT=10, LONG=11, DOUBLE=12;
    public record Region(float left,float top,float right,float bottom) {
        public boolean contains(float x,float y) {return x>=left&&x<right&&y>=top&&y<bottom;}
        public boolean valid() {return Float.isFinite(left)&&Float.isFinite(top)&&Float.isFinite(right)
            &&Float.isFinite(bottom)&&left<right&&top<bottom;}
    }
    public record Geometry(int width,int height,int rotation,float density,float navTop,float corner,
                           Region handle,Object owner) {
        public boolean valid() {return owner!=null&&width>0&&height>0&&Float.isFinite(density)
            &&density>=.5f&&density<=8&&Float.isFinite(navTop)&&navTop>=0&&navTop<height
            &&Float.isFinite(corner)&&corner>0&&corner<=width/4f;}
    }
    public record Result(boolean claim,boolean handled,int event) {
        static final Result NONE=new Result(false,false,0);
    }
    private Geometry geometry;
    private long revision,down,latest,tap=-1;
    private float x,y,lastX,lastY,tapX,tapY;
    private int kind,mask; // 1 left, 2 right, 3 hint
    private boolean active,owned,second,invalid;
    private final float slop;
    private final int doubleTimeout,longTimeout;
    public HomeExtraGestureState(float touchSlop,int doubleTimeout,int longTimeout) {
        this.slop=touchSlop;this.doubleTimeout=doubleTimeout;this.longTimeout=longTimeout;
    }
    public void reset() {active=owned=second=invalid=false;kind=0;tap=-1;geometry=null;}
    public boolean active() {return active;}
    public boolean pendingLong() {return active&&kind==3&&!invalid&&!owned&&!second&&(mask&8)!=0;}
    public long longDeadline() {return down+longTimeout;}
    private boolean matches(Geometry g,long rev,int m) {
        return geometry!=null&&geometry.equals(g)&&revision==rev&&mask==m;
    }
    private Result result(boolean claim,int event) {return new Result(claim,owned,event);}
    public Result sample(int action,int pointers,float px,float py,long time,long downTime,
                         Geometry g,long rev,int m) {
        if(g==null||!g.valid()||!Float.isFinite(px)||!Float.isFinite(py)||time<downTime||downTime<0
            ||time-downTime>5000||pointers!=1||!Float.isFinite(slop)||slop<=0) {reset();return Result.NONE;}
        if(action==0) {
            if(!matches(g,rev,m))tap=-1;
            geometry=g;revision=rev;mask=m;down=downTime;latest=time;x=lastX=px;y=lastY=py;
            active=true;owned=invalid=second=false;kind=0;
            if(py>=g.navTop&&py<g.height&&px>=0&&px<g.width) {
                if(px<g.corner&&(m&2)!=0)kind=1;
                else if(px>=g.width-g.corner&&(m&4)!=0)kind=2;
                else if(g.handle!=null&&g.handle.valid()&&g.handle.contains(px,py)&&(m&24)!=0)kind=3;
            }
            if(kind==0){active=false;tap=-1;return Result.NONE;}
            second=kind==3&&(m&16)!=0&&tap>=0&&time-tap>=0&&time-tap<=doubleTimeout
                &&Math.hypot(px-tapX,py-tapY)<=slop;
            if(second){owned=true;tap=-1;return result(true,0);}
            return Result.NONE;
        }
        if(!active)return Result.NONE;
        if(!matches(g,rev,m)||down!=downTime||time<latest){reset();return Result.NONE;}
        latest=time;lastX=px;lastY=py;
        if(action==3){reset();return Result.NONE;}
        if(action!=1&&action!=2)return result(false,0);
        float dx=px-x,up=y-py,inward=kind==2?-dx:dx;
        double travel=Math.hypot(dx,up);
        boolean claim=false;
        if(kind==3) {
            if(travel>slop){invalid=true;tap=-1;}
            if(action==1) {
                int event=second&&owned&&!invalid&&time-down<=250?DOUBLE:0;
                if(!owned&&!invalid&&time-down<=250&&(m&16)!=0){tap=time;tapX=px;tapY=py;}
                else tap=-1;
                active=false;return result(false,event);
            }
        } else {
            if(travel>slop&&(inward<=0||up<=0||up>inward*3||inward>up*3))invalid=true;
            if(action==2&&!invalid&&!owned&&travel>slop){owned=true;claim=true;tap=-1;}
            if(action==1) {
                active=false;
                return result(claim,owned&&!invalid&&inward>=48*g.density&&up>=48*g.density
                    &&time-down<=1500?(kind==1?LEFT:RIGHT):0);
            }
        }
        return result(claim,0);
    }
    public Result longPress(long now,Geometry g,long rev,int m) {
        if(!pendingLong()||!matches(g,rev,m)||now<down+longTimeout||now-down>5000)return Result.NONE;
        owned=true;tap=-1;invalid=true;return result(true,LONG);
    }

    /** Two different acceleration impulses; monotonic sensor timestamps, bounded interval and throttle. */
    public static final class Shake {
        private float x,y,z;
        private long previous=-1,impulse=-1,last=-1;
        public void reset(){previous=impulse=last=-1;}
        public boolean sample(float nx,float ny,float nz,long millis) {
            if(!Float.isFinite(nx)||!Float.isFinite(ny)||!Float.isFinite(nz)||millis<0){reset();return false;}
            if(previous<0||millis<=previous||millis-previous>500){x=nx;y=ny;z=nz;previous=millis;impulse=-1;return false;}
            float dx=Math.abs(nx-x),dy=Math.abs(ny-y),dz=Math.abs(nz-z);
            x=nx;y=ny;z=nz;previous=millis;
            boolean changed=(dx>4&&dy>4)||(dx>4&&dz>8)||(dy>4&&dz>8);
            if(!changed)return false;
            if(impulse<0||millis-impulse>400){impulse=millis;return false;}
            if(millis-impulse<30)return false;
            impulse=-1;
            if(last>=0&&millis-last<750)return false;
            last=millis;return true;
        }
    }
}
