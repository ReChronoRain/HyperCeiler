from pathlib import Path
import re,struct,sys
W=Path(__file__).resolve().parents[2];D=Path(__file__).resolve().parent
sys.path.insert(0,str(W/'tests/back-gesture-os4/runtime'))
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM,UC_HOOK_CODE
from unicorn.arm64_const import *
s=(D/'FolderAutoCloseTest.cpp').read_text(encoding='utf8');words=[int(x.rstrip('u'),16) for x in re.search(r'words=\{(.*?)\}',s,re.S)[1].split(',')]
va=0x144f440;rx=0x17d148c;registered=0x89a718;logger=0x8b3840;close=0x1931b18
checks=0
for enabled in [False,True]:
 for mode in [False,True]:
  for exists in [False,True]:
   for opened in [False,True]:
    uc=Uc(UC_ARCH_ARM64,UC_MODE_ARM);mapped=set()
    def mapat(addr,size=0x1000):
     page=addr&~0xfff
     for p in range(page,(addr+size+0xfff)&~0xfff,0x1000):
      if p not in mapped:uc.mem_map(p,0x1000);mapped.add(p)
    for addr,size in [(va,0x1000),(0x100000,0x10000),(0x200000,0x2000),(0x300000,0x1000),(0x400000,0x1000),(0x500000,0x4000),(0x600000,0x100000),(rx,4),(registered,4),(logger,4),(close,4)]:mapat(addr,size)
    uc.mem_write(va,struct.pack('<'+'I'*len(words),*words))
    if enabled:uc.mem_write(va+0x328,struct.pack('<I',0xd503201f))
    fp=0x108000;thread=0x100000;pool=0x600000;null=0x300001
    for reg,val in [(UC_ARM64_REG_X29,fp),(UC_ARM64_REG_X15,fp-0x38),(UC_ARM64_REG_SP,0x107000),(UC_ARM64_REG_X26,thread),(UC_ARM64_REG_X27,pool),(UC_ARM64_REG_X28,0),(UC_ARM64_REG_X22,null)]:uc.reg_write(reg,val)
    def w64(at,v):uc.mem_write(at,struct.pack('<Q',v))
    w64(fp-0x20,0x200101);w64(thread+0x78,0x500000);w64(0x500000+0x1cd0,0x600001);w64(0x500000+0x3990,0x400001);w64(0x500000+0x2490,0x600001)
    uc.mem_write(0x400001+0xef,struct.pack('<I',0x200201))
    count=[0]
    def hook(uc,pc,size,data):
     if pc==va+0x400:uc.emu_stop();return
     if pc in [rx,registered,logger,close]:
      if pc==rx:result=mode if uc.reg_read(UC_ARM64_REG_X1)==0x200101 else opened
      elif pc==registered:result=exists
      else:result=None
      if pc==close:count[0]+=1
      uc.reg_write(UC_ARM64_REG_X0,null+(32 if result else 48) if result is not None else null)
      uc.reg_write(UC_ARM64_REG_PC,uc.reg_read(UC_ARM64_REG_X30))
    uc.hook_add(UC_HOOK_CODE,hook);uc.emu_start(va+0x320,va+len(words)*4,count=1000)
    expected=int((mode or enabled) and exists and opened)
    assert count[0]==expected,(enabled,mode,exists,opened,count[0],expected);checks+=1
print(f'FOLDER_AUTO_CLOSE_ARM64={checks} cases; failed=0; actual stock mode/registered/open/close instruction path; mocked Dart callees')
