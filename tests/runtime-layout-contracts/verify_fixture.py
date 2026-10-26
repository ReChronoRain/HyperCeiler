#!/usr/bin/env python3
"""Check the committed instruction fixture against the pinned launcher APK."""
from pathlib import Path
import bisect, hashlib, json, lzma, struct, sys, zipfile

class Elf:
    def __init__(self, raw):
        self.raw=raw
        assert raw[:6]==b'\x7fELF\x02\x01'
        phoff,shoff=struct.unpack_from('<QQ',raw,32)
        phsize,phnum,shsize,shnum,strings=struct.unpack_from('<HHHHH',raw,54)
        self.sections=[struct.unpack_from('<IIQQQQIIQQ',raw,shoff+i*shsize) for i in range(shnum)]
        table=self.section(strings)
        self.names={table[s[0]:table.index(b'\0',s[0])].decode():i for i,s in enumerate(self.sections)}
        self.loads=[]
        for i in range(phnum):
            p=struct.unpack_from('<IIQQQQQQ',raw,phoff+i*phsize)
            if p[0]==1:self.loads.append(p)
    def section(self, index):
        s=self.sections[index];return self.raw[s[4]:s[4]+s[5]]
    def read(self, va, size):
        for p in self.loads:
            if p[3]<=va and va+size<=p[3]+p[5]:return self.raw[p[2]+va-p[3]:p[2]+va-p[3]+size]
        raise AssertionError('not file-backed: '+hex(va))

apk=Path(sys.argv[1]).read_bytes()
assert hashlib.sha256(apk).hexdigest()=='f4fe410efcd72ae30550bf6beac9a991d06d1ce82f0d51b51f644b11658e7cc8'
with zipfile.ZipFile(sys.argv[1]) as z:outer=Elf(z.read('lib/arm64-v8a/libapp.so'))
mini=Elf(lzma.decompress(outer.section(outer.names['.gnu_debugdata'])))
sym=mini.sections[mini.names['.symtab']];strings=mini.section(sym[6])
functions=[];names={}
for at in range(sym[4],sym[4]+sym[5],sym[9]):
    name,info,other,section,va,size=struct.unpack_from('<IBBHQQ',mini.raw,at)
    if not (0<section<len(mini.sections)) or not mini.sections[section][2]&4:continue
    if info&15!=2 or not va or va>0xffffffff or va%4 or size%4:continue
    if not any(p[1]&1 and p[3]<=va<p[3]+p[5] for p in outer.loads):continue
    functions.append(va) # zero-sized and unnamed STT_FUNC still bound the scan
    if name:names.setdefault(strings[name:strings.index(b'\0',name)].decode(),set()).add(va)
functions=sorted(set(functions))
fixture=json.loads(Path(__file__).with_name('launcher-7751.json').read_text())
for name,row in fixture.items():
    va=row['va'];size=len(row['words'])*4
    if not name.startswith('anonymous_'):
        assert va in names[name],name
        assert functions[bisect.bisect_right(functions,va)]-va==size,(name,'span mismatch')
    assert list(struct.unpack('<'+'I'*(size//4),outer.read(va,size)))==row['words'],name
print('Pinned 7751 fixture: full STT_FUNC bounds (including size-zero), named owners and original bytes: PASS')
