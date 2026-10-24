from pathlib import Path
import sys,json,zipfile,hashlib,subprocess,difflib
D=Path(__file__).resolve().parent;W=D.parents[1]
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(phase,c,input):
 r=subprocess.run(c,capture_output=True,text=True,encoding='utf8',errors='replace')
 record=dict(phase=phase,command=c,input=str(input),stdout=r.stdout,stderr=r.stderr,exit=r.returncode)
 print(phase+': '+r.stdout.strip().split('\n')[-1]+f'; exit={r.returncode}')
 return record
base=json.loads((D/'baseline-hashes.json').read_text(encoding='utf8'))
if len(sys.argv)>1 and sys.argv[1]=='rollback':
 target=Path(sys.argv[2]).resolve()
 if target != (D/'rollback-copy').resolve():raise SystemExit('rollback target must be independent rollback-copy')
 changed=json.loads((D/'modified-hashes.json').read_text(encoding='utf8'))
 assert all(digest(target/p)==h for p,h in changed.items())
 with zipfile.ZipFile(D/'BASELINE.zip') as z:
  for p in base:(target/p).write_bytes(z.read(p))
 assert all(digest(target/p)==h for p,h in base.items())
 print(f'ROLLBACK_HASHES={len(base)} ORIGINAL; HEAD audit failure behavior restored');sys.exit(0)
modified={p:(W/p).read_bytes() for p in base}
(D/'modified-hashes.json').write_text(json.dumps({p:hashlib.sha256(v).hexdigest() for p,v in modified.items()},indent=2),encoding='utf8')
with zipfile.ZipFile(D/'MODIFIED_FILE.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p,data in modified.items():z.writestr(p,data)
patch=[]
with zipfile.ZipFile(D/'BASELINE.zip') as z:
 for p in base:
  assert hashlib.sha256(z.read(p)).hexdigest()==base[p]
  patch.extend(difflib.unified_diff(z.read(p).decode('utf8').splitlines(True),modified[p].decode('utf8').splitlines(True),fromfile='a/'+p,tofile='b/'+p))
  dest=D/'baseline-copy'/p;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z.read(p))
(D/'DIFF_FILE.patch').write_text(''.join(patch),encoding='utf8')
for p,data in modified.items():
 dest=D/'rollback-copy'/p;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(data)
rollback=D/'ROLLBACK.sh'
rollback.write_text('#!/usr/bin/env bash\nset -euo pipefail\nHERE="$(cd "$(dirname "$0")" && pwd)"\nexec python -X utf8 "$HERE/transaction.py" rollback "$1"\n',encoding='utf8')
records=[]
for phase,target in [('BASELINE',D/'baseline-copy'),('MODIFIED',W)]:
 rec=run(phase,[sys.executable,'-X','utf8',str(D/'verify.py'),str(target)],target);records.append(rec)
 assert rec['exit']==(0 if phase=='MODIFIED' else 1),rec
rec=run('ROLLBACK_SCRIPT',['D:/Git/bin/bash.exe',str(rollback),str(D/'rollback-copy')],D/'rollback-copy');records.append(rec);assert rec['exit']==0
rec=run('ROLLBACK',[sys.executable,'-X','utf8',str(D/'verify.py'),str(D/'rollback-copy')],D/'rollback-copy');records.append(rec);assert rec['exit']==1
# Supplementary real production binders and the existing geometry regressions.
rec=run('MODIFIED_BINDINGS',[sys.executable,'-X','utf8',str(D/'verify_bindings.py'),str(W)],W);records.append(rec);assert rec['exit']==0
rec=run('MODIFIED_GEOMETRY',[sys.executable,'-X','utf8',str(D/'verify_geometry.py')],W);records.append(rec);assert rec['exit']==0
v="""HEAD=1d96c1829; reviewed 0ba84123b/6d3234834/1d96c1829.
Changed branches/fields: complete STT_FUNC spans and strict bounds; workspace/hotseat patch sites;
unique Gadget anchors, original return PC, clone/factory guards; signed ARM64 decoding;
per-image widget original restoration; GridInfo columns/rows/origin/strides/item indices/occupied pointer;
Dock count and closure chain; capsule packing splice; indicator gate/result/idle caller;
all title font/height/color/custom/hide splice sites and new-install caller PC.
Grid counts are proved from named currentConfig bounds checks sharing a reject arm;
indices from occupied X/Y arithmetic; same stride contract in workspace/folder/drop paths.
No neighbouring-body or adjacent-slot guesses, no hardcoded patch VA fallback.
Exact frame/GC/register/class/pool ABI guards retained; this is not universal support for arbitrary compilers.
Dart allocation8/nativeSP16 unchanged. No new polling thread/timer/service/wakelock.
44 addressing checks +19 actual-production binding checks pass; six geometry regression suites pass.
Baseline and independent rollback restore HEAD's 36 failing checks of the same 44-check audit;
12 of those are missing new owning-field capabilities, not previously proven runtime crashes.
Release built; no installation/restart/UI/settings/device layout changes. ADB enumeration: empty.
"""
for key,name in [('MODIFIED_FILE','MODIFIED_FILE.zip'),('DIFF_FILE','DIFF_FILE.patch'),('VERIFICATION','VERIFICATION.txt'),('ROLLBACK','ROLLBACK.sh')]:v+=key+'='+str(D/name)+'\n'
v+='Exact BASELINE/MODIFIED/ROLLBACK command, input, literal output and status:\n'+json.dumps(records,ensure_ascii=False,indent=2)
v+='\nBASELINE_HASHES='+json.dumps(base)+'\nMODIFIED_HASHES='+json.dumps(json.loads((D/'modified-hashes.json').read_text()))
v+='\nROLLBACK_COPY=original nine hashes and old audit failures; LIVE_SOURCE=modified nine hashes.\n'
v+='Build exact command/input: C:/Users/XiaoYe/.gradle/wrapper/dists/gradle-9.7.1-bin/1w1c7tv4s851m17nbqdsro2tv/gradle-9.7.1/bin/gradle.bat :app:assembleRelease --offline --no-daemon; input=current worktree, JAVA_HOME=D:/zulu-17; exit=0. Full output: release-build-final.log.\n'
(D/'VERIFICATION.txt').write_text(v,encoding='utf8')
assert all(digest(W/p)==hashlib.sha256(data).hexdigest() for p,data in modified.items())
for name in ['MODIFIED_FILE.zip','DIFF_FILE.patch','VERIFICATION.txt','ROLLBACK.sh']:
 p=D/name
 if name.endswith('.zip'):
  with zipfile.ZipFile(p) as z:assert z.testzip() is None;assert all(z.read(k)==val for k,val in modified.items())
 else:assert p.read_text(encoding='utf8')
 print(name+' REOPEN=PASS')
print('SOURCE_LIVE=MODIFIED; ROLLBACK_COPY=ORIGINAL')
