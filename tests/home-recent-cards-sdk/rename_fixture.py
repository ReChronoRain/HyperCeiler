# SPDX-License-Identifier: AGPL-3.0-or-later
"""Rename private linkage consistently and rebuild a valid single-bucket GNU hash.

Only temporary copies of caller-supplied SDK ELF files are edited. No vendor
symbols or binaries are checked into this test directory.
"""
import struct


def rename(data):
    blob=bytearray(data)
    phoff=struct.unpack_from('<Q',blob,32)[0]
    phnum=struct.unpack_from('<H',blob,56)[0]
    headers=[struct.unpack_from('<IIQQQQQQ',blob,phoff+i*56) for i in range(phnum)]
    def offset(va,size=1):
        for kind,flags,off,start,_,filesz,memsz,align in headers:
            if kind==1 and start<=va and size<=filesz-(va-start):return off+va-start
        raise ValueError('ELF address outside file-backed PT_LOAD')
    dynamic={}
    for kind,flags,off,start,_,filesz,memsz,align in headers:
        if kind!=2:continue
        for at in range(off,off+filesz,16):
            tag,value=struct.unpack_from('<QQ',blob,at)
            if tag==0:break
            dynamic[tag]=value
    gnu=offset(dynamic[0x6ffffef5],16)
    buckets,symoffset,bloom,shift=struct.unpack_from('<IIII',blob,gnu)
    chains=gnu+16+8*bloom+4*buckets
    first=max(struct.unpack_from('<'+str(buckets)+'I',blob,gnu+16+8*bloom))
    assert first>=symoffset
    last=first
    while not struct.unpack_from('<I',blob,chains+4*(last-symoffset))[0]&1:last+=1
    count=last+1
    strings=offset(dynamic[5],dynamic[10]);strsz=dynamic[10]
    changed=bytes(blob[strings:strings+strsz]).replace(b'_RN',b'_QN')
    blob[strings:strings+strsz]=changed
    symtab=offset(dynamic[6],24*count)
    end=chains+4*(count-symoffset)
    size=16+8+4+4*(count-symoffset)
    assert size<=end-gnu
    blob[gnu:end]=bytes(end-gnu)
    struct.pack_into('<IIIIQI',blob,gnu,1,symoffset,1,0,0xffffffffffffffff,symoffset)
    for symbol in range(symoffset,count):
        name=struct.unpack_from('<I',blob,symtab+24*symbol)[0]
        assert name<strsz
        nul=blob.index(0,strings+name,strings+strsz)
        value=5381
        for c in blob[strings+name:nul]:value=((value*33)+c)&0xffffffff
        struct.pack_into('<I',blob,gnu+28+4*(symbol-symoffset),(value&~1)|(symbol==count-1))
    return bytes(blob)
