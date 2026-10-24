from pathlib import Path
import sys,subprocess,json
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W;D=W/'handover/desktop-columns-20261018'/('rows-'+R.name);D.mkdir(parents=True,exist_ok=True)
s=(R/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text('utf8');a=s.index('extern "C" int hc_grid_autofit_body(uintptr_t saved, uintptr_t frame, unsigned kind) {');i=s.index('{',a);j=i+1;n=1
while n:
 if s[j]=='{':n+=1
 if s[j]=='}':n-=1
 j+=1
body=s[a:j]
source=r'''
#include "targets/home/home_grid_autofit.h"
#include <cstring>
#include <vector>
#include <array>
#include <iostream>
uint32_t hc_grid_autofit_requested=0;home_layout::GridAutofitSites g_grid_autofit_fields;
'''+body+r'''
unsigned checks=0,failed=0;void ck(bool v){checks++;if(!v){failed++;std::cout<<"FAIL="<<checks<<"\n";}}
int main(){auto&f=g_grid_autofit_fields;f.width=7;f.height=15;f.dock=23;f.rows=31;f.legacy_dock=23;f.legacy_height=15;
for(int old:{6,7,9,12})for(int request:{4,6,7,10})for(bool enabled:{false,true}){
 alignas(16)std::array<std::byte,256> owner{},box{};alignas(16)std::array<std::byte,704> saved{};alignas(16)std::array<std::byte,128> frame{};
 uintptr_t op=(uintptr_t)owner.data()+1,bp=(uintptr_t)box.data()+1,sp=(uintptr_t)saved.data(),fp=(uintptr_t)frame.data()+64;
 auto put=[](uintptr_t p,auto v){std::memcpy((void*)p,&v,sizeof(v));};auto rd=[](uintptr_t p){uint64_t v;std::memcpy(&v,(void*)p,8);return v;};auto num=[](uintptr_t p){double v;std::memcpy(&v,(void*)p,8);return v;};
 put(fp-8,op);put(sp,bp);put(sp+24,uint64_t(old));put(sp+32,uint64_t(old+1));put(op+7,60.0);put(op+15,60.0);put(op+23,60.0);put(op+31,int64_t(old));put(bp+7,100.0);hc_grid_autofit_requested=enabled?(1u|(unsigned(request)<<8)):0;
 auto o=owner;auto b=box;auto s=saved;auto fr=frame;int result=hc_grid_autofit_body(sp,fp,1);
 ck(result==(enabled?1:0));
 if(enabled){ck(rd(op+31)==unsigned(request));ck(std::abs(num(op+15)-(60.0*(old+1)+100.0-60.0)/request)<1e-9);ck(rd(sp+24)==unsigned(request));ck(rd(sp+32)==unsigned(request+1));
  std::memcpy(owner.data()+16,o.data()+16,8);std::memcpy(owner.data()+32,o.data()+32,8);
  std::memcpy(saved.data()+24,s.data()+24,16);std::memcpy(saved.data()+160,s.data()+160,48);
 }
 ck(owner==o&&box==b&&saved==s&&frame==fr);
}
std::cout<<"GRID_ROWS_INPUT checks="<<checks<<" failed="<<failed<<"; actual body + fresh scalar owner; native boundary only, not full launcher\n";return failed?1:0;}
'''
(D/'test.cpp').write_text(source,encoding='utf8');zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe';p=subprocess.run([str(zig),'c++','-std=c++20','-I'+str(R/'app/src/main/cpp'),'-I'+str(W/'app/src/main/cpp'),'-I'+str(W/'app/src/main/cpp/targets/home'),str(D/'test.cpp'),'-o',str(D/'test.exe')],capture_output=True,text=True,encoding='utf8')
if p.returncode:print(p.stdout+p.stderr);raise SystemExit(p.returncode)
p=subprocess.run([str(D/'test.exe')],capture_output=True,text=True,encoding='utf8');print(p.stdout+p.stderr,end='');(D/'result.json').write_text(json.dumps(dict(stdout=p.stdout,stderr=p.stderr,exit=p.returncode),indent=2),encoding='utf8');raise SystemExit(p.returncode)
