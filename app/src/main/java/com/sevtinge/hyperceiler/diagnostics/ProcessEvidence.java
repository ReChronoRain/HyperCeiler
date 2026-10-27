/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.diagnostics;

import android.content.Context;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** On-demand bounded Root probe. No process starts/stops, configuration writes or resident polling. */
final class ProcessEvidence {
    final Map<Integer,String> starts=new HashMap<>(),names=new HashMap<>();
    boolean complete;
    String boot="";
    static ProcessEvidence query(Context context,Set<Integer> requested){
        ProcessEvidence out=new ProcessEvidence();File result=null;java.lang.Process reader=null;
        try{
            try {out.boot=Files.readString(new File("/proc/sys/kernel/random/boot_id").toPath()).trim();}catch(Exception unavailable){}
            List<Integer> ids=requested.stream().filter(p->p>0&&p<Integer.MAX_VALUE).sorted().limit(128).toList();
            StringBuilder script=new StringBuilder("printf 'HC_BEGIN\\nB '; cat /proc/sys/kernel/random/boot_id; ");
            for(int pid:ids)script.append("if [ -r /proc/").append(pid).append("/stat ]; then printf 'P ").append(pid).append(" '; cat /proc/").append(pid).append("/stat; printf 'N ").append(pid).append(" '; tr '\\000' '\\n' < /proc/").append(pid).append("/cmdline | head -n 1; elif [ -d /proc/").append(pid).append(" ]; then printf 'U ").append(pid).append("\\n'; fi; ");
            script.append("printf 'HC_END\\n'");
            result=File.createTempFile("hook-process-",".probe",context.getCacheDir());
            reader=new ProcessBuilder("su","-c",script.toString()).redirectErrorStream(true).redirectOutput(result).start();
            if(!reader.waitFor(5,TimeUnit.SECONDS)){reader.destroyForcibly();return out;}
            if(reader.exitValue()!=0||result.length()>131072)return out;
            String text=Files.readString(result.toPath());
            out.complete=requested.size()<=128&&text.startsWith("HC_BEGIN\n")&&text.endsWith("HC_END\n");
            for(String line:text.split("\n")){
                if(line.startsWith("B "))out.boot=line.substring(2).trim();
                if(line.startsWith("U "))out.complete=false;
                int space=line.indexOf(' ',2);if(space<0)continue;
                if(line.startsWith("P ")){
                    int pid=Integer.parseInt(line.substring(2,space));String stat=line.substring(space+1);
                    int close=stat.lastIndexOf(')');if(close<0)continue;
                    String[] fields=stat.substring(close+2).split(" +");if(fields.length>19&&fields[19].matches("[0-9]+"))out.starts.put(pid,fields[19]);else out.complete=false;
                }else if(line.startsWith("N "))out.names.put(Integer.parseInt(line.substring(2,space)),line.substring(space+1));
            }
        }catch(Exception unavailable){out.complete=false;}
        finally{if(out.boot.isEmpty())out.complete=false;if(reader!=null)reader.destroy();if(result!=null)result.delete();}
        return out;
    }
}
