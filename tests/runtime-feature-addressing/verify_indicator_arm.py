#!/usr/bin/env python3
"""Execute shipping indicator banks and the slide bank's original replay; pass NDK path.

No Dart VM, animation scheduler or launcher UI is emulated. The branch destinations,
scalar state, Dart boolean/pool replay and preservation of CPU state are checked.
"""
import json
from pathlib import Path
import struct
import subprocess
import sys
import tempfile
from capstone import Cs,CS_ARCH_ARM64,CS_MODE_ARM
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM
from unicorn import arm64_const as A
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tests/home-layout-native'))
from dart_dump import Elf
clang=next((Path(sys.argv[1])/'toolchains/llvm/prebuilt').glob('*/bin/clang'))
with tempfile.TemporaryDirectory(prefix='hc-indicator-arm-') as tmp:
    obj=Path(tmp)/'bank.o'
    subprocess.run([str(clang),'--target=aarch64-linux-android35','-I'+str(ROOT/'app/src/main/cpp'),'-c',str(ROOT/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S'),'-o',str(obj)],check=True)
    e=Elf(obj);tab=e.sections[e.section_names['.symtab']];st=e.sections[tab[6]];strings=e.data[st[4]:st[4]+st[5]];symbols=[]
    for p in range(tab[4],tab[4]+tab[5],24):
        n,_,_,section,va,size=struct.unpack_from('<IBBHQQ',e.data,p)
        symbols.append((strings[n:strings.find(b'\0',n)].decode(),section,va,size))
    text=e.section_names['.text'];s=e.sections[text];original=e.data[s[4]:s[4]+s[5]]
    BASE,DATA,REPLAY,STOP,DOTS,EMPTY,EDIT=0x100000,0x300000,0x400000,0x401000,0x402000,0x403000,0x404000
    globs={n:DATA+i*128 for i,n in enumerate(['hc_layout_indicator_mode','hc_layout_indicator_edit_call',
        'hc_layout_dart_SearchBarMargin_original','hc_layout_dart_IndicatorDot_hits','hc_layout_indicator_build_dots',
        'hc_indicator_empty_pool','hc_layout_indicator_build_empty','hc_layout_dart_IndicatorDot_original',
        'hc_layout_indicator_idle_caller','hc_layout_dart_SearchBarWidth_original'])}
    def bank(name,next_name):
        start=next(v for n,_,v,_ in symbols if n==name);end=next(v for n,_,v,_ in symbols if n==next_name)
        code=bytearray(original)
        assert not any(i.mnemonic in ('bl','blr') for i in Cs(CS_ARCH_ARM64,CS_MODE_ARM).disasm(original[start:end],start))
        for sec in e.sections.values():
            if sec[1]!=4 or sec[7]!=text:continue
            for p in range(sec[4],sec[4]+sec[5],24):
                at,info,add=struct.unpack_from('<QQq',e.data,p)
                if not start<=at<end:continue
                address=globs[symbols[info>>32][0]]+add;kind=info&0xffffffff;w=struct.unpack_from('<I',code,at)[0]
                if kind==275:
                    pages=(address>>12)-((BASE+at)>>12)
                    w=(w&~((3<<29)|(0x7ffff<<5)))|((pages&3)<<29)|(((pages>>2)&0x7ffff)<<5)
                elif kind==277:w=(w&~(4095<<10))|((address&4095)<<10)
                elif kind==286:w=(w&~(4095<<10))|(((address&4095)//8)<<10)
                else:raise AssertionError((name,kind))
                struct.pack_into('<I',code,at,w)
        return bytes(code),BASE+start
    def setup(code):
        u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
        for addr,size in [(BASE,(len(code)+4095)&~4095),(DATA,4096),(REPLAY,32768),(0x600000,65536),(0x700000,65536)]:u.mem_map(addr,size)
        u.mem_write(BASE,code)
        r={i:0x10000+i*256 for i in range(31)};r.update({15:0x608000,29:0x609000,27:0x700000})
        q={i:((i+17)<<80)|i+123 for i in range(32)}
        for i,v in r.items():u.reg_write(getattr(A,'UC_ARM64_REG_X'+str(i)),v)
        for i,v in q.items():u.reg_write(getattr(A,'UC_ARM64_REG_Q'+str(i)),v)
        u.reg_write(A.UC_ARM64_REG_SP,0x708000);u.reg_write(A.UC_ARM64_REG_NZCV,0xa0000000)
        return u,r,q
    def put(u,name,value):u.mem_write(globs[name],struct.pack('<Q',value))
    def preserved(u,r,q,allowed=(),flags=True):
        for i,v in r.items():
            if i not in (*allowed,16,17):assert u.reg_read(getattr(A,'UC_ARM64_REG_X'+str(i)))==v,(i,allowed)
        for i,v in q.items():assert u.reg_read(getattr(A,'UC_ARM64_REG_Q'+str(i)))==v
        assert u.reg_read(A.UC_ARM64_REG_SP)==0x708000
        if flags:assert u.reg_read(A.UC_ARM64_REG_NZCV)==0xa0000000
    cases=0
    code,start=bank('hc_layout_indicator_policy_entry','hc_layout_indicator_edit_result_entry')
    for mode in range(3):
        u,r,q=setup(code);put(u,'hc_layout_indicator_mode',mode);put(u,'hc_layout_indicator_edit_call',EDIT);put(u,'hc_layout_dart_SearchBarMargin_original',REPLAY)
        destination=EDIT if mode==2 else REPLAY
        u.emu_start(start,destination,count=2000);assert u.reg_read(A.UC_ARM64_REG_PC)==destination
        preserved(u,r,q);cases+=1
    fixture=json.loads((Path(__file__).with_name('indicator-7751.json')).read_text())
    policy=next(f['words'] for f in fixture['functions'] if f['va']==fixture['roots']['policy'])
    results=[i for i in range(len(policy)-6) if policy[i]&0xfff8001f==0x36200000
             and policy[i+2]&0xfff8001f==0x36200000 and policy[i+3]&0xffc003ff==0xf9400360
             and policy[i+4:i+7]==[0xaa1d03ef,0xa8c179fd,0xd65f03c0]]
    assert len(results)==1;original_pool=((policy[results[0]+3]>>10)&4095)*8
    code,start=bank('hc_layout_indicator_edit_result_entry','hc_layout_indicator_slide_only_entry')
    for mode in range(3):
        for editing in (False,True):
            for pool in (original_pool,original_pool+8):
                u,r,q=setup(code);r[0]=0x8020 if editing else 0x8030;u.reg_write(A.UC_ARM64_REG_X0,r[0])
                for n,v in [('hc_layout_indicator_mode',mode),('hc_layout_indicator_build_dots',DOTS),('hc_indicator_empty_pool',pool),('hc_layout_indicator_build_empty',EMPTY),('hc_layout_dart_IndicatorDot_original',REPLAY)]:put(u,n,v)
                widget=0x60a001;u.mem_write(r[27]+pool,struct.pack('<Q',widget))
                destination=REPLAY if mode!=2 else DOTS if editing else EMPTY
                u.emu_start(start,destination,count=2000);assert u.reg_read(A.UC_ARM64_REG_PC)==destination
                changed=mode==2 and not editing
                assert u.reg_read(A.UC_ARM64_REG_X0)==(widget if changed else r[0])
                assert struct.unpack('<Q',u.mem_read(globs['hc_layout_dart_IndicatorDot_hits'],8))[0]==int(mode==2)
                preserved(u,r,q,(0,) if changed else ());cases+=1
    show=next(f['words'] for f in fixture['functions'] if f['va']==fixture['roots']['show'])
    def imm9(w):
        n=(w>>12)&511
        return n-512 if n&256 else n
    matches=[i for i in range(len(show)-3) if show[i]&0xffe00fff==0xf84003a0 and show[i+1]&0xffe00fff==0xf80003a3 and show[i+2]&0xffe00fff==0xf8400002 and show[i+3]==0xeb03005f]
    assert len(matches)==1;replay=show[matches[0]:matches[0]+4]
    code,start=bank('hc_layout_indicator_slide_only_entry','hc_layout_container_probe_entry')
    for mode in range(3):
        for request in range(5):
            for idle in (False,True):
                for current in (0,3):
                    u,r,q=setup(code);r[3]=request;u.reg_write(A.UC_ARM64_REG_X3,request)
                    put(u,'hc_layout_indicator_mode',mode);put(u,'hc_layout_indicator_idle_caller',0x1234);put(u,'hc_layout_dart_SearchBarWidth_original',REPLAY)
                    u.mem_write(r[29]+8,struct.pack('<Q',0x1234 if idle else 0x5678))
                    owner=0x60a001;u.mem_write(r[29]+imm9(replay[0]),struct.pack('<Q',owner));u.mem_write(owner+imm9(replay[2]),struct.pack('<Q',current))
                    u.mem_write(REPLAY,struct.pack('<4I',*replay))
                    u.emu_start(start,REPLAY+16,count=2000);assert u.reg_read(A.UC_ARM64_REG_PC)==REPLAY+16
                    expected=3 if request==0 and (mode==2 or mode==1 and idle) else request
                    assert u.reg_read(A.UC_ARM64_REG_X3)==expected
                    assert struct.unpack('<Q',u.mem_read(r[29]+imm9(replay[1]),8))[0]==expected
                    assert u.reg_read(A.UC_ARM64_REG_X0)==owner and u.reg_read(A.UC_ARM64_REG_X2)==current
                    preserved(u,r,q,(0,2,3),False)
                    difference=(current-expected)&((1<<64)-1)
                    flags=((difference>>63)<<31)|(int(current==expected)<<30)|(int(current>=expected)<<29)
                    assert u.reg_read(A.UC_ARM64_REG_NZCV)==flags
                    cases+=1
    print(f'INDICATOR_ARM PASS: {cases} shipping policy/dot/slide cases, original slide replay, decoded pool, edit/idle semantics and GPR/SIMD/SP/NZCV preservation')
