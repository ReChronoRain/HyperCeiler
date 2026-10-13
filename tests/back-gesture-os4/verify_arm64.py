from pathlib import Path
import sys,subprocess,struct,json
D=Path(__file__).resolve().parent;W=D.parents[1]
sys.path[:0]=[str(D/'runtime'),str(W/'tests/home-layout-native')]
from unicorn import *
from unicorn.arm64_const import *
from dart_dump import Elf
native=next((W/'app/build/intermediates/cxx/RelWithDebInfo').rglob('obj/arm64-v8a/libHyperCeilerNative.so'))
nm='D:/build-tools/android-sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-nm.exe'
symbols={}
for line in subprocess.check_output([nm,str(native)],text=True).splitlines():
 fields=line.split()
 if len(fields)==3 and ('hc_back_gesture' in fields[2] or 'hc_back_window' in fields[2] or 'g_back_screen' in fields[2] or 'g_back_gesture_config' in fields[2]):symbols[fields[2]]=int(fields[0],16)
entry=symbols['hc_back_gesture_entry'];slot=symbols['hc_back_gesture_original'];config=next(v for k,v in symbols.items() if 'g_back_gesture_config' in k)
p=subprocess.run([str(D/'check.exe'),str(D/'libapp_launcher.so')],capture_output=True,text=True,check=True)
owner,left,right,screen,splice=map(lambda x:int(x,16),p.stdout.split())
e=Elf(native);rust=Elf(D/'libapp_launcher.so');native_base=0x10000000;backup=0x20000000;heap=0x30000000;stack=0x40000000
checks=failed=0
for rotation in range(4):
 for height,width in [(60,100),(25,200),(10,400),(95,105),(100,400)]:
  u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
  u.mem_map(native_base,0x1000000)
  for va,off,n in e.loads:u.mem_write(native_base+va,e.data[off:off+n])
  u.mem_map(backup,4096);u.mem_write(backup,rust.read(splice,44))
  u.mem_map(heap,4096);u.mem_map(stack,0x200000)
  u.mem_write(native_base+slot,struct.pack('<Q',backup));u.mem_write(native_base+config,struct.pack('<I',height|(width<<16)))
  xregs=[UC_ARM64_REG_X0+i for i in range(29)]+[UC_ARM64_REG_X29,UC_ARM64_REG_X30]
  qregs=[UC_ARM64_REG_Q0+i for i in range(32)]
  for i,r in enumerate(xregs):u.reg_write(r,0x1234567800000000+i)
  for i,r in enumerate(qregs):u.reg_write(r,0x12345678000000000000000000000000+i)
  w,h=(2670,1200) if rotation&1 else (1200,2670);top,bottom=(240,960) if rotation&1 else (0,2573)
  for r,v in [(8,top),(21,bottom),(24,w),(28,h),(22,480),(19,heap+128)]:u.reg_write(xregs[r],v)
  u.reg_write(UC_ARM64_REG_S8,struct.unpack('<I',struct.pack('<f',21.0))[0])
  u.reg_write(UC_ARM64_REG_SP,stack+0x100000);u.reg_write(UC_ARM64_REG_NZCV,0xa0000000)
  before_x=[u.reg_read(r) for r in xregs];before_q=[u.reg_read(r) for r in qregs];sp=u.reg_read(UC_ARM64_REG_SP)
  def stop(uc,address,size,data):
   if address==backup+44:uc.emu_stop()
  u.hook_add(UC_HOOK_CODE,stop)
  try:u.emu_start(native_base+entry,backup+44,count=10000)
  except UcError as err:print('EMULATION_ERROR',rotation,height,width,hex(u.reg_read(UC_ARM64_REG_PC)),err);raise
  assert u.reg_read(UC_ARM64_REG_PC)==backup+44
  l=struct.unpack('<4i',u.mem_read(heap+128+left,16));r=struct.unpack('<4i',u.mem_read(heap+128+right,16))
  actual_top,actual_bottom=top,bottom
  if height!=60:
   length=h*height//100
   if h>=w:actual_top=max(0,bottom-length)
   else:actual_top=max(0,min(h-length,(top+bottom)//2-length//2));actual_bottom=actual_top+length
  # Replay FCVTZS: exact native float arithmetic, not the old Java round().
  rounded=lambda x:struct.unpack('<f',struct.pack('<f',x))[0]
  dp=rounded(rounded(21.0*rounded(width/100.0)) if width!=100 else 21.0)
  pixels=int(rounded(rounded(dp*480.0)/160.0))
  validations=[l==(0,actual_top,pixels,actual_bottom),r==(w-pixels,actual_top,w,actual_bottom),u.reg_read(UC_ARM64_REG_SP)==sp,u.reg_read(UC_ARM64_REG_NZCV)==0xa0000000]
  for i,v in enumerate(before_x):
   if i not in [8,9,17,21]:validations.append(u.reg_read(xregs[i])==v)
  for i,v in enumerate(before_q):
   if i not in [0,1,8]:validations.append(u.reg_read(qregs[i])==v)
  checks+=len(validations);failed+=sum(not b for b in validations)
  if not all(validations):print('FAIL',rotation,height,width,l,r,pixels,[i for i,b in enumerate(validations) if not b])

print(f'BACK_GESTURE_ARM64={checks} checks; failed={failed}; built trampoline + production body + real original instructions; SP16/NZCV/Q preserved')
(D/'ARM64.json').write_text(json.dumps(dict(command=['python','-X','utf8',str(Path(__file__).resolve())],input=str(native),result=f'{checks} checks; failed={failed}',exit_status=int(failed>0)),indent=2))
sys.exit(int(failed>0))
