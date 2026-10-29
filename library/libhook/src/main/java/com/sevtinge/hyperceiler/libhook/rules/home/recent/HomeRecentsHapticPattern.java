/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.recent;

/** Explicit pulse shapes remain distinct even without Android composition primitives. */
public final class HomeRecentsHapticPattern {
    private HomeRecentsHapticPattern() {}
    public record Pattern(long[] timings,int[] amplitudes) {}
    public static Pattern create(int type,int strength,int count,boolean amplitudeControl) {
        if(type<0||type>3||strength<10||strength>100||count<1||count>8)
            throw new IllegalArgumentException("Invalid recents vibration profile");
        int pulses=count*(type==3?2:1);
        long[] timings=new long[pulses*2];int[] amplitudes=new int[pulses*2];
        int duration=switch(type){case 0->7;case 1->15;case 2->32;default->11;};
        int gain=switch(type){case 0->35;case 2->100;default->70;};
        int amplitude=amplitudeControl?Math.max(1,255*strength*gain/10000):-1;
        for(int i=0;i<pulses;++i){
            timings[2*i]=i==0?0:type==3&&(i&1)==1?42:24;
            timings[2*i+1]=duration;amplitudes[2*i+1]=amplitude;
        }
        return new Pattern(timings,amplitudes);
    }
}
