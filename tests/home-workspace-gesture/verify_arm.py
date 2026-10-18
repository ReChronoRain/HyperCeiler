"""Execute the actual assembly adapters. C callback is a hostile ABI stub, not Dart/GC emulation."""
from pathlib import Path
import struct,subprocess,sys
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
D=W/'handover/gestures-os4-20261014/device/systemui-coexist/workspace-source-20261016'
source=R/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S'
if 'hc_workspace_double_callback_entry:' not in source.read_text():
 print('WORKSPACE_ARM=missing source adapters; failed=1');raise SystemExit(1)
clang=Path('D:/build-tools/android-sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64/bin/clang.exe')
obj=D/'workspace-stubs.o'
p=subprocess.run([str(clang),'--target=aarch64-linux-android35','-I'+str(R/'app/src/main/cpp'),'-c',str(source),'-o',str(obj)],capture_output=True,text=True)
assert p.returncode==0,p.stderr
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf
sys.path.insert(0,str(W/'tests/back-gesture-os4/runtime'))
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM,UC_HOOK_CODE
from unicorn import arm64_const as A
e=Elf(obj);sy=e.sections[e.section_names['.symtab']];st=e.sections[sy[6]];strings=e.data[st[4]:st[4]+st[5]];symbols=[]
for at in range(sy[4],sy[4]+sy[5],24):
 n,info,other,section,value,size=struct.unpack_from('<IBBHQQ',e.data,at)
 symbols.append((strings[n:strings.find(b'\0',n)].decode(),section,value,size))
ix=e.section_names['.text'];ts=e.sections[ix];code=bytearray(e.data[ts[4]:ts[4]+ts[5]])
names=['hc_workspace_double_policy_entry','hc_workspace_double_callback_entry','hc_workspace_status_true_entry','hc_workspace_status_register_entry','hc_workspace_status_field_entry']
starts={n:next(v for k,s,v,z in symbols if k==n and s==ix) for n in names}
begin=min(starts.values());base=0x100000;data=0x300000;emit=0x400000
targets={'hc_workspace_double_enabled':data,'hc_workspace_double_policy_original':data+8,
 'hc_workspace_double_callback_original':data+16,'hc_workspace_double_resume':data+24,
 'hc_workspace_double_emit':emit,'hc_workspace_double_selected':data+32,
 'hc_workspace_double_policy_resume':data+40,'hc_workspace_double_rx':data+48,
 'hc_workspace_double_frame':data+56,'hc_workspace_double_field':data+64,
 'hc_workspace_swipe_enabled':data+72,'hc_workspace_scope_authorize':emit+256,'hc_workspace_status_result':emit+512}
for sec in e.sections.values():
 if sec[1]!=4 or sec[7]!=ix:continue
 for at in range(sec[4],sec[4]+sec[5],24):
  off,info,addend=struct.unpack_from('<QQq',e.data,at)
  if off<begin:continue
  name=symbols[info>>32][0];kind=info&0xffffffff;addr=targets[name]+addend;pc=base+off
  word=struct.unpack_from('<I',code,off)[0]
  if kind==275:
   pages=(addr>>12)-(pc>>12);word=(word&~((3<<29)|(0x7ffff<<5)))|((pages&3)<<29)|(((pages>>2)&0x7ffff)<<5)
  elif kind==277:word=(word&~(4095<<10))|((addr&4095)<<10)
  elif kind==286:word=(word&~(4095<<10))|(((addr&4095)//8)<<10)
  elif kind==283:word=0x94000000|(((addr-pc)//4)&0x3ffffff)
  else:raise AssertionError((name,kind))
  struct.pack_into('<I',code,off,word)
checks=0
for name in names:
 for enabled in [0,1]:
  for seed in [1,7,123,2,8,124]:
   selected=seed%2;stock=seed%2;frame=-8 if seed%2 else -16;field=71 if seed%2 else 75
   u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
   for address,size in [(base,(len(code)+4095)&~4095),(data,4096),(emit,4096),(0x500000,4096),(0x600000,65536),(0x700000,65536)]:u.mem_map(address,size)
   u.mem_write(base,bytes(code));u.mem_write(data,struct.pack('<QQQQQQQqQ',enabled,0x500800,0x500100,0x500200,selected,0x500000,0x500300,frame,field))
   u.mem_write(data+72,struct.pack('<I',seed%2))
   # Original policy epilogue and its real caller-owned Dart frame.
   u.mem_write(0x500000,struct.pack('<III',0xaa1d03ef,0xa8c179fd,0xd65f03c0))
   regs={i:0x10000000+i*0x1000+seed for i in range(31)};regs[15]=0x608000;regs[29]=0x60a000;regs[22]=0x10016001
   obj=0x60c001;rxvalue=0x123
   u.mem_write(regs[29]+frame,struct.pack('<Q',obj));u.mem_write(obj+field,struct.pack('<I',rxvalue));u.mem_write(regs[29],struct.pack('<QQ',0x60b000,0x500400))
   for i,value in regs.items():u.reg_write(getattr(A,'UC_ARM64_REG_X'+str(i)),value)
   simd={i:((i+seed)<<80)|(i*123+seed) for i in range(32)}
   for i,value in simd.items():u.reg_write(getattr(A,'UC_ARM64_REG_Q'+str(i)),value)
   u.reg_write(A.UC_ARM64_REG_SP,0x708000);flags=(seed&15)<<28;u.reg_write(A.UC_ARM64_REG_NZCV,flags)
   seen=[];emitted=[];rx=[];authorized=[];filtered=[]
   def step(uc,pc,size,unused):
    if pc in [0x500400,0x500100,0x500200]:seen.append(pc);uc.emu_stop()
    elif pc==0x500300:
     assert uc.reg_read(A.UC_ARM64_REG_LR)==0x500000,'original Dart GC return PC moved'
     assert uc.reg_read(A.UC_ARM64_REG_X0)==obj
     assert uc.reg_read(A.UC_ARM64_REG_X1)==(rxvalue+(regs[28]<<32))&((1<<64)-1)
     rx.append(pc);uc.reg_write(A.UC_ARM64_REG_X0,regs[22]+(32 if stock else 48));uc.reg_write(A.UC_ARM64_REG_PC,0x500000)
    elif pc==emit+512:
     filtered.append(pc);lr=uc.reg_read(A.UC_ARM64_REG_LR)
     assert uc.reg_read(A.UC_ARM64_REG_X0)==regs[29] and uc.reg_read(A.UC_ARM64_REG_X1)==regs[28] and uc.reg_read(A.UC_ARM64_REG_X2)==regs[21]
     assert uc.reg_read(A.UC_ARM64_REG_X4)==names.index(name)-2 and uc.reg_read(A.UC_ARM64_REG_X5)==regs[22]
     for i in list(range(19))+[30]:uc.reg_write(getattr(A,'UC_ARM64_REG_X'+str(i)),0xdeadbeef+i)
     for i in range(32):uc.reg_write(getattr(A,'UC_ARM64_REG_Q'+str(i)),0xfedcba9876543210+i)
     uc.reg_write(A.UC_ARM64_REG_X0,regs[22]+(32 if seed%2 else 48));uc.reg_write(A.UC_ARM64_REG_PC,lr)
    elif pc==emit or pc==emit+256:
     if pc==emit:emitted.append(pc)
     else:
      authorized.append(pc)
      assert [uc.reg_read(getattr(A,'UC_ARM64_REG_X'+str(i))) for i in range(3)]==[regs[29],regs[28],regs[21]]
     lr=uc.reg_read(A.UC_ARM64_REG_LR)
     for i in list(range(19))+[30]:uc.reg_write(getattr(A,'UC_ARM64_REG_X'+str(i)),0xdeadbeef+i)
     for i in range(32):uc.reg_write(getattr(A,'UC_ARM64_REG_Q'+str(i)),0xfedcba9876543210+i)
     uc.reg_write(A.UC_ARM64_REG_NZCV,0xf0000000);uc.reg_write(A.UC_ARM64_REG_PC,lr)
   u.hook_add(UC_HOOK_CODE,step);u.emu_start(base+starts[name],base+len(code),count=500)
   policy=name==names[0];status=name in names[2:];expected=0x500400 if policy or status else 0x500200 if selected else 0x500100
   assert seen==[expected] and len(emitted)==int(not policy and not status and selected),(name,enabled,seen,emitted)
   assert len(rx)==int(policy and not enabled)
   assert len(authorized)==int(policy and seed%2)
   assert len(filtered)==int(status)
   for i,value in regs.items():
    if i in [16,17]:continue
    want=value
    if policy or status:
     if i==0 and status:want=regs[22]+(32 if seed%2 else 48)
     elif i==0:want=regs[22]+(32 if enabled or stock else 48)
     elif i==1 and policy and not enabled:want=(rxvalue+(regs[28]<<32))&((1<<64)-1)
     elif i==15:want=regs[29]+16
     elif i==29:want=0x60b000
     elif i==30:want=0x500400
    assert u.reg_read(getattr(A,'UC_ARM64_REG_X'+str(i)))==want,(name,enabled,i)
   for i,value in simd.items():assert u.reg_read(getattr(A,'UC_ARM64_REG_Q'+str(i)))==value,(name,enabled,'q',i)
   assert u.reg_read(A.UC_ARM64_REG_SP)==0x708000 and u.reg_read(A.UC_ARM64_REG_NZCV)==flags
   assert struct.unpack('<I',bytes(u.mem_read(data+32,4)))[0]==(enabled if policy else selected)
   checks+=1
print(f'WORKSPACE_ARM={checks} cases; failed=0; status return filtering / scope C boundary / final Rx preference / original GC return PC / caller epilogue / bool / GPR/SIMD/two stacks; callees stubbed')
