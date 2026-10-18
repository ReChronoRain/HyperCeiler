"""Execute the real scalar banks; the original replay boundary is a stop stub."""
from pathlib import Path
import sys,struct,subprocess,json
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
T=W/'handover/launcher-7751-20261017/visual-compat';out=T/('arm-'+R.name);out.mkdir(parents=True,exist_ok=True)
source=R/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S'
if 'icon_render_bank hc_icon_width_entry' not in source.read_text(encoding='utf8'):print('ICON_RENDER_ARM_IMPLEMENTATION_MISSING');raise SystemExit(1)
clang=Path('D:/build-tools/android-sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64/bin/clang.exe');obj=out/'banks.o'
p=subprocess.run([str(clang),'--target=aarch64-linux-android35','-I'+str(R/'app/src/main/cpp'),'-c',str(source),'-o',str(obj)],capture_output=True,text=True);assert p.returncode==0,p.stderr
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf
sys.path.insert(0,str(W/'tests/back-gesture-os4/runtime'));from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM,UC_HOOK_CODE;from unicorn import arm64_const as A
e=Elf(obj);sy=e.sections[e.section_names['.symtab']];st=e.sections[sy[6]];strings=e.data[st[4]:st[4]+st[5]];symbols=[]
for at in range(sy[4],sy[4]+sy[5],24):
 n,info,other,section,value,size=struct.unpack_from('<IBBHQQ',e.data,at);symbols.append((strings[n:strings.find(b'\0',n)].decode(),section,value,size))
ix=e.section_names['.text'];ts=e.sections[ix];code=bytearray(e.data[ts[4]:ts[4]+ts[5]]);names=['hc_icon_width_entry','hc_icon_height_entry','hc_desktop_icon_width_entry','hc_desktop_icon_height_entry','hc_dock_icon_width_entry','hc_dock_icon_height_entry'];starts={n:next(v for k,s,v,z in symbols if k==n and s==ix) for n in names};begin=min(starts.values());base=0x100000;data=0x300000;stop=0x400000
targets={n:data+i*8 for i,n in enumerate(['hc_icon_render_enabled','hc_folder_title_scale_bits','hc_icon_width_hits','hc_icon_height_hits','hc_desktop_icon_width_hits','hc_desktop_icon_height_hits','hc_dock_icon_width_hits','hc_dock_icon_height_hits','hc_icon_width_original','hc_icon_height_original','hc_desktop_icon_width_original','hc_desktop_icon_height_original','hc_dock_icon_width_original','hc_dock_icon_height_original'])}
# Relocations belong to the six tested banks only. New unrelated stubs can
# follow them in the same object; they are not executed by this harness.
from capstone import Cs,CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN
last=max(starts.values());bank_end=next(i.address+4 for i in Cs(CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN).disasm(bytes(code[last:]),last) if i.mnemonic=='br')
for sec in e.sections.values():
 if sec[1]!=4 or sec[7]!=ix:continue
 for at in range(sec[4],sec[4]+sec[5],24):
  off,info,addend=struct.unpack_from('<QQq',e.data,at)
  if off<begin or off>=bank_end:continue
  name=symbols[info>>32][0];kind=info&0xffffffff;addr=targets[name]+addend;pc=base+off;word=struct.unpack_from('<I',code,off)[0]
  if kind==275:
   pages=(addr>>12)-(pc>>12);word=(word&~((3<<29)|(0x7ffff<<5)))|((pages&3)<<29)|(((pages>>2)&0x7ffff)<<5)
  elif kind==277:word=(word&~(4095<<10))|((addr&4095)<<10)
  elif kind==286:word=(word&~(4095<<10))|(((addr&4095)//8)<<10)
  else:raise AssertionError((name,kind))
  struct.pack_into('<I',code,off,word)
cases=0;checks=0
def ok(v):
 global checks
 checks+=1;assert v,(cases,checks)
for name in names:
 for enabled in [0,1]:
  for scale in [0.59375,1.0,1.1875]:
   for scalar in [40.0,61.23499018086033,92.0]:
    u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
    for a,z in [(base,(len(code)+4095)&~4095),(data,4096),(stop,4096),(0x600000,65536),(0x700000,65536)]:u.mem_map(a,z)
    u.mem_write(base,bytes(code));u.mem_write(data,struct.pack('<'+'Q'*14,enabled,struct.unpack('<Q',struct.pack('<d',scale))[0],*([0]*6),*[stop+i*4 for i in range(6)]))
    regs={i:0x10000000+i*0x1000+cases for i in range(31)};regs[15]=0x608000
    for i,v in regs.items():u.reg_write(getattr(A,'UC_ARM64_REG_X'+str(i)),v)
    simd={i:((i+1)<<80)|(i+100) for i in range(32)};changed=1 if names.index(name) in [0,2,3] else 0;simd[changed]=struct.unpack('<Q',struct.pack('<d',scalar))[0]
    for i,v in simd.items():u.reg_write(getattr(A,'UC_ARM64_REG_Q'+str(i)),v)
    u.reg_write(A.UC_ARM64_REG_SP,0x708000);flags=0xa0000000;u.reg_write(A.UC_ARM64_REG_NZCV,flags)
    seen=[]
    def step(uc,pc,size,unused):
     if pc in [stop+i*4 for i in range(6)]:seen.append(pc);uc.emu_stop()
    u.hook_add(UC_HOOK_CODE,step);u.emu_start(base+starts[name],stop+24,count=200)
    ok(seen==[stop+names.index(name)*4])
    for i,v in regs.items():
     if i not in [16,17]:ok(u.reg_read(getattr(A,'UC_ARM64_REG_X'+str(i)))==v)
    for i,v in simd.items():
     got=u.reg_read(getattr(A,'UC_ARM64_REG_Q'+str(i)))
     if i==changed:ok(struct.unpack('<d',struct.pack('<Q',got&((1<<64)-1)))[0]==scalar*(scale if enabled else 1))
     else:ok(got==v)
    ok(u.reg_read(A.UC_ARM64_REG_SP)==0x708000);ok(u.reg_read(A.UC_ARM64_REG_NZCV)==flags)
    hits=struct.unpack('<'+'Q'*6,u.mem_read(data+16,48));expected=[0]*6;expected[names.index(name)]=enabled;ok(hits==tuple(expected));cases+=1
print(f'ICON_RENDER_ARM cases={cases} checks={checks} failed=0; original replay/GC not emulated')
(out/'result.json').write_text(json.dumps(dict(cases=cases,checks=checks,failed=0)))
