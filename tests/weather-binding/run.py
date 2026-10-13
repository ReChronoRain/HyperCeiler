#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,importlib.util,json,re,subprocess,sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--binding-header',type=Path);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True);b=Path(__file__).resolve().parent;root=test_paths.TEST_DATA_ROOT;src=test_paths.SOURCE_ROOT;sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
z=subprocess.run([sys.executable,str(b/'generate.py'),'--check','--output',str(a.output/'regeneration')],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(a.output/'regeneration.txt').write_text(z.stdout)
if z.returncode:print(z.stdout);raise SystemExit(z.returncode)
m=json.loads((a.output/'regeneration/guard-manifest.json').read_text());images=['Weather','Public','Standard','Dart'];assert [len(m['guards'][n])for n in images]==[11,8,6,9];assert sum(r['size']for n in images for r in m['guards'][n])==9005
# Whole functions and independent contract offsets, separate from production's runtime guard tables.
assert {r['name']for r in m['guards']['Dart']}=={'elf-prefix','map_min','query_normalize_cache','copy_cached_level','is_high_predicate','is_low_or_other_flag_predicate','is_middle_predicate','vm-snapshot-header','isolate-snapshot-header'}
assert m['constants']['kWeatherEntryVa']==0x2a56a8 and m['constants']['kDartVmDataVa']==0x340
lines=['#pragma once','#include <array>','#include <span>','namespace fixture {','struct Load {uint64_t va,offset,filesz,memsz;};','struct Import {uintptr_t slot,target;};','struct Region {size_t image;uintptr_t va;size_t size;};','inline constexpr std::array<uintptr_t,4> bases={0x100000000ULL,0x200000000ULL,0x300000000ULL,0x400000000ULL};']
for n in images:
 loads=m['inputs'][n]['loads'];lines.append(f'inline constexpr std::array<Load,{len(loads)}> {n.lower()}_loads={{{{');lines += ['{%s,%s,%s,%s},'%(x['va'],x['file_offset'],x['filesz'],x['memsz'])for x in loads];lines.append('}};')
lines.append('inline constexpr std::array<std::span<const Load>,4> loads={weather_loads,public_loads,standard_loads,dart_loads};')
for name,key in [('weather_imports','weather_imports'),('standard_imports','standard_imports'),('vtables','weather_vtables')]:
 values=m[key];lines.append(f'inline constexpr std::array<Import,{len(values)}> {name}={{{{');lines += ['{%s,%s},'%tuple(x)for x in values];lines.append('}};')
regions=[(i,r['va'],r['size'])for i,n in enumerate(images)for r in m['guards'][n]];lines.append(f'inline constexpr std::array<Region,{len(regions)}> regions={{{{');lines+=['{%s,%s,%s},'%r for r in regions];lines+=['}};','}'];fixture=a.output/'fixture.generated.h';fixture.write_text('\n'.join(lines)+'\n')
production=[src/'app/src/main/cpp/targets/weather'/f for f in ['weather_guard.generated.h','weather_binding.h','weather_device_level.h','weather_snapshot.h']];include=src/'app/src/main/cpp'
if a.binding_header:
 include=a.output/'override';d=include/'targets/weather';d.mkdir(parents=True,exist_ok=True)
 for f in production:(d/f.name).write_bytes((a.binding_header if f.name=='weather_binding.h'else f).read_bytes())
 production=[a.binding_header if f.name=='weather_binding.h'else f for f in production]
exe=a.output/'binding-test';cmd=['g++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-Wno-mismatched-new-delete','-pthread','-fsanitize=undefined','-fno-sanitize-recover=all','-I',str(include),'-I',str(a.output),str(b/'binding_test.cpp'),'-o',str(exe)];z=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(a.output/'build.txt').write_text(z.stdout)
if z.returncode:print(z.stdout);raise SystemExit(z.returncode)
z=subprocess.run([str(exe.resolve()),*[m['inputs'][n]['path']for n in images]],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=120);(a.output/'tests.txt').write_text(z.stdout);print(z.stdout,end='');match=re.search(r'RESULT passes=(\d+) failures=(\d+) guard_bit_mutations=(\d+)',z.stdout)
report={'passed':z.returncode==0 and match is not None and int(match[2])==0,'tests':{'passed':int(match[1])if match else 0,'failed':int(match[2])if match else -1,'guard_bit_mutations':int(match[3])if match else 0},'exit_code':z.returncode,'command':cmd,'production_inputs':{str(f):sha(f)for f in production},'test_inputs':{str(f):sha(f)for f in [b/'run.py',b/'generate.py',b/'binding_test.cpp',fixture]},'guard_extraction_report':{'path':str(a.output/'regeneration/guard-manifest.json'),'sha256':sha(a.output/'regeneration/guard-manifest.json')},'elf_inputs':m['inputs'],'limits':['Actual ROM ELF PT_LOAD bytes and production binding verifier; relocations simulated only for the exact8 imports and2 Weather vtable pointers','Every guard bit tested; no guarded object function executed, no actual ELF loader or imports patched','Global new throws during resolver/Dart verifier; reader uses preallocated maps, max256-byte stack read chunks','Successful output assignment is logically final in this function, not atomic inter-thread struct publication; caller must publish prepared result safely','Actual memory reader/protection, import installation, TOCTOU after validation and real phone behavior remain caller responsibilities; phone NOT_TESTED']};(a.output/'report.json').write_text(json.dumps(report,indent=2)+'\n');raise SystemExit(0 if report['passed']else 1)
