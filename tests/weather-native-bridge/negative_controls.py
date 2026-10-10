#!/usr/bin/env python3
"""Change copied production code only; require a runtime assertion failure, never a compile failure."""
from pathlib import Path
import argparse,concurrent.futures,hashlib,json,subprocess,sys
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True);b=Path(__file__).resolve().parent;r=b.parents[1];src=r/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/weather/WeatherNativeEndpointOS4.java';original=src.read_text()
cases=[
 ('drop_caller_verifier',' || !verifier.isWeather(uid, pid)',''),
 ('renew_lease_after_query','published = new Published(values, epoch, ++revision, started + LEASE_NS);','published = new Published(values, epoch, ++revision, now + LEASE_NS);'),
 ('drop_global_gate','boolean allowed = v.allow() && FrameworkHookGate','boolean allowed = FrameworkHookGate'),
 ('default_level_zero','number(row[0], 3)','number(row[0], 0)'),
 ('map_raw_level_plus_one','new Values(level, card, blur,','new Values(level + 1, card, blur,'),
 ('drop_publication_identity_recheck','if (retired || published != value) continue;','if (retired) continue;'),
 ('drop_query_retirement_and_ticket','if (retired || ticket != attempt || revision == Long.MAX_VALUE','if (revision == Long.MAX_VALUE'),
 ('spawn_replacement_for_stuck_query','if (worker != null) return;','// MUTATION: omit single-worker exclusion'),
]
def run(c):
 name,before,after=c;assert original.count(before)==1,(name,original.count(before));d=a.output/name;d.mkdir(parents=True,exist_ok=True);f=d/src.name;f.write_text(original.replace(before,after));cmd=[sys.executable,str(b/'run.py'),'--output',str(d/'run'),'--override-dir',str(d)];z=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=100);(d/'stdout.txt').write_text(z.stdout);p=d/'run/report.json';report=json.loads(p.read_text())if p.exists()else{};rejected=z.returncode==1 and report.get('tests',{}).get('failed',0)>0
 return {'name':name,'rejected_by_runtime_assertion':rejected,'exit_code':z.returncode,'tests':report.get('tests'),'mutated_source_sha256':hashlib.sha256(f.read_bytes()).hexdigest(),'report':str(p)}
with concurrent.futures.ThreadPoolExecutor(max_workers=2)as ex:results=list(ex.map(run,cases))
x={'passed':all(r['rejected_by_runtime_assertion']for r in results),'controls':results,'production_input':{'path':str(src),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()},'boundaries':'Copied source mutations exercised by actual production class host regressions; no Android runtime claim'};(a.output/'report.json').write_text(json.dumps(x,indent=2)+'\n');print(json.dumps(x,indent=2));raise SystemExit(0 if x['passed']else 1)
