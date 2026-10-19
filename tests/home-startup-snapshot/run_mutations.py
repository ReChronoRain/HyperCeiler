#!/usr/bin/env python3
"""Reject real production guard mutations using temporary source copies, never in-place edits."""
import argparse,hashlib,json,re,subprocess,tempfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[2];a.output.mkdir(parents=True,exist_ok=True)
source=r/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java';text=source.read_text()
cases={
 'renew_lease_after_slow_query':('final long deadline = started + HYOS_MAX_SNAPSHOT_AGE_NANOS;','final long deadline = SystemClock.elapsedRealtimeNanos() + HYOS_MAX_SNAPSHOT_AGE_NANOS;'),
 'return_retired_publication':('if (combinedPublished != value || paused) continue;','if (paused) continue;'),
 'old_query_overwrites_new':(' || refreshTicket != ticket',''),
 'off_frame_leaks_enabled_layout':('return new CombinedSnapshot(1, 0, value.epoch(), value.revision(), 0, null);','return new CombinedSnapshot(1, 0, value.epoch(), value.revision(), 0, value.layout());')}
results=[]
with tempfile.TemporaryDirectory(prefix='hc-hcs-mutants-') as tmp:
 for name,(before,after) in cases.items():
  assert before in text,name
  d=Path(tmp)/name;d.mkdir();f=d/source.name;f.write_text(text.replace(before,after));out=a.output/name
  z=subprocess.run(['python3',str(r/'tests/home-startup-snapshot/run_java.py'),'--endpoint-source',str(f),'--output',str(out)],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90)
  out.mkdir(parents=True,exist_ok=True);(out/'runner.txt').write_text(z.stdout)
  m=re.search(r'RESULT passes=(\d+) failures=(\d+)',z.stdout)
  results.append({'mutation':name,'rejected':z.returncode!=0 and m is not None and int(m[2])>0,'exit_code':z.returncode,'passed':int(m[1]) if m else None,'failed':int(m[2]) if m else None,'mutant_sha256':hashlib.sha256(f.read_bytes()).hexdigest(),'replacement':{'before':before,'after':after}})
report={'baseline_source':str(source.relative_to(r.parent)),'baseline_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'cases':results,'all_rejected':all(x['rejected'] for x in results),'limits':'Temporary source-copy host mutations; complete production Java class and tests compile. No APK/device execution.'}
(a.output/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2));raise SystemExit(not report['all_rejected'])
