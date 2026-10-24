from pathlib import Path
import sys,subprocess
W=Path(__file__).resolve().parents[2]; ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
D=W/'handover/search-margin-8244-20261009';D.mkdir(exist_ok=True)
base=(W/'tests/dynamic-address-audit/verify_bindings.py').read_text(encoding='utf8')
base=base[:base.index("pre+=fun('bool capsule_wrapper_layout_compatible('")]
base=base.replace("D=Path(__file__).resolve().parent", "D=W/'tests/dynamic-address-audit'")
base=base.replace("ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else D.parents[1]", "ROOT=ROOT")
for version,fixture,expected in [('7722',Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f'),0x15c9),('8244',W/'handover/folder-return-8244-20261009',0x15e7)]:
 a=base.index("R=Path('C:/");b=base.index('\nsec=',a)
 code=base[:a]+f"R=Path({str(fixture)!r});e=Elf(R/'launcher-{version}-libapp.so');sym=Symbols(R/'launcher-{version}-mini.elf');mini=Elf(R/'launcher-{version}-mini.elf')"+base[b:]
 env=dict(globals());exec(code,env)
 pre=env['pre']+env['fun']('bool capsule_wrapper_layout_compatible(')
 pre+='\nint main(){int failed=0;\n'
 for n,(v,z) in env['sym'].by_name.items():
  if n in env['names']:pre+=f'symbols["{n}"]={{{v},{z}}};\n'
 for v,words in env['images'].items():pre+=f'images[{v}]={{{",".join(hex(w) for w in words)}}};\n'
 for v,z in env['spans'].items():pre+=f'spans[{v}]={z};\n'
 registry=(ROOT/'app/src/main/cpp/targets/home/tweaks/symtab.cpp').read_text(encoding='utf8')
 if '"Workspace._createIndicator"' not in registry:pre+='symbols.erase("Workspace._createIndicator");'
 v=env['sym'].by_name['Workspace._createIndicator'][0]
 pre+=f'uint32_t patch=0;bool ok=capsule_wrapper_layout_compatible({v},0,&patch);failed+=!ok;'
 if 'uint32_t *edge_cid' in env['source']:pre+=f'uint32_t cid=0;failed+=!capsule_wrapper_layout_compatible({v},0,&patch,&cid)||cid!={expected};'
 pre+='if(ok){'
 pre+=f'auto original=images[{v}];auto span=spans[{v}];unsigned k=(patch-{v})/4;'
 pre+=f'for(unsigned back:{{1u,2u,3u,4u,5u,6u}}){{images[{v}]=original;images[{v}][k-back]=0xd503201f;uint32_t untouched=0xabcdef;failed+=capsule_wrapper_layout_compatible({v},0,&untouched)||untouched!=0xabcdef;}}'
 pre+=f'images[{v}]=original;images[{v}][k+1]^=1;uint32_t untouched=0xabcdef;failed+=capsule_wrapper_layout_compatible({v},0,&untouched)||untouched!=0xabcdef;images[{v}]=original;spans[{v}]=span;'
 pre+='}'
 pre+=f'std::cout<<"SEARCH_MARGIN_{version}="<<(!failed?"PASS":"BIND_FAILED")<<"\\n";return failed?1:0;}}'
 out=D/('host-'+ROOT.name+'-'+version);out.mkdir(exist_ok=True);(out/'test.cpp').write_text(pre)
 zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
 p=subprocess.run([str(zig),'c++','-std=c++20','-I'+str(ROOT/'app/src/main/cpp/targets/home'),str(out/'test.cpp'),'-o',str(out/'test.exe')],capture_output=True,text=True);print(p.stdout+p.stderr,end='')
 if p.returncode:sys.exit(p.returncode)
 p=subprocess.run([str(out/'test.exe')],capture_output=True,text=True);print(p.stdout+p.stderr,end='')
 if p.returncode:sys.exit(p.returncode)
