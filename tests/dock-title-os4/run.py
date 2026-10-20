#!/usr/bin/env python3
"""Portable host regression: production resolver + real 7751 words, cache, Java provider/endpoint."""
from pathlib import Path
import json, os, re, shlex, shutil, subprocess, tempfile
ROOT=Path(__file__).resolve().parents[2]; HERE=Path(__file__).resolve().parent
HOME=ROOT/'app/src/main/cpp/targets/home'
def run(args): subprocess.run([str(a) for a in args],check=True)
with tempfile.TemporaryDirectory(prefix='dock-title-os4-') as work:
    out=Path(work)
    fixture=json.loads((HERE/'launcher-7751.json').read_text())
    (out/'fixture.h').write_text('\n'.join('constexpr uint32_t va%d=%d; constexpr uint32_t body%d[]={%s};'%(i,row['va'],i,','.join(hex(w) for w in row['words'])) for i,row in enumerate(fixture)))
    cxx=shlex.split(os.environ.get('CXX','clang++'))
    includes=['-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(out)]
    run(cxx+['-std=c++20','-Wall','-Wextra']+includes+[HERE/'dock_title_test.cpp','-o',out/'resolver'])
    run([out/'resolver'])
    source=(HOME/'home_layout_config.cpp').read_text()
    funcs=source[source.index('void serialize_config('):source.index('bool write_config_cache(')]
    version=re.search(r'constexpr uint32_t kCacheVersion = (\d+);',source).group(1)
    (out/'cache.cpp').write_text('#include "targets/home/home_layout_config.h"\n#include <cstring>\n#include <cassert>\n#include <cstdio>\nusing namespace home_layout;\nconstexpr char kCacheMagic[]={\'H\',\'C\',\'L\',\'C\'};constexpr uint32_t kCacheVersion='+version+';\n'+funcs+r'''
int main(){unsigned checks=0;auto ok=[&](bool b){++checks;assert(b);};
 for(bool value:{false,true}){Config s;s.dock_show_title=value;s.widget_allow_move=true;s.folder_auto_close=true;s.cell_x=4;s.cell_y=6;
 std::vector<uint8_t> raw;serialize_config(s,raw);Config got;ok(parse_config(raw,got)&&got.dock_show_title==value&&got.widget_allow_move&&got.folder_auto_close);
 for(size_t n=1;n<=8;++n){auto cut=raw;cut.resize(cut.size()-n);ok(!parse_config(cut,got));}
 auto bad=raw;bad[bad.size()-4]=2;ok(!parse_config(bad,got));bad=raw;bad[bad.size()-8]^=1;ok(!parse_config(bad,got));
 auto old=raw;old.resize(old.size()-8);old[4]=14;ok(parse_config(old,got)&&!got.dock_show_title&&got.widget_allow_move&&got.folder_auto_close);
 old.resize(old.size()-28);old[4]=13;ok(parse_config(old,got)&&!got.dock_show_title&&got.widget_allow_move&&got.folder_auto_close);
 }printf("dock title cache: %u checks passed (v15 roundtrip, corrupt/truncated, v14/v13 defaults)\n",checks);}
''')
    run(cxx+['-std=c++20']+includes+[out/'cache.cpp','-o',out/'cache']);run([out/'cache'])
    # Existing Android stubs predate the package rename. Translate only the test
    # stubs in a temp directory; compile the production Java sources unmodified.
    sources={}
    for folder in [ROOT/'tests/home-layout-native/stubs',ROOT/'tests/home-dock-window/stubs']:
        for p in folder.rglob('*.java'):
            rel=str(p.relative_to(folder)).replace('com/sevtinge/hyperceiler','com/sevtinge/hyperceiler')
            sources[rel]=p.read_text().replace('com.sevtinge.hyperceiler','com.sevtinge.hyperceiler')
    sources['com/sevtinge/hyperceiler/common/log/ModuleLog.java']='package com.sevtinge.hyperceiler.common.log; public class ModuleLog { public static String last="";public static void i(String tag,String text){last=text;} public static void w(String tag,String text){last=text;} }'
    java=[]
    for rel,text in sources.items():
        p=out/'stubs'/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);java.append(p)
    java += [ROOT/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeLayoutPrefsSnapshot.java',ROOT/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java',ROOT/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeGesturePrefsSnapshot.java',HERE/'EndpointTest.java']
    classes=out/'classes';classes.mkdir()
    run(['javac','-encoding','UTF-8','-d',classes]+java)
    run(['java','-ea','-cp',classes,'com.sevtinge.hyperceiler.libhook.rules.home.dock.EndpointTest'])
