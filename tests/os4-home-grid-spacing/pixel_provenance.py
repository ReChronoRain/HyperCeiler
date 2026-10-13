"""Resolve the two math slots using the supplied engine, not upstream offsets."""
import hashlib,struct
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

R=test_paths.TEST_DATA_ROOT
ENGINE=test_paths.ROM_ROOT / 'components/system_ext/lib64/libhyper_os_flutter.so'
def prove():
 b=ENGINE.read_bytes();ph=struct.unpack_from('<Q',b,32)[0];psz,pnum=struct.unpack_from('<HH',b,54)
 def read(va,size):
  for j in range(pnum):
   kind,flags,off,start,_,n,_,_=struct.unpack_from('<IIQQQQQQ',b,ph+j*psz)
   if kind==1 and start<=va and va+size<=start+n:return b[off+va-start:off+va-start+size]
  raise AssertionError('unmapped virtual address')
 sh=struct.unpack_from('<Q',b,40)[0];ss,n,names=struct.unpack_from('<HHH',b,58)
 sections=[struct.unpack_from('<IIQQQQIIQQ',b,sh+j*ss)for j in range(n)]
 table=sections[names];strings=b[table[4]:table[4]+table[5]]
 name=lambda data,off:data[off:data.index(0,off)].decode()
 sections={name(strings,s[0]):s for s in sections}
 rel=sections['.rela.dyn'];syms=sections['.dynsym'];st=sections['.dynstr'];st=b[st[4]:st[4]+st[5]]
 found={}
 for off in range(rel[4],rel[4]+rel[5],rel[9]):
  va,info,addend=struct.unpack_from('<QQq',b,off)
  if va in [0x1113aa8,0x1113ab0]:
   symbol=struct.unpack_from('<I',b,syms[4]+(info>>32)*syms[9])[0]
   assert info&0xffffffff==1025 and addend==0
   found[va]=name(st,symbol)
 assert found=={0x1113aa8:'trunc',0x1113ab0:'round'}
 guards={0x657670:0x900055ea,0x657678:0xf945554a,0x657680:0xd00057c8,0x657688:0xf902950a,
  0x65768c:0x900055e8,0x657694:0xf9455908,0x657684:0xd00057c9,0x65769c:0xf9029928,
  0xf79e48:0x90000ecb,0xf79e54:0xf942956b,0xf79e68:0xf903a80b,
  0xf79e50:0x90000ec9,0xf79e58:0xf9429929,0xf79e70:0xf903ac09}
 for va,word in guards.items():assert int.from_bytes(read(va,4),'little')==word
 # Validate ADRP pages and the immediate load/store field offsets separately.
 def adrp(pc,reg,target):
  w=guards[pc];imm=((w>>5)&0x7ffff)<<2|((w>>29)&3);imm=imm-(1<<21) if imm&(1<<20) else imm
  assert w&0x9f000000==0x90000000 and w&31==reg and (pc&~4095)+(imm<<12)==target
 for args in [(0x657670,10,0x1113000),(0x65768c,8,0x1113000),(0x657680,8,0x1151000),(0x657684,9,0x1151000),(0xf79e48,11,0x1151000),(0xf79e50,9,0x1151000)]:adrp(*args)
 for pc,load,reg,base,offset in [(0x657678,True,10,10,0xaa8),(0x657688,False,10,8,0x528),(0x657694,True,8,8,0xab0),(0x65769c,False,8,9,0x530),(0xf79e54,True,11,11,0x528),(0xf79e68,False,11,0,0x750),(0xf79e58,True,9,9,0x530),(0xf79e70,False,9,0,0x758)]:
  w=guards[pc];assert w&0xffc00000==(0xf9400000 if load else 0xf9000000) and w&31==reg and (w>>5)&31==base and ((w>>10)&4095)*8==offset
 return {'engine':str(test_paths.report_path(ENGINE, R)),'engine_sha256':hashlib.sha256(b).hexdigest(),'method':'actual ELF dynamic relocations and exact guarded native load/store chains; no guessed upstream Thread offsets',
  'slots':{'0x750':{'global':'0x1151528','got':'0x1113aa8','symbol':'trunc'},'0x758':{'global':'0x1151530','got':'0x1113ab0','symbol':'round'}},
  'guarded_words':{hex(va):hex(word)for va,word in guards.items()},'initializer_slice_sha256':hashlib.sha256(read(0x65766c,0x38)).hexdigest(),'publication_slice_sha256':hashlib.sha256(read(0xf79e48,0x2c)).hexdigest(),
  'execution_scope':'static binding proof; the test supplies C math imports rather than running engine initialization or Android libc'}
