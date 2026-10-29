/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.common.diagnostics;
/** logd UID/PID metadata, not a caller-supplied tag, establishes the OS principal. */
public final class DiagnosticProvenance {
    private DiagnosticProvenance(){}
    public static int uid(String value){
        if(value.equals("system"))return 1000;if(value.equals("root"))return 0;
        if(value.matches("u[0-9]+_a[0-9]+")){int sep=value.indexOf('_');return Math.addExact(Math.addExact(Math.multiplyExact(Integer.parseInt(value.substring(1,sep)),100000),10000),Integer.parseInt(value.substring(sep+2)));}
        return Integer.parseInt(value);
    }
    public static boolean matches(String source,String ids,long loggedAt,DiagnosticEvent e,int expectedUid){
        if(!"logcat".equals(source)||e.pid()<=0||e.time()<loggedAt-5000||e.time()>loggedAt+5000)return false;
        String[] parts=ids.split(":");if(parts.length!=3)return false;
        try{return Integer.parseInt(parts[1])==e.pid()&&uid(parts[0])==expectedUid;}catch(IllegalArgumentException|ArithmeticException invalid){return false;}
    }
}
