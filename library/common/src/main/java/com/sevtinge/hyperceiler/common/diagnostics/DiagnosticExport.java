/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.common.diagnostics;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Immutable, private export snapshot survives the document picker's activity recreation. */
public final class DiagnosticExport {
    private DiagnosticExport() {}
    private static final int MAX_BYTES=4*1024*1024, MAX_FILES=8;
    private static File directory(File cache){return new File(cache,"hook-diagnostics-exports");}
    private static File path(File cache,String token)throws IOException{
        if(token==null||!token.matches("[0-9a-f-]{36}\\.txt"))throw new IOException("Missing export snapshot");
        return new File(directory(cache),token);
    }
    public static synchronized String stage(File cache,String text)throws IOException{
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES)throw new IOException("Export snapshot exceeds budget");
        File dir=directory(cache);
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Export cache unavailable");
        File[] old=dir.listFiles(f->f.getName().matches("[0-9a-f-]{36}\\.txt"));
        if(old!=null){Arrays.sort(old,Comparator.comparingLong(File::lastModified).reversed());
            for(int i=MAX_FILES-1;i<old.length;i++)if(!old[i].delete())throw new IOException("Cannot bound export cache");}
        String token=UUID.randomUUID()+".txt";File file=path(cache,token);
        try(var out=new FileOutputStream(file)){out.write(bytes);}
        catch(IOException e){file.delete();throw e;}
        return token;
    }
    public static void copy(File cache,String token,OutputStream out)throws IOException{
        File file=path(cache,token);
        if(!file.isFile()||file.length()>MAX_BYTES)throw new IOException("Export snapshot unavailable");
        try(var in=new FileInputStream(file)){
            byte[] buffer=new byte[8192];int n,total=0;
            while((n=in.read(buffer))!=-1){total+=n;if(total>MAX_BYTES)throw new IOException("Export budget exceeded");out.write(buffer,0,n);}
        }
    }
    public static void discard(File cache,String token){try{path(cache,token).delete();}catch(IOException ignored){}}
}
