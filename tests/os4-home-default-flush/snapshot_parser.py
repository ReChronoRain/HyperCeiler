#!/usr/bin/env python3
"""Exploratory bounded parser; all inferred layout must be validated before evidence use."""
from pathlib import Path
import re,struct,json
BASE=Path(__file__).resolve().parents[3]/'acceptance/weather-port-next/dart-consumer'
class Reader:
 def __init__(self,b,p=0):self.b=b;self.p=p
 def n(self,marker=128):
  out=0;shift=0
  for _ in range(10):
   x=self.b[self.p];self.p+=1
   if x>=128:return out+((x-marker)<<shift)
   out|=x<<shift;shift+=7
  raise ValueError('overflow')
 def s(self):return self.n(192)
 def ref(self):
  out=0
  for _ in range(4):
   x=self.b[self.p];self.p+=1;out=(out<<7)+(x&127)
   if x>=128:return out
  raise ValueError('ref overflow')
def cids():
 s=(BASE/'official-dart-3.10.1/runtime_vm_class_id.h').read_text().replace('\\\n',' ')
 macros=dict(re.findall(r'#define (CLASS_LIST\w*)\(V\)\s+([^\n]+)',s))
 def expand(name):
  out=[]
  for a,b in re.findall(r'(CLASS_LIST\w*)\(V\)|V\((\w+)\)',macros[name]):out+=expand(a) if a else[b]
  return out
 n=['Illegal','NativePointer','FreeListElement','ForwardingCorpse']+expand('CLASS_LIST')+['Ffi'+x for x in expand('CLASS_LIST_FFI')]
 for x in expand('CLASS_LIST_TYPED_DATA'):n+=['TypedData'+x,'TypedData'+x+'View','ExternalTypedData'+x,'UnmodifiableTypedData'+x+'View']
 n+=['ByteDataView','UnmodifiableByteDataView','ByteBuffer','Null','Dynamic','Void','Never']
 return n

def alloc(b,base):
 r=Reader(b,b.index(b'\0',base+52)+1);header=[r.n() for _ in range(5)];names=cids();print('header',hex(base),header,'at',hex(r.p),'cids',len(names))
 nb,no,nc,*_=header; idx=nb+1;clusters=[]
 for ci in range(nc):
  at=r.p;tags=r.s();cid=tags>>12;canon=bool(tags&2);name=names[cid] if cid<len(names) else 'Instance';count=r.n()
  c=dict(index=ci,at=at,tags=tags,cid=cid,name=name,canonical=canon,count=count,start=idx)
  try:
   assert 0<=count<=no,(count,no)
   if name=='Class':
    c['predefined']=[r.s() for _ in range(count)];second=r.n();c['firstcount']=count;count+=second;c['count']=count
   elif name=='Code':
    c['states']=[r.s() for _ in range(count)];nd=r.n();c['deferred']=[r.s() for _ in range(nd)];count+=nd;c['count']=count
   elif name=='Mint':c['values']=[r.s() for _ in range(count)]
   elif name=='Instance':c['nextfield']=r.s();c['size']=r.s()
   elif name in ['ObjectPool','PcDescriptors','CodeSourceMap','CompressedStackMaps','ExceptionHandlers','Context','ContextScope','TypeArguments','Record','String','Array','ImmutableArray','WeakArray'] or name.startswith(('TypedData','ExternalTypedData')) and not name.endswith('View'):
    c['lengths']=[r.n() for _ in range(count)]
   if canon and not (base==0x340 and name=='String') and name in ['String','TypeArguments','Type','FunctionType','RecordType','TypeParameter']:
    c['tablelength']=r.n();c['firstelement']=r.n();c['gaps']=[r.n() for _ in range(count-c['firstelement'])]
   assert cid>0 and cid<20000 and idx+count-1<=no
  except Exception as e: print('FAIL',ci,hex(at),c,e);raise
  idx+=count; c['end']=r.p;clusters.append(c)
  print(ci,hex(at),name,cid,canon,count,hex(r.p))
 assert idx-1==no,(idx-1,no)
 return dict(header=header,clusters=clusters,fillstart=r.p)
if __name__=='__main__':
 b=(BASE.parents[2]/'acceptance/native-evidence/weather-libapp.so').read_bytes()
 a=alloc(b,0x4ac0);(BASE/'alloc-exploration.json').write_text(json.dumps(a,indent=2)+'\n')

def strings(b,a):
 c=a['clusters'][0];r=Reader(b,a['fillstart']);out={}
 for i,length in enumerate(c['lengths']):
  at=r.p;encoded=r.n();assert length==encoded,(i,hex(at),length,encoded)
  n=encoded>>1;raw=b[r.p:r.p+n*(1+(encoded&1))];assert len(raw)==n*(1+(encoded&1));r.p+=len(raw)
  out[c['start']+i]={'offset':at,'text':raw.decode('utf-16le' if encoded&1 else 'latin1')}
 print('STRINGS',len(out),'end',hex(r.p));return out,r.p

def enc(v):
 out=[]
 while v>=128:out.append(v&127);v>>=7
 return bytes(out+[v+128])
def pools(b,a):
 c=next(c for c in a['clusters'] if c['name']=='ObjectPool');assert c['count']==1
 length=c['lengths'][0];token=enc(length);found=[];at=a['fillstart']
 while True:
  at=b.find(token,at+1)
  if at<0:break
  r=Reader(b,at);assert r.n()==length;entries=[]
  try:
   for i in range(length):
    pos=r.p;bits=b[r.p];r.p+=1;behavior=bits>>5;typ=bits&15
    assert behavior in [0,2,3,4] and typ<=2
    v=None
    if behavior==0:
     if typ==1:v=r.ref();assert 0<v<=a['header'][1]
     elif typ==0:v=r.s()
    entries.append(dict(index=i,offset=pos,bits=bits,value=v))
   found.append(dict(start=at,end=r.p,entries=entries))
  except (AssertionError,IndexError,ValueError):pass
 print('POOLS',[(hex(p['start']),hex(p['end'])) for p in found]);assert len(found)==1
 return found[0]
if __name__=='__main__':
 ss,_=strings(b,a);pp=pools(b,a);wanted={k:v for k,v in ss.items() if any(t.lower() in v['text'].lower() for t in ['DeviceLevel','cpu_level','gpu_level','ram_level'])};print('WANTED',wanted)
 for e in pp['entries']:
  if e['value'] in wanted:print('POOLMATCH',hex(e['index']*8+16),e,wanted[e['value']])
 (BASE/'snapshot-strings.json').write_text(json.dumps(ss,indent=2,ensure_ascii=False)+'\n');(BASE/'snapshot-pool.json').write_text(json.dumps(pp,indent=2)+'\n')
def first_fills(b,a,stop=None):
 r=Reader(b,a['fillstart']);objs={};spans=[]
 def ref():
  v=r.ref();assert 0<v<=a['header'][1],(hex(r.p),v);return v
 def refs(n):return[ref() for _ in range(n)]
 def byte():v=b[r.p];r.p+=1;return v
 for c in a['clusters'][:stop]:
  at=r.p;name=c['name'];bitmap=r.n() if name=='Instance' else None
  for j in range(c['count']):
   pos=r.p;ob={'cid':c['cid'],'at':pos,'name':name};rr=[]
   if name=='String':
    n=r.n();assert n==c['lengths'][j];nb=(n>>1)*(1+(n&1));ob['text']=b[r.p:r.p+nb].decode('utf-16le' if n&1 else 'latin1',errors='surrogatepass');r.p+=nb
   elif name=='Mint':ob['value']=c['values'][j]
   elif name=='Double':ob['bits']=r.s()
   elif name=='TypeParameter':rr=refs(3);ob['flags']=[r.s(),r.s(),byte()]
   elif name=='Type':rr=refs(3);ob['flags']=r.n()
   elif name=='TypeArguments':
    n=r.n();assert n==c['lengths'][j];ob['hash']=r.s();ob['nullability']=r.n();rr=refs(n+1)
   elif name=='Code':ob['payload_info']=r.n();rr=refs(6)
   elif name=='Function':rr=refs(4);ob['codeindex']=r.n();ob['kindtag']=r.s()
   elif name=='Closure':rr=refs(6)
   elif name=='Instance':
    for word in range(2,c['nextfield']):
     if bitmap&(1<<word):rr.append({'wordparts':[r.s(),r.s()]})
     else:rr.append(ref())
   elif name=='FunctionType':rr=refs(6);ob['flags']=[byte(),r.s(),r.s()]
   elif name=='RecordType':rr=refs(4);ob['flags']=byte()
   elif name=='Record':
    shape=r.n();assert shape&65535==c['lengths'][j];ob['shape']=shape;rr=refs(shape&65535)
   elif name in ('ConstMap','ConstSet'):rr=refs(5)
   elif name in ('Array','ImmutableArray'):
    n=r.n();assert n==c['lengths'][j],(c['index'],j,hex(pos),n,c['lengths'][j]);rr=refs(n+1)
   elif name=='Class':
    rr=refs(13);ob['classid']=r.s();ob['fields']=[r.s() for _ in range(6)]
    if j<c['firstcount'] or ob['classid']<(1<<20):ob['bitmap']=r.n()
   elif name=='PatchClass':rr=refs(2)
   elif name=='TypeParameters':rr=refs(4)
   elif name=='ClosureData':rr=refs(2);ob['packed']=r.n()
   elif name=='FfiTrampolineData':rr=refs(4);ob['id']=r.s();ob['kind']=byte()
   elif name=='Field':rr=refs(4);ob['kind']=r.s();ob['fieldid']=ref()
   elif name=='Script':rr=refs(1);ob['index']=r.s()
   elif name=='Library':rr=refs(10);ob['fields']=[r.s(),r.s(),byte(),byte()]
   elif name=='WeakArray':
    n=r.n();assert n==c['lengths'][j];rr=refs(n)
   elif name=='ObjectPool':
    n=r.n();assert n==c['lengths'][j];entries=[]
    for k in range(n):
     bits=byte();behavior=bits>>5;typ=bits&15;assert behavior in [0,2,3,4] and typ<=2
     value=ref() if behavior==0 and typ==1 else r.s() if behavior==0 and typ==0 else None
     entries.append({'bits':bits,'value':value})
    ob['entries']=entries
   elif name=='PcDescriptors':
    n=r.n();assert n==c['lengths'][j];r.p+=n
   elif name=='ExceptionHandlers':
    packed=r.n();n=packed>>1;assert n==c['lengths'][j],(n,c['lengths'][j]);rr=refs(1)
    ob['handlers']=[[r.s(),r.s(),byte(),byte(),byte()] for _ in range(n)]
   elif name=='UnlinkedCall':rr=refs(2);ob['patch']=byte()
   elif name=='SubtypeTestCache':rr=refs(1);ob['flags']=[r.s(),r.s()]
   elif name=='LoadingUnit':rr=refs(1);ob['id']=r.s()
   elif name=='GrowableObjectArray':rr=refs(3)
   elif name in ['TypedDataInt8Array','TypedDataInt32Array','TypedDataUint32Array']:
    n=r.n();assert n==c['lengths'][j];r.p+=n*(1 if name=='TypedDataInt8Array' else 4)
   else:raise ValueError(name)
   ob['refs']=rr;ob['end']=r.p;objs[c['start']+j]=ob
  spans.append({'index':c['index'],'name':name,'start':at,'end':r.p});print('FILL',c['index'],name,hex(at),hex(r.p))
 return objs,spans
if __name__=='__main__':
 objs,spans=first_fills(b,a);(BASE/'first-objects.json').write_text(json.dumps(objs,separators=(',',':'))+'\n');(BASE/'fill-spans.json').write_text(json.dumps(spans,indent=2)+'\n');print('CHANNEL',objs[86595])
def roots(b,a,spans):
 s=(BASE/'official-dart-3.10.1/runtime_vm_object_store.h').read_text();s=s[s.index('#define OBJECT_STORE_FIELD_LIST'):s.index('/* Roots for JIT/AOT snapshots')]
 names=re.findall(r'(?:R_|RW|ARW_RELAXED|ARW_AR|LAZY_\w+)\(\w+,\s*(\w+)\)',s)
 r=Reader(b,spans[-1]['end']);store=[r.ref() for _ in names];n=r.n();fields=[r.ref() for _ in range(n)];ns=r.n();shared=[r.ref() for _ in range(ns)]
 assert all(0<x<=a['header'][1] for x in store+fields+shared)
 at=r.p;nd=r.n();firstcode=r.n() if nd else None;recent=[None]*64;ri=0;value=None;dispatch=[]
 while len(dispatch)<nd:
  v=r.s()
  if v==0:value=0;dispatch.append(value)
  elif v<0:
   assert ~v<64;value=recent[~v];assert value is not None;dispatch.append(value)
  elif v<=63:dispatch.extend([value]*v)
  else:
   value=v-64;recent[ri]=value;ri=(ri+1)&63;dispatch.append(value)
 assert len(dispatch)==nd
 return {'start':spans[-1]['end'],'object_store':dict(zip(names,store)),'fields':fields,'shared':shared,'dispatch_start':at,'dispatch_length':nd,'dispatch_first_code':firstcode,'dispatch':dispatch,'end':r.p}
if __name__=='__main__':
 rt=roots(b,a,spans);(BASE/'roots-exploration.json').write_text(json.dumps(rt,indent=2)+'\n');print('ROOTS END',hex(rt['end']),'EXPECTED',hex(0x4ac0+struct.unpack_from('<Q',b,0x4ac4)[0]+4));assert rt['end']==0x4ac0+struct.unpack_from('<Q',b,0x4ac4)[0]+4
