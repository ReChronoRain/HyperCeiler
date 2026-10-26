#!/usr/bin/env python3
"""Execute shipping column/persistence/save ARM64 banks. Pass Android NDK as argv[1].

No Dart VM/GC, named-argument cloning or launcher UI is emulated. Original
four-word replay, transient persistence ownership and live scalar stores are.
"""
from pathlib import Path
import sys,subprocess,struct,tempfile
W=Path(__file__).resolve().parents[2];R=W
_tmp=tempfile.TemporaryDirectory(prefix='hc-columns-arm-');O=Path(_tmp.name)
p=R/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S'
clang=next((Path(sys.argv[1])/'toolchains/llvm/prebuilt').glob('*/bin/clang'));obj=O/'banks.o'
subprocess.run([str(clang),'--target=aarch64-linux-android35','-I'+str(R/'app/src/main/cpp'),'-c',str(p),'-o',str(obj)],check=True)
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM,UC_HOOK_CODE;from unicorn import arm64_const as A
from capstone import Cs,CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN
e=Elf(obj);ss=e.sections[e.section_names['.symtab']];st=e.sections[ss[6]];strings=e.data[st[4]:st[4]+st[5]];sy=[]
for at in range(ss[4],ss[4]+ss[5],24):
 n,info,o,sec,v,z=struct.unpack_from('<IBBHQQ',e.data,at);sy.append((strings[n:strings.find(b'\0',n)].decode(),sec,v,z))
ix=e.section_names['.text'];ts=e.sections[ix];code=bytearray(e.data[ts[4]:ts[4]+ts[5]]);start=next(v for n,s,v,z in sy if n=='hc_dart_columns_entry');base=0x100000;data=0x300000;stock=0x400000;end=0x401000
region_stop=next(v for n,s,v,z in sy if n=='hc_layout_save_entry')
region_end=next(i.address+4 for i in Cs(CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN).disasm(bytes(code[region_stop:]),region_stop) if i.mnemonic=='ret')
globs={n:data+i*8 for i,n in enumerate(['hc_native_columns_request','hc_dart_columns_frame','hc_dart_columns_continue','hc_dart_columns_original','hc_grid_autofit_requested','hc_dart_columns_ready','hc_grid_persist_original','hc_grid_transient_seen','hc_layout_save_original','hc_layout_save_field'])}
for sec in e.sections.values():
 if sec[1]!=4 or sec[7]!=ix:continue
 for at in range(sec[4],sec[4]+sec[5],24):
  off,inf,add=struct.unpack_from('<QQq',e.data,at)
  if not start<=off<region_end:continue
  name=sy[inf>>32][0];kind=inf&0xffffffff;addr=globs[name]+add;pc=base+off;word=struct.unpack_from('<I',code,off)[0]
  if kind==275:
   delta=(addr>>12)-(pc>>12);word=(word&~((3<<29)|(0x7ffff<<5)))|((delta&3)<<29)|(((delta>>2)&0x7ffff)<<5)
  elif kind==277:word=(word&~(4095<<10))|((addr&4095)<<10)
  elif kind==286:word=(word&~(4095<<10))|(((addr&4095)//8)<<10)
  else:raise AssertionError((name,kind))
  struct.pack_into('<I',code,off,word)
assert not any(x.mnemonic in ['bl','blr'] for x in Cs(CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN).disasm(bytes(code[start:region_end]),base+start))
checks=0;cases=0
U64=(1<<64)-1
def ck(v):
 global checks
 checks+=1;assert v,(cases,checks)
for requested in [0,1|(1<<8),1|(4<<8),1|(5<<8),1|(6<<8),1|(8<<8),1,1|(9<<8),5<<8]:
 for ready in [0,1]:
  for row in [-1,0,2]:
   for rowRequest in [0,1|(2<<8),1|(7<<8),1|(16<<8),1|(17<<8)]:
    for field,frame in [(19,-24),(27,-40),(51,-72)]:
     u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
     for a,z in [(base,(len(code)+4095)&~4095),(data,4096),(stock,8192),(0x600000,65536),(0x700000,65536)]:u.mem_map(a,z)
     u.mem_write(base,bytes(code));u.mem_write(data,struct.pack('<QQQQQQQQQQ',requested,frame&U64,end,stock,rowRequest,ready,stock,0,stock,7))
     replay=[0xb8400002|((field&511)<<12),0xd37ff823,0xa90009e3,0xf84003a1|((frame&511)<<12)]
     u.mem_write(stock,struct.pack('<IIII',*replay));r={i:0x10000000+i*0x1000+cases for i in range(31)};r[0]=0x60a001;r[1]=row&U64;r[15]=0x608000;r[29]=0x609000
     for i,v in r.items():u.reg_write(getattr(A,'UC_ARM64_REG_X'+str(i)),v)
     q={i:(i+100)<<80|i+500 for i in range(32)}
     for i,v in q.items():u.reg_write(getattr(A,'UC_ARM64_REG_Q'+str(i)),v)
     u.reg_write(A.UC_ARM64_REG_SP,0x708000);u.reg_write(A.UC_ARM64_REG_NZCV,0xa0000000)
     u.mem_write(r[0]+field,struct.pack('<I',8));u.mem_write(r[29]+frame,struct.pack('<Q',0x60b001));seen=[]
     def step(uc,pc,sz,arg):
      if pc in [end,stock+16]:seen.append(pc);uc.emu_stop()
     u.hook_add(UC_HOOK_CODE,step);u.emu_start(base+start,end+4,count=150)
     custom=bool(ready) and bool(requested&1) and row==-1 and 1<=(requested>>8)<=8;col=(requested>>8)*2 if custom else 8;rowTag=((rowRequest>>8)*2 if custom and rowRequest&1 and 2<=(rowRequest>>8)<=16 else row*2)&U64
     ck(seen==[end if custom else stock+16]);ck(u.reg_read(A.UC_ARM64_REG_X2)==col);ck(u.reg_read(A.UC_ARM64_REG_X1)==0x60b001);ck(u.reg_read(A.UC_ARM64_REG_X3)==rowTag)
     ck(bytes(u.mem_read(r[15],16))==struct.pack('<QQ',rowTag,col))
     for i,v in r.items():
      if i not in [1,2,3,16,17]:ck(u.reg_read(getattr(A,'UC_ARM64_REG_X'+str(i)))==v)
     for i,v in q.items():ck(u.reg_read(getattr(A,'UC_ARM64_REG_Q'+str(i)))==v)
     ck(u.reg_read(A.UC_ARM64_REG_SP)==0x708000);ck(u.reg_read(A.UC_ARM64_REG_NZCV)==0xa0000000);cases+=1

policy=next(v for n,s,v,z in sy if n=='hc_grid_persist_entry')
layout=next(v for n,s,v,z in sy if n=='hc_layout_save_entry')
for entry in [policy,layout]:
 for request in [0,1,5<<8,1|(5<<8)]:
  for sticky in [0,1]:
   for field in [7,15,39]:
    u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
    for a,z in [(base,(len(code)+4095)&~4095),(data,4096),(stock,8192),(0x600000,65536),(0x700000,65536)]:u.mem_map(a,z)
    u.mem_write(base,bytes(code));u.mem_write(data,struct.pack('<QQQQQQQQQQ',request,0,end,stock,0,1,stock,sticky,stock,field))
    r={i:0x10000000+i*0x1000 for i in range(31)};r[1]=0x60a001;r[2]=2;r[15]=0x608000;r[30]=end
    for i,v in r.items():u.reg_write(getattr(A,'UC_ARM64_REG_X'+str(i)),v)
    original=bytes([0xa5]*128);u.mem_write(r[1],original)
    u.reg_write(A.UC_ARM64_REG_SP,0x708000);u.reg_write(A.UC_ARM64_REG_NZCV,0xa0000000);seen=[]
    def stop(uc,pc,sz,arg):
     if pc in [end,stock]:seen.append(pc);uc.emu_stop()
    u.hook_add(UC_HOOK_CODE,stop);u.emu_start(base+entry,end+4,count=150)
    custom=bool(request&1 or sticky)
    ck(seen==[end if custom else stock]);ck(u.reg_read(A.UC_ARM64_REG_X0)==(r[22] if custom else r[0]))
    expected=bytearray(original)
    if custom and entry==layout:expected[field:field+8]=struct.pack('<Q',r[2])
    ck(bytes(u.mem_read(r[1],128))==bytes(expected))
    ck(struct.unpack('<I',u.mem_read(globs['hc_grid_transient_seen'],4))[0]==int(custom))
    for i,v in r.items():
     if i not in [0,16,17]:ck(u.reg_read(getattr(A,'UC_ARM64_REG_X'+str(i)))==v)
    ck(u.reg_read(A.UC_ARM64_REG_SP)==0x708000);ck(u.reg_read(A.UC_ARM64_REG_NZCV)==0xa0000000);cases+=1
print(f'DART_COLUMNS_ARM cases={cases} checks={checks} failed=0; real stub+original four-word replay; no Dart call/GC added; copyWith boundary not executed')
