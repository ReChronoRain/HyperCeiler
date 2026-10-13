#!/usr/bin/env python3
"""Audit complete actual template bytes from GCC executable and Android PIC ELF."""
import argparse,hashlib,json,struct
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=test_paths.TEST_DATA_ROOT
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
class Elf:
 def __init__(self,p):
  self.raw=p.read_bytes();r=self.raw
  assert r[:6]==b'\x7fELF\x02\x01'and struct.unpack_from('<H',r,18)[0]==183
  off=struct.unpack_from('<Q',r,32)[0];size,count=struct.unpack_from('<HH',r,54);assert size==56
  self.ph=[struct.unpack_from('<IIQQQQQQ',r,off+i*size)for i in range(count)]
  off=struct.unpack_from('<Q',r,40)[0];size,count,names=struct.unpack_from('<HHH',r,58);assert size==64
  rows=[struct.unpack_from('<IIQQQQIIQQ',r,off+i*size)for i in range(count)]
  def sec(x):return r[x[4]:x[4]+x[5]]
  strings=sec(rows[names]);named={strings[x[0]:].split(b'\0',1)[0].decode():x for x in rows}
  st=named['.symtab'];names=sec(rows[st[6]]);self.syms={}
  for n,info,other,ndx,va,sz in struct.iter_unpack('<IBBHQQ',sec(st)):
   if n and ndx:self.syms[names[n:].split(b'\0',1)[0].decode()]=(va,sz,info&15)
 def read(self,a,n):
  matches=[p for p in self.ph if p[0]==1 and p[1]&1 and p[3]<=a and a+n<=p[3]+p[5]];assert len(matches)==1
  p=matches[0];off=p[2]+a-p[3];return self.raw[off:off+n]
def signed(v,bits):return v-(1<<bits)if v&(1<<(bits-1))else v

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--run',type=Path,required=True);ap.add_argument('--output',type=Path,required=True);a=ap.parse_args()
 out=a.output.resolve();out.mkdir(parents=True,exist_ok=False);run=a.run.resolve()
 paths=[run/'normal',run/'libweather-card-template.so'];images=[Elf(p)for p in paths]
 previous=test_paths.TEST_DATA_ROOT / 'acceptance/weather-card-next/gateway-independent-v3/emitted/report.json'
 old=json.loads(previous.read_text());items=[]
 for index,part in enumerate(('recompute','constructor')):
  name='hc_weather_card_'+part+'_template';allbytes=[]
  for image in images:
   start,size,kind=image.syms[name];assert size==284 and kind==2 and start%8==0
   assert image.syms[name+'_code_end'][0]-start==284
   assert image.syms[name+'_lease_literal'][0]-start==288
   assert image.syms[name+'_end'][0]-start==296
   raw=image.read(start,296);assert raw[284:]==bytes(12);allbytes.append(raw)
  assert allbytes[0]==allbytes[1],'Actual Android shared code differs from executed executable template'
  words=struct.unpack('<71I',allbytes[0][:284]);targets=[];literals=[]
  for i,w in enumerate(words):
   pc=i*4;target=None
   if w&0xfc000000==0x14000000:target=pc+4*signed(w&0x3ffffff,26)
   elif w&0xff000010==0x54000000:target=pc+4*signed((w>>5)&0x7ffff,19)
   elif w&0x7e000000==0x34000000:target=pc+4*signed((w>>5)&0x7ffff,19)
   elif w&0x7e000000==0x36000000:target=pc+4*signed((w>>5)&0x3fff,14)
   if target is not None:
    assert 0<=target<284 and target%4==0;targets.append({'offset':pc,'target':target})
   if w&0xff000000==0x58000000:
    target=pc+4*signed((w>>5)&0x7ffff,19);assert target==288 and w&31==17;literals.append(pc)
   assert w&0x1f000000!=0x10000000,'No ADR/ADRP outside copied block'
   assert w&0xfc000000!=0x94000000,'No BL'
   assert w&0xfffffc1f not in (0xd61f0000,0xd63f0000),'No BR/BLR'
  assert len(literals)==2 and words.count(0xd4000001)==1
  assert words[-4:]==(0xaa1603e0,0xaa1d03ef,0xa8c179fd,0xd65f03c0)
  original=struct.unpack('<71I',bytes.fromhex(old['functions'][index]['full_bytes_hex']))
  changed=[i*4 for i,(x,y)in enumerate(zip(words,original))if x!=y]
  assert changed==[x for pos in literals for x in (pos,pos+4)]
  assert all(words[pos//4+1]==0xd503201f for pos in literals)
  items.append({'name':name,'code_size':284,'template_size':296,'lease_literal_offset':288,'literal_load_offsets':literals,'internal_branches':targets,'only_v3_changed_offsets':changed,'code_sha256':hashlib.sha256(allbytes[0][:284]).hexdigest(),'code_hex':allbytes[0][:284].hex(),'shared_and_executed_bytes_identical':True})
 result={'passed':True,'scope':'Full machine bytes and all direct control-flow/literal references of two copyable production templates, not installer or runtime syscall-policy approval','device_status':'NOT_TESTED','templates':items,'inputs':{str(p):sha(p)for p in paths+[previous,Path(__file__)]}}
 (out/'report.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({'passed':True,'templates':2,'full_code_bytes':568,'report_sha256':sha(out/'report.json')}))
if __name__=='__main__':main()
