#!/usr/bin/env python3
"""OS4 first-render bootstrap, drawer graph, policy and settings integration."""
import argparse,hashlib,json,struct,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--image7751',type=Path);parser.add_argument('--image8583',type=Path);parser.add_argument('--image8584',type=Path);args=parser.parse_args()
def run(cmd):subprocess.run(list(map(str,cmd)),check=True)
for version in ('7751','8583','8584'):
    data=json.loads((HERE/f'drawer-{version}.json').read_text());source=getattr(args,'image'+version)
    if source:
        raw=source.read_bytes();assert hashlib.sha256(raw).hexdigest()==data['source_sha256']
        ph=struct.unpack_from('<Q',raw,32)[0];count=struct.unpack_from('<H',raw,56)[0]
        loads=[struct.unpack_from('<IIQQQQQQ',raw,ph+i*56)for i in range(count)]
        for f in data['functions']:
            va=f['va'];size=len(f['words'])*4;p=next(p for p in loads if p[0]==1 and p[3]<=va and va+size<=p[3]+p[5])
            assert list(struct.unpack_from('<'+'I'*len(f['words']),raw,p[2]+va-p[3]))==f['words']
    with tempfile.TemporaryDirectory(prefix='hc-layout-drawer-')as work:
        out=Path(work);header='static Functions drawer_fixture(){return {\n'
        for f in data['functions']:header+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
        header+='};}\n'
        for key in ('owner','site','width','caller','closure','empty_site','holder','blank_factory','bottom','content','color','context_allocator','common'):header+=f'static constexpr uint32_t {key}={data[key]};\n'
        header+=f'static constexpr int32_t child_frame={data["child_frame"]};\n'
        (out/'fixture.h').write_text(header)
        run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(out),HERE/'native_test.cpp','-o',out/'native'])
        run([out/'native']+([source]if source else[]))
hooks=(ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
def function(name):
    at=hooks.index(name+' {');start=hooks.index('{',at);depth=1;end=start+1
    while depth:
        depth+=(hooks[end]=='{')-(hooks[end]=='}');end+=1
    return hooks[at:end]
bootstrap=function('void prime_home_layout_first_render_locked()')
assert 'query_config(' not in bootstrap and 'delay_ms(' not in bootstrap and 'HomeLayoutEngineFindSymbol' not in bootstrap
with tempfile.TemporaryDirectory(prefix='hc-layout-first-render-')as work:
    out=Path(work);src=(HERE/'bootstrap_test.inc').read_text()+bootstrap+r'''
int main(){
    // Simulate the consumer caching all icon widgets and Dock offset immediately
    // after callback return, before the general worker can scan/bind anything.
    cached.drawer_hide_search=true;cached.tweaks.icon_scale_enabled=true;cached.knobs[0]={true,35};
    prime_home_layout_first_render_locked();
    assert(g_drawer_search_ready&&hc_icon_render_enabled&&scale==1.5&&margin==105&&!g_first_render_discovery);
    assert(hc_hotseat_margin_caller_count==2&&hc_hotseat_margin_callers[0]==0x102e&&hc_hotseat_margin_callers[1]==0x1032);
    auto queries=config_reads;prime_home_layout_first_render_locked();assert(config_reads==queries);
    // A disabled feature and delta zero are authoritative. Zero means 70 dp,
    // not a disabled slider; failed installs must never publish a live gate.
    hc_icon_render_enabled=0;g_knobs[0].hook_armed=false;cached.tweaks.icon_scale_enabled=false;cached.knobs[0]={true,0};
    prime_home_layout_first_render_locked();assert(scale==1&&margin==70);
    hc_icon_render_enabled=0;g_knobs[0].hook_armed=false;allow_install=false;
    prime_home_layout_first_render_locked();assert(!hc_icon_render_enabled&&!g_knobs[0].hook_armed&&margin==-1);
    allow_install=true;have_config=false;scale=1.25;
    prime_home_layout_first_render_locked();assert(hc_icon_render_enabled&&scale==1.25&&!g_first_render_discovery);
    hc_icon_render_enabled=0;g_knobs[0].hook_armed=false;graph=false;
    prime_home_layout_first_render_locked();assert(!g_first_render_discovery);
    std::cout<<"first-render production bootstrap PASS: before first consumer, idempotence, on/off, zero margin, failed install, busy cache and discovery scope\n";
}
'''
    (out/'bootstrap.cpp').write_text(src)
    run(['c++','-std=c++20','-I'+str(ROOT/'app/src/main/cpp'),out/'bootstrap.cpp','-o',out/'bootstrap']);run([out/'bootstrap'])
entry=(ROOT/'app/src/main/cpp/targets/home/hyperceiler_home.cpp').read_text()
callback=entry[entry.index('void on_library_loaded('):]
assert callback.index('prime_home_layout_first_render(')<callback.index('prime_home_layout_knobs(')
worker=hooks[hooks.index('void *worker('):hooks.index('void prime_home_layout_first_render_locked() {')]
assert worker.index('prime_home_layout_first_render_locked();')<worker.index('std::optional<Located> located')
binding=function('bool bind_drawer_search()')
assert 'g_runtime_features.drawer' in binding and 'HomeLayoutEngineFindSymbol' not in binding and 'dartscan::' not in binding
import xml.etree.ElementTree as ET
root=ET.parse(ROOT/'library/core/src/main/res/xml/home_drawer.xml').getroot();android='{http://schemas.android.com/apk/res/android}'
preferences=[p for p in root.iter() if p.get(android+'key')=='prefs_key_home_drawer_hide_search']
assert len(preferences)==1 and preferences[0].get(android+'defaultValue')=='false'
provider=(ROOT/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeLayoutPrefsSnapshot.java').read_text()
assert 'home_drawer_hide_search' in provider
writer=(ROOT/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeDockWindow.kt').read_text()
assert 'HomeLayoutNativeEndpointOS4.writeDrawer(reply, result?.drawerHideSearch() ?: false)' in writer
print('first-render/drawer production integration PASS: early callback + worker adoption, UI/provider/Binder writer, executable-only bank')
