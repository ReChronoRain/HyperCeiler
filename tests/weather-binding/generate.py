#!/usr/bin/env python3
"""Generate every pinned guard byte from the actual four ROM ELFs; standard library only."""
from pathlib import Path
import argparse,hashlib,json,struct

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT
HEADER=test_paths.SOURCE_ROOT / 'app/src/main/cpp/targets/weather/weather_guard.generated.h'
sha=lambda b:hashlib.sha256(b).hexdigest()
class Elf:
 def __init__(self,path,expected):
  self.path=Path(path);self.data=self.path.read_bytes();assert sha(self.data)==expected,(path,'SHA mismatch')
  assert self.data[:6]==b'\x7fELF\x02\x01' and struct.unpack_from('<H',self.data,18)[0]==183
  off=struct.unpack_from('<Q',self.data,32)[0];size,n=struct.unpack_from('<HH',self.data,54);assert size==56
  self.phend=off+size*n;self.ph=[struct.unpack_from('<IIQQQQQQ',self.data,off+i*size)for i in range(n)];self.span=max(x[3]+x[6]for x in self.ph if x[0]==1)
  off=struct.unpack_from('<Q',self.data,40)[0];size,n,names=struct.unpack_from('<HHH',self.data,58);assert size==64
  sections=[struct.unpack_from('<IIQQQQIIQQ',self.data,off+i*size)for i in range(n)];ns=sections[names];strings=self.data[ns[4]:ns[4]+ns[5]];self.sections={strings[s[0]:strings.index(0,s[0])].decode():s for s in sections}
  strings=self.section('.dynstr');self.symbols=[(strings[n:strings.index(0,n)].decode(),value,size,info,index)for n,info,other,index,value,size in struct.iter_unpack('<IBBHQQ',self.section('.dynsym'))]
 def section(self,n):
  s=self.sections[n];return self.data[s[4]:s[4]+s[5]]
 def read(self,va,size,immutable=True):
  found=[s for s in self.ph if s[0]==1 and s[3]<=va and va+size<=s[3]+s[5]];assert len(found)==1,(self.path,hex(va),size)
  p=found[0];assert not immutable or not(p[1]&2),(self.path,hex(va),'mutable guard')
  start=p[2]+va-p[3];return self.data[start:start+size]
 def relocations(self):
  if hasattr(self,'_relocations'):return self._relocations
  out=[]
  for n in ['.rela.dyn','.rela.plt']:
   if n not in self.sections:continue
   data=self.section(n)
   if data.startswith(b'APS2'):
    pos=4
    def read():
     nonlocal pos
     result=0;shift=0
     while True:
      assert pos<len(data) and shift<70
      b=data[pos];pos+=1;result|=(b&127)<<shift;shift+=7
      if not b&128:return result-(1<<shift)if b&64 else result
    count,offset=read(),read();addend=0;rows=[]
    while len(rows)<count:
     group,flags=read(),read();assert group>0 and len(rows)+group<=count and flags&~15==0
     delta=read()if flags&2 else None;info=read()if flags&1 else None
     kind=flags&12
     if kind==12:addend+=read()
     elif kind!=8:addend=0
     for _ in range(group):
      offset+=delta if flags&2 else read()
      if not flags&1:info=read()
      if kind==8:addend+=read()
      rows.append((offset,info,addend))
    assert pos==len(data)
   else:
    assert self.sections[n][1]==4
    rows=struct.iter_unpack('<QQq',data)
   for slot,info,addend in rows:out.append({'slot':slot,'type':info&0xffffffff,'symbol':self.symbols[info>>32][0],'addend':addend})
  self._relocations=out
  return out

def generate():
 evidence={name:ROOT/rel for name,rel in {'channel':'acceptance/weather-port-next/rust-channel/evidence-v1/report.json','allocator':'acceptance/weather-port-next/allocator-evidence/evidence-v1/report.json','dart':'acceptance/weather-port-next/dart-consumer/guard-inputs.json'}.items()};reports={k:json.loads(p.read_text())for k,p in evidence.items()}
 channel,allocator,dart=(reports[k]for k in ['channel','allocator','dart']);dreport=ROOT/dart['evidence_report'];assert sha(dreport.read_bytes())==dart['evidence_report_sha256']
 pinned={Path(v['path']).name:v for v in allocator['inputs']};els={name:Elf(pinned[key]['path'],pinned[key]['sha256'])for name,key in [('Weather','weather-libweather_app.so'),('Public','libhyper_os_system_public.so'),('Standard','libstd.dylib.so')]};els['Dart']=Elf(dart['elf_path'],dart['elf_sha256'])
 for item in channel['inputs']:
  if Path(item['path']).name in pinned:assert item==pinned[Path(item['path']).name]
 regions={k:[]for k in els}
 def add(image,name,va,size,expected=None):
  data=els[image].read(va,size);assert expected is None or sha(data)==expected,(image,name,'region SHA')
  assert all(not(va<=x['slot']<va+size)for x in els[image].relocations()),(image,name,'relocation inside exact guard')
  regions[image].append({'name':name,'va':va,'size':size,'sha256':sha(data),'bytes':data})
 for name in ['Weather','Public','Standard']:
  e=els[name];add(name,'elf-header-and-program-headers',0,e.phend)
  for i,p in enumerate(x for x in e.ph if x[0]==4):add(name,'elf-note-'+str(i),p[3],p[5])
 for r in channel['regions']:add('Weather',r['name'],r['start'],r['size'],r['code_sha256'])
 # Bind immutable strings too; code pointers alone cannot prove the selected channel labels.
 add('Weather','channel-name',channel['channel']['rodata_va'],channel['channel']['length'],sha(channel['channel']['value'].encode()))
 for r in channel['output_fields']:add('Weather','field-'+r['name'],r['va'],len(r['name']),sha(r['name'].encode()))
 functions={}
 for r in allocator['functions']:
  image='Public'if 'public.so' in r['image']else 'Standard';e=els[image];assert any(n==r['name']and v==r['va']and z==r['size']and ix!=0 for n,v,z,_,ix in e.symbols)
  add(image,r['name'],r['va'],r['size'],r['code_sha256']);functions[r['name']]=r['va']
 for r in dart['regions']:
  data=els['Dart'].read(int(r['va'],0),r['size']);assert data.hex()==r['expected_hex'];add('Dart',r['name'],int(r['va'],0),r['size'],r['sha256'])
 weather_imports=[]
 for r in channel['weather_import_slots']:
  assert any(all(x[k]==r[k]for k in ['slot','type','symbol','addend'])for x in els['Weather'].relocations());weather_imports.append((r['slot'],functions[r['symbol']]))
 standard_imports=[]
 for r in allocator['public_imports']:
  assert any(x['slot']==r['slot']and x['symbol']==r['symbol']and x['type']==1026 for x in els['Public'].relocations());standard_imports.append((r['slot'],functions[r['symbol']]))
 vtables=[]
 for r in channel['regions']:
  if r['name']not in ['future-constructor','future-poll']:continue
  for x in r['relocation_refs']:
   assert x['type']==1027 and x['addend']==r['start'] and x in [dict(y,section='.rela.dyn')for y in els['Weather'].relocations()]
   vtables.append((x['slot'],r['start']))
 assert len(vtables)==2 and len(weather_imports)==5 and len(standard_imports)==3
 exports={}
 for x in dart['exports']:
  symbols=[s for s in els['Dart'].symbols if s[0]==x['name'] and s[4]!=0]
  assert len(symbols)==1 and symbols[0][1:3]==(int(x['va'],0),x['size']),('Dart export differs',x['name'])
  exports[x['name']]=symbols[0][1]
 entry=next(r['start']for r in channel['regions']if r['name']=='app-entry')
 constants={'kWeatherEntryVa':entry,'kDartVmDataVa':exports['_kDartVmSnapshotData'],'kDartVmInstructionsVa':exports['_kDartVmSnapshotInstructions'],'kDartIsolateDataVa':exports['_kDartIsolateSnapshotData'],'kDartIsolateInstructionsVa':exports['_kDartIsolateSnapshotInstructions'],'kWeatherGetSlot':weather_imports[0][0],'kPublicGetVa':functions['DeviceLevel_get_device_level'],'kPublicDropVa':functions['DeviceLevel_drop'],'kPublicAllocateSlot':next(s for s,v in standard_imports if v==0x630ac),'kStandardAllocateVa':0x630ac,'kStandardDeallocateVa':0x63110,'kStandardShimVa':0x63284}
 text=['/* SPDX-License-Identifier: AGPL-3.0-or-later */','// Generated by tests/weather-binding/generate.py. Do not hand-edit guard bytes.','#pragma once','#include <array>','#include <cstddef>','#include <cstdint>','#include <span>','namespace weather_guard {','struct Guard { uintptr_t va; std::span<const uint8_t> bytes; };','struct Import { uintptr_t slot; uintptr_t target; };']
 for n,v in constants.items():text.append(f'inline constexpr uintptr_t {n} = 0x{v:x};')
 for image,elf in els.items():
  text.append(f'inline constexpr uintptr_t k{image}ImageSpan = 0x{elf.span:x};')
  text.append(f'inline constexpr char k{image}SourceSha256[] = "{sha(elf.data)}";')
  for i,r in enumerate(regions[image]):
   text += [f'// {r["name"]}: RVA 0x{r["va"]:x}, {r["size"]} complete bytes; SHA256 {r["sha256"]}',f'inline constexpr std::array<uint8_t, {r["size"]}> k{image}Bytes{i} = {{']
   for j in range(0,len(r['bytes']),16):text.append('    '+', '.join(f'0x{x:02x}'for x in r['bytes'][j:j+16])+',')
   text.append('};')
  text.append(f'inline constexpr std::array<Guard, {len(regions[image])}> k{image}Guards = {{{{')
  for i,r in enumerate(regions[image]):text.append(f'    {{0x{r["va"]:x}, k{image}Bytes{i}}},')
  text.append('}};')
 for name,values in [('WeatherImports',weather_imports),('StandardImports',standard_imports),('WeatherVtables',vtables)]:
  text.append(f'inline constexpr std::array<Import, {len(values)}> k{name} = {{{{')
  for slot,target in values:text.append(f'    {{0x{slot:x}, 0x{target:x}}},')
  text.append('}};')
 text.append('} // namespace weather_guard\n')
 manifest={'inputs':{name:{'path':str(e.path),'sha256':sha(e.data),'image_span':e.span,'loads':[{'va':p[3],'file_offset':p[2],'filesz':p[5],'memsz':p[6],'flags':p[1]}for p in e.ph if p[0]==1]}for name,e in els.items()},'evidence_inputs':{str(p):sha(p.read_bytes())for p in [*evidence.values(),dreport]},'guards':{n:[{k:v for k,v in r.items()if k!='bytes'}for r in rows]for n,rows in regions.items()},'constants':constants,'weather_imports':weather_imports,'standard_imports':standard_imports,'weather_vtables':vtables,'aps2_reference':'https://android.googlesource.com/platform/bionic/+/refs/heads/main/linker/linker_reloc_iterators.h (blob c6a8cf620ab2f7d3ea1c7e921f12e06743265fa2)', 'scope':'Exact selected complete functions, immutable labels, full ELF/program headers and notes; no runtime whole-file hash or behavior claim'}
 return '\n'.join(text),manifest

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);p.add_argument('--check',action='store_true');p.add_argument('--header',type=Path,default=HEADER);a=p.parse_args();text,manifest=generate();again,_=generate();assert text==again,'nondeterministic regeneration';a.output.mkdir(parents=True,exist_ok=True)
 if a.check:assert a.header.read_text()==text,'generated header differs'
 else:a.header.parent.mkdir(parents=True,exist_ok=True);a.header.write_text(text)
 manifest.update({'passed':True,'regeneration_identical':True,'mode':'check'if a.check else 'generate','generated_header':{'path':str(a.header),'sha256':sha(a.header.read_bytes())},'generator':{'path':str(Path(__file__).resolve()),'sha256':sha(Path(__file__).read_bytes())}})
 (a.output/'guard-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n');print(json.dumps({'passed':True,'guards':{k:len(v)for k,v in manifest['guards'].items()},'bytes':sum(x['size']for v in manifest['guards'].values()for x in v),'header_sha256':manifest['generated_header']['sha256']}))
if __name__=='__main__':main()
