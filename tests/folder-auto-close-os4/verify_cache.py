from pathlib import Path
import subprocess,os
W=Path(__file__).resolve().parents[2];D=Path(__file__).resolve().parent;R=W
s=(R/'app/src/main/cpp/targets/home/home_layout_config.cpp').read_text(encoding='utf8')
a=s.index('void serialize_config(');b=s.index('bool write_config_cache(',a);f=s[a:b]
f=f.replace('kCacheMagic','cache_magic').replace('kCacheVersion','cache_version')
version=s[s.index('constexpr uint32_t kCacheVersion = '):].split(';',1)[0].split('=')[1].strip()
c='#include "home_layout_config.h"\n#include <cstring>\n#include <iostream>\nusing namespace home_layout;\nconstexpr char cache_magic[4]={\'H\',\'C\',\'L\',\'C\'};constexpr uint32_t cache_version='+version+';\n'+f+r'''
int main(){unsigned checks=0,failed=0;auto ok=[&](bool b){++checks;if(!b)++failed;};
for(bool enabled:{false,true}){Config source;source.cell_x=4;source.cell_y=6;source.folder_auto_close=enabled;source.back_gesture={25,200};std::vector<uint8_t> raw;serialize_config(source,raw);Config got;
ok(parse_config(raw,got)&&got.folder_auto_close==enabled&&got.back_gesture.height==25);
for(size_t remove=1;remove<=8;++remove){auto shortdata=raw;shortdata.resize(raw.size()-remove);ok(!parse_config(shortdata,got));}
auto corrupt=raw;corrupt[raw.size()-4]=2;ok(!parse_config(corrupt,got));corrupt=raw;corrupt[raw.size()-8]^=1;ok(!parse_config(corrupt,got));
auto old=raw;old.resize(raw.size()-8);old[4]=11;ok(parse_config(old,got)&&!got.folder_auto_close&&got.back_gesture.width==200);
old.resize(old.size()-12);old[4]=10;ok(parse_config(old,got)&&!got.folder_auto_close&&got.back_gesture.width==100);
}
std::cout<<"FOLDER_AUTO_CLOSE_CACHE="<<checks<<" checks; failed="<<failed<<"; v12 roundtrip/corrupt/truncation/v10/v11 defaults\n";return failed?1:0;}
'''
out=D/'run';out.mkdir(exist_ok=True);(out/'cache.cpp').write_text(c,encoding='utf8')
zig=os.environ.get('ZIG',str(W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'))
subprocess.run([zig,'c++','-std=c++20','-I'+str(W/'app/src/main/cpp/targets/home'),'-I'+str(W/'app/src/main/cpp'),str(out/'cache.cpp'),'-o',str(out/'cache.exe')],check=True)
subprocess.run([str(out/'cache.exe')],check=True)
