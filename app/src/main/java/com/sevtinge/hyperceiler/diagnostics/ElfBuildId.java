/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.diagnostics;
import java.nio.*;
import java.util.HexFormat;
/** Bounded ELF64 little-endian PT_NOTE reader; does not treat filenames as build identity. */
public final class ElfBuildId {
    private ElfBuildId(){}
    public static String read(byte[] bytes){
        try{
            if(bytes.length<64||bytes[0]!=127||bytes[1]!='E'||bytes[2]!='L'||bytes[3]!='F'||bytes[4]!=2||bytes[5]!=1)return "";
            var b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);long ph=b.getLong(32);int size=Short.toUnsignedInt(b.getShort(54)),count=Short.toUnsignedInt(b.getShort(56));
            if(ph<0||size<56||count>128||ph+(long)size*count>bytes.length)return "";
            for(int i=0;i<count;i++){
                int p=(int)(ph+(long)i*size);if(b.getInt(p)!=4)continue;
                long off=b.getLong(p+8),length=b.getLong(p+32);if(off<0||length<0||off>bytes.length||length>bytes.length-off)continue;
                long end=off+length;
                for(long at=off;at+12<=end;){
                    int n=b.getInt((int)at),d=b.getInt((int)at+4),type=b.getInt((int)at+8);at+=12;
                    if(n<0||d<0||n>end-at)break;long desc=at+((n+3L)&~3L);
                    if(desc>end||d>end-desc)break;
                    if(type==3&&n==4&&bytes[(int)at]=='G'&&bytes[(int)at+1]=='N'&&bytes[(int)at+2]=='U'&&bytes[(int)at+3]==0&&d>0&&d<=64)return "elf:"+HexFormat.of().formatHex(bytes,(int)desc,(int)desc+d);
                    at=desc+((d+3L)&~3L);
                }
            }
        }catch(IndexOutOfBoundsException|IllegalArgumentException invalid){}
        return "";
    }
}
