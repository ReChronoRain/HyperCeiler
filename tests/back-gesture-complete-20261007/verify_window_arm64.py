from pathlib import Path
import sys,subprocess,struct,json
D=Path(__file__).resolve().parent;W=D.parents[1];sys.path[:0]=[str(W/'tests/back-gesture-os4/runtime'),str(W/'tests/home-layout-native')]
from unicorn import *
from unicorn.arm64_const import *
from dart_dump import Elf
native=next((W/'app/build/intermediates/cxx/RelWithDebInfo').rglob('obj/arm64-v8a/libHyperCeilerNative.so'));e=Elf(native)
nm='D:/build-tools/android-sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-nm.exe'
symbols={f[2]:int(f[0],16) for l in subprocess.check_output([nm,str(native)],text=True).splitlines() if len(f:=l.split())==3}
entry=symbols['hc_back_window_entry'];body=symbols['hc_back_window_body'];slot=symbols['hc_back_window_original']
p=subprocess.check_output([str(W/'tests/back-gesture-window-20261007/resolve.exe'),str(W/'tests/back-gesture-os4/libapp_launcher.so')],text=True);apply,update,splice,rectsp,side=map(lambda x:int(x,16),p.split());rust=Elf(W/'tests/back-gesture-os4/libapp_launcher.so')
checks=0
for side in [0,1]:
 for width in [68,136,273]:
  for nzcv in [0,0x20000000,0xa0000000,0xf0000000]:
   u=Uc(UC_ARCH_ARM64,UC_MODE_ARM);base=0x10000000;backup=0x20000000;stack=0x40000000
   u.mem_map(base,0x1000000)
   for va,off,n in e.loads:u.mem_write(base+va,e.data[off:off+n])
   u.mem_map(backup,4096);u.mem_write(backup,rust.read(splice,16));u.mem_write(base+slot,struct.pack('<Q',backup));u.mem_map(stack,0x200000)
   regs=[UC_ARM64_REG_X0+i for i in range(29)]+[UC_ARM64_REG_X29,UC_ARM64_REG_X30];qs=[UC_ARM64_REG_Q0+i for i in range(32)]
   for i,r in enumerate(regs):u.reg_write(r,0x1234567800000000+i)
   for i,r in enumerate(qs):u.reg_write(r,0x23456789000000000000000000000000+i)
   sp=stack+0x100000;u.reg_write(UC_ARM64_REG_SP,sp);u.reg_write(UC_ARM64_REG_NZCV,nzcv);u.reg_write(regs[19],122)
   u.mem_write(sp+rectsp,struct.pack('<4i',0,1909,width,2576));bx=[u.reg_read(r) for r in regs];bq=[u.reg_read(r) for r in qs]
   called=[]
   def hook(uc,pc,size,data):
    if pc==base+body:
     saved=uc.reg_read(UC_ARM64_REG_X0);assert saved==sp-784 and saved%16==0
     assert struct.unpack('<Q',uc.mem_read(saved+19*8,8))[0]==122
     uc.mem_write(saved+19*8,struct.pack('<Q',max(122,width)));called.append(True)
     # This test stubs the separately host-tested body, then stresses all caller-saved regs.
     lr=uc.reg_read(UC_ARM64_REG_LR)
     for r in regs[:19]:uc.reg_write(r,0xdeadbeef)
     for r in qs:uc.reg_write(r,0xabcdef)
     uc.reg_write(UC_ARM64_REG_NZCV,0);uc.reg_write(UC_ARM64_REG_PC,lr)
    elif pc==backup:
     assert uc.reg_read(UC_ARM64_REG_NZCV)==nzcv
    elif pc==backup+16:uc.emu_stop()
   u.hook_add(UC_HOOK_CODE,hook);u.emu_start(base+entry,backup+16,count=1000)
   valid=[len(called)==1,u.reg_read(UC_ARM64_REG_SP)==sp,u.reg_read(UC_ARM64_REG_W8)==width,u.reg_read(UC_ARM64_REG_W9)==0,u.reg_read(regs[19])==max(122,width)]
   valid += [u.reg_read(r)==bx[i] for i,r in enumerate(regs) if i not in [8,9,17,19]]
   valid += [u.reg_read(r)==bq[i] for i,r in enumerate(qs)]
   checks+=len(valid);assert all(valid)
result=f'WINDOW_ARM64={checks} checks; failed=0; built entry + stubbed separately tested body + real original instructions; SP16/NZCV/Q restored'
print(result);(D/'WINDOW_ARM64.json').write_text(json.dumps({'command':['python','-X','utf8',str(D/'verify_window_arm64.py')],'input':str(native),'literal_output':result,'exit_status':0},indent=2))
