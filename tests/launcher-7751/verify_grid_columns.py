"""Native Rust saved/default column input consumer; real images and scalar-frame isolation."""
from pathlib import Path
import sys,subprocess,json
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
T=W/'handover/launcher-7751-20261017/grid-columns-fix';O=T/('native-'+R.name);O.mkdir(parents=True,exist_ok=True)
if not (R/'app/src/main/cpp/targets/home/home_native_columns.h').exists():
 print('NATIVE_COLUMNS_IMPLEMENTATION_MISSING');raise SystemExit(1)
s=r'''
#include "targets/home/home_native_columns.h"
#include <fstream>
#include <iostream>
using namespace home_layout;
unsigned checks=0,failed=0;void ck(bool b){++checks;if(!b){++failed;std::cout<<"FAIL="<<checks<<"\n";}}
int main(int argc,char**argv){std::ifstream f(argv[1],std::ios::binary);std::vector<char>b((std::istreambuf_iterator<char>(f)),{});std::span<const std::byte>raw{reinterpret_cast<const std::byte*>(b.data()),b.size()};auto t=elf_targets::resolve(raw);ck(bool(t));if(!t)return 1;auto c=resolve_native_columns(raw,*t);ck(bool(c));if(!c)return 1;
std::cout<<"NATIVE_COLUMNS splice="<<std::hex<<c->splice<<" slot="<<c->slot<<" context="<<c->context<<" width="<<c->width<<std::dec<<"\n";
auto im=elf_targets::parse(raw);auto fn=im->bounds(t->grid_config_initializer+4);
uint64_t heightCall=0;for(auto pc=fn->begin;pc<fn->end;pc+=4){auto d=nhk::arm64::branch_target(pc,im->word(pc).value_or(0),true);if(d&&*d==t->grid_height)heightCall=pc;}
for(unsigned columns=1;columns<=8;columns++)for(unsigned saved:{3u,4u,5u,8u})for(bool on:{false,true}){
 std::vector<std::byte> frame(784+c->frame,std::byte{0x5a});std::memcpy(frame.data()+784+c->slot,&saved,4);auto original=frame;
 ck(apply_native_columns(frame.data(),*c,on?(1u|(columns<<8)):0)==on);uint32_t value=0;std::memcpy(&value,frame.data()+784+c->slot,4);ck(value==(on?columns:saved));
 std::memcpy(frame.data()+784+c->slot,&saved,4);ck(frame==original);
}
for(unsigned req:{1u,1u|(9u<<8),1u|(999u<<8)}){std::vector<std::byte> frame(784+c->frame,std::byte{0x66});auto orig=frame;ck(!apply_native_columns(frame.data(),*c,req));ck(frame==orig);}
auto mutate=[&](uint64_t pc,uint32_t word){auto copy=b;auto off=im->file_offset(pc,4);std::memcpy(copy.data()+*off,&word,4);return copy;};
auto moved=mutate(c->splice,*im->word(c->splice)+(1u<<10));auto d=resolve_native_columns({reinterpret_cast<const std::byte*>(moved.data()),moved.size()},*t);ck(d&&d->slot==c->slot+4);
for(auto pc=fn->begin;pc+24<fn->end;pc+=4){auto w=im->word(pc).value_or(0);
 auto use=nhk::arm64::branch_target(pc+12,im->word(pc+12).value_or(0),true);
 if(pc==c->splice || (pc<heightCall&&(w&0xffe0ffffu)==0x2a0003e2u&&use&&*use==c->width) || ((w&0xffe0001fu)==0x52800002u&&((w>>5)&65535)==c->bytes)){
  auto copy=mutate(pc,0xd503201f);auto accepted=resolve_native_columns({reinterpret_cast<const std::byte*>(copy.data()),copy.size()},*t);if(accepted)std::cout<<"ACCEPTED_MUTANT="<<std::hex<<pc<<std::dec<<"\n";ck(!accepted);
 }}
auto wrong=*t;wrong.grid_height+=4;ck(!resolve_native_columns(raw,wrong));
std::cout<<"NATIVE_COLUMNS checks="<<checks<<" failed="<<failed<<"; real Rust images + native scalar isolation/disabled/mutation; not full launcher emulation\n";return failed?1:0;}
'''
(O/'test.cpp').write_text(s,encoding='utf8');zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
p=subprocess.run([str(zig),'c++','-std=c++20','-I'+str(R/'app/src/main/cpp'),str(O/'test.cpp'),'-o',str(O/'test.exe')],capture_output=True,text=True,encoding='utf8',errors='replace')
if p.returncode:print(p.stdout+p.stderr);raise SystemExit(p.returncode)
rows=[]
for v,path in [('7751',W/'handover/launcher-7751-20261017/libapp_launcher.so'),('8244',W/'handover/gestures-os4-20261014/libapp_launcher.so')]:
 c=[str(O/'test.exe'),str(path)];p=subprocess.run(c,capture_output=True,text=True,encoding='utf8',errors='replace');rows.append(dict(version=v,command=c,stdout=p.stdout,stderr=p.stderr,exit=p.returncode));print(v,p.stdout.strip(),flush=True)
(O/'results.json').write_text(json.dumps(rows,indent=2));raise SystemExit(any(x['exit'] for x in rows))
