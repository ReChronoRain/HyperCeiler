"""Portable executable contracts for independent title centering and close caches."""
from pathlib import Path
import json, os, shlex, subprocess, tempfile
root=Path(__file__).resolve().parents[2]
here=Path(__file__).resolve().parent
data=json.loads((here/'9239.json').read_text(encoding='utf8'))
with tempfile.TemporaryDirectory(prefix='hc-folder-title-animation-') as work:
    out=Path(work)
    header='static Functions fixture(){return {\n'
    for f in data['functions']:
        header+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
    header+='};}\nstatic std::array<home_dynamic::Body,7> roots(){return {{\n'
    for f in data['roots']:header+='{'+','.join(map(str,f['words']))+'},\n'
    header+='}};}\n'
    for key in ('text','text_end','editor','style','current','grid','value'):
        header+=f'static constexpr uint32_t {key}={data[key]};\n'
    (out/'fixture.h').write_text(header,encoding='utf8')
    binary=out/'test.exe'
    subprocess.run([*shlex.split(os.environ.get('CXX','c++')),'-std=c++20','-O2','-UNDEBUG',
        '-Wall','-Wextra','-Werror','-I'+str(root/'app/src/main/cpp'),'-I'+str(out),
        str(here/'test.cpp'),'-o',str(binary)],check=True)
    subprocess.run([str(binary)],check=True)
    preview=json.loads((here/'preview-9239.json').read_text(encoding='utf8'))
    fixture='#include <array>\n#include <vector>\n'
    fixture+='static const std::array<std::vector<uint32_t>,14> bodies={{\n'
    for f in preview:fixture+='{'+','.join(map(str,f['words']))+'},\n'
    fixture+='}};\nstatic constexpr std::array<uint32_t,14> addresses={'+','.join(str(f['va']) for f in preview)+'};\n'
    (out/'preview_fixture.h').write_text(fixture,encoding='utf8')
    executable=out/'preview.exe'
    subprocess.run([*shlex.split(os.environ.get('CXX','c++')),'-std=c++20','-O2','-UNDEBUG',
        '-Wall','-Wextra','-Werror','-I'+str(root/'app/src/main/cpp'),'-I'+str(out),
        str(here/'preview_test.cpp'),'-o',str(executable)],check=True)
    subprocess.run([str(executable)],check=True)
    # Execute the real shared-landing capture gate, including a reused frame.
    source=(root/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text(encoding='utf8')
    start=source.index('extern "C" void hc_folder_preview_capture(')
    end=source.index('extern "C" home_layout::FolderPreviewReturnValue hc_folder_preview_take',start)
    harness=r'''
#include "targets/home/home_folder_preview_return_state.h"
#include <cassert>
#include <bit>
uint64_t hc_small_folder_scale_bits=std::bit_cast<uint64_t>(1.0);
uint64_t hc_big_folder_scale_bits=std::bit_cast<uint64_t>(1.0);
uint64_t hc_medium_folder_scale_bits[2]{};
home_layout::FolderPreviewReturnState state;
auto& folder_preview_state(){return state;}
''' + source[start:end] + r'''
int main(){
    constexpr uintptr_t frame=0x1000;
    const auto width=std::bit_cast<uint64_t>(20.0),height=std::bit_cast<uint64_t>(24.0);
    for(double a:{0.6,1.0,1.4})for(double b:{0.6,1.0,1.4}){
        hc_medium_folder_scale_bits[0]=std::bit_cast<uint64_t>(a);
        hc_medium_folder_scale_bits[1]=std::bit_cast<uint64_t>(b);
        hc_folder_preview_capture(frame,width,height);
        assert(!state.take(frame,false).applied&&!state.take(frame,true).applied);
    }
    // Existing explicitly scaled small/large folders retain their legacy pair.
    hc_small_folder_scale_bits=std::bit_cast<uint64_t>(1.2);
    hc_folder_preview_capture(frame,width,height);
    assert(state.take(frame,false).bits==width&&state.take(frame,true).bits==height);
    hc_folder_preview_capture(frame,width,height);
    hc_small_folder_scale_bits=std::bit_cast<uint64_t>(1.0);
    hc_folder_preview_capture(frame,width,height);
    assert(!state.take(frame,false).applied); // No stale pair on the same frame.
}
'''
    (out/'capture.cpp').write_text(harness,encoding='utf8')
    gate=out/'capture.exe'
    subprocess.run([*shlex.split(os.environ.get('CXX','c++')),'-std=c++20','-O2','-UNDEBUG',
        '-Wall','-Wextra','-Werror','-I'+str(root/'app/src/main/cpp'),str(out/'capture.cpp'),'-o',str(gate)],check=True)
    subprocess.run([str(gate)],check=True)
print('Shared landing gate PASS: medium switches do not override unscaled folders; legacy scaled pair retained')
