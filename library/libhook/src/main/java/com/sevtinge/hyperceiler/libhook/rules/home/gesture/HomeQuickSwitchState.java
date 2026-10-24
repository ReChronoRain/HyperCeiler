/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

/** Input/cleanup state only; no Android objects, app addresses or wall-clock guesses. */
final class HomeQuickSwitchState {
    final long down, revision;
    final int device, display, source;
    final float x, y, density, width;
    float lastX, lastY, currentX, currentY;
    long lastMove, latest, released;
    boolean valid=true, accepted, pending, returned, reset, finished, sent;
    volatile boolean retired;
    int expected=-1;
    Object tracker, navigation;

    HomeQuickSwitchState(long down, long revision, int device, int display, int source,
                         float x, float y, float density, float width) {
        this.down=down;this.revision=revision;this.device=device;this.display=display;this.source=source;
        this.x=lastX=currentX=x;this.y=lastY=currentY=y;this.density=density;this.width=width;
        latest=lastMove=down;
        valid=finite(x,y)&&Float.isFinite(density)&&density>=.5f&&density<=8&&Float.isFinite(width)&&width>0&&down>0;
    }
    private static boolean finite(float a,float b) { return Float.isFinite(a)&&Float.isFinite(b); }
    void sample(int action,int count,float x,float y,long time) {
        if(!valid||retired)return;
        if(count!=1||!finite(x,y)||time<latest||time-down>10000||action==3||action==5||action==6) {
            valid=false;return;
        }
        latest=time;currentX=x;currentY=y;
        if(Math.hypot(x-lastX,y-lastY)>4*density) {lastMove=time;lastX=x;lastY=y;}
    }
    boolean claim(long epoch,boolean enabled,int action) {
        float dx=currentX-x,dy=currentY-y;
        boolean inward=x<width/2?dx>0:dx<0;
        if(!valid||retired||pending||!enabled||epoch!=revision||!accepted||tracker==null||action!=1
                ||!inward||Math.abs(dx)<64*density||Math.abs(dy)>=Math.abs(dx)*.5f
                ||latest-lastMove<300||latest-down<350)return false;
        pending=true;released=latest;return true;
    }
    boolean complete(long now,long epoch,boolean enabled) {
        if(sent||retired||!pending||!returned||!reset||!finished||expected<0||!enabled
                ||epoch!=revision||now<released||now-released>750)return false;
        sent=true;return true;
    }
}
