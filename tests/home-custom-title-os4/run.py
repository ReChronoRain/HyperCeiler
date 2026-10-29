#!/usr/bin/env python3
import argparse,json,subprocess,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[2];here=Path(__file__).parent;cpp=root/'app/src/main/cpp'
p=argparse.ArgumentParser();p.add_argument('--image',type=Path);args=p.parse_args()
with tempfile.TemporaryDirectory(prefix='hc-custom-title-')as tmp:
    out=Path(tmp);fixture=json.loads((here/'fixture-8584.json').read_text());header='static Functions custom_fixture(){return {\n'
    for f in fixture['functions']:header+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
    header+='};}\nstatic constexpr uint32_t custom_app='+str(fixture['app'])+';\nstatic constexpr std::array<uint32_t,2> custom_builders={'+','.join(map(str,fixture['builders']))+'};\n'
    (out/'custom_fixture.h').write_text(header)
    source=(cpp/'targets/home/home_layout_hooks.cpp').read_text();a=source.index('extern "C" uint64_t hc_title_custom_label(');b=source.index('extern "C" uint32_t hc_title_is_new_asset',a)
    (out/'actual_callback.h').write_text(source[a:b])
    subprocess.run(['c++','-std=c++20','-O1','-g','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(cpp),'-I'+str(root/'tests/runtime-feature-addressing'),'-I'+str(out),str(here/'native_test.cpp'),'-o',str(out/'native')],check=True)
    subprocess.run([str(out/'native')]+([str(args.image)]if args.image else[]),check=True)
