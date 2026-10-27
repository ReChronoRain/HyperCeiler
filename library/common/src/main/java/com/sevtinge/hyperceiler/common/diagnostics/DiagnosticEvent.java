/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.common.diagnostics;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Versioned, bounded wire record. A log record is evidence, never an authenticated command. */
public record DiagnosticEvent(String hookId, String feature, String type, String packageName,
    String processName, int pid, String bootId, String processStart, String generation,
    String moduleVersion, String systemVersion, long time, String phase, String stage, String code,
    String detail, String source, long rawId) {
    public static final String PREFIX = "HCD1|";
    public static final int MAX_WIRE = 6000;
    public DiagnosticEvent {
        hookId=bound(hookId,256);feature=bound(feature,192);type=bound(type,32);
        packageName=bound(packageName,160);processName=bound(processName,192);
        bootId=bound(bootId,80);processStart=bound(processStart,80);generation=bound(generation,100);
        moduleVersion=bound(moduleVersion,192);systemVersion=bound(systemVersion,192);
        phase=bound(phase,40);stage=bound(stage,64);code=bound(code,80);detail=bound(detail,1400);source=bound(source,96);
        if(hookId.isEmpty()||pid<0||time<0)throw new IllegalArgumentException("Invalid diagnostic identity");
    }
    public static String bound(String s,int max){return s==null?"":s.substring(0,Math.min(max,s.length()));}
    private static String encode(String s){return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    public String wire(){
        String[] fields={hookId,feature,type,packageName,processName,Integer.toString(pid),bootId,
            processStart,generation,moduleVersion,systemVersion,Long.toString(time),phase,stage,code,detail};
        StringBuilder out=new StringBuilder(PREFIX);
        for(int i=0;i<fields.length;i++){if(i>0)out.append('|');out.append(encode(fields[i]));}
        return out.toString();
    }
    public static DiagnosticEvent parse(String wire,String source,long rawId){
        if(wire==null||wire.length()>MAX_WIRE||!wire.startsWith(PREFIX))return null;
        try{
            String[] p=wire.substring(PREFIX.length()).split("\\|",-1);if(p.length!=16)return null;
            for(int i=0;i<p.length;i++)p[i]=new String(Base64.getUrlDecoder().decode(p[i]),StandardCharsets.UTF_8);
            DiagnosticEvent e=new DiagnosticEvent(p[0],p[1],p[2],p[3],p[4],Integer.parseInt(p[5]),p[6],p[7],p[8],p[9],p[10],Long.parseLong(p[11]),p[12],p[13],p[14],p[15],source,rawId);
            return e.wire().equals(wire)?e:null; // Reject truncation, invalid UTF-8 and noncanonical data.
        }catch(IllegalArgumentException invalid){return null;}
    }
    public String scope(){return packageName+"/"+processName+"/"+pid;}
    public String identity(){return scope()+"/"+bootId+"/"+processStart+"/"+generation+"/"+type+"/"+hookId;}
}
