#!/usr/bin/env python3
"""Reject reintroducing the OEM shared JNI singleton at the Java call site."""
from pathlib import Path
import argparse,importlib.util,json,hashlib
HERE=Path(__file__).resolve().parent;s=importlib.util.spec_from_file_location('ambient_host',HERE/'run.py');h=importlib.util.module_from_spec(s);s.loader.exec_module(h)
p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();o=a.output.resolve();o.mkdir(parents=True,exist_ok=False)
h.MUTANTS['return_to_shared_native']=('AmbientNativeColorOS4.resolve(pixels, sampled.width, sampled.height)','api.maxArea.declaringClass.getDeclaredMethod("getMainColorHCT", IntArray::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!).invoke(null, pixels, sampled.width, sampled.height) as? Int')
positive=h.run(o/'positive');negative=h.run(o/'negative','return_to_shared_native');text=(o/'negative/tests.txt').read_text();required=['native_unavailable_is_not_transparent_success','native_invalid_success_word_rejected'];witnesses={name:('FAIL '+name+' ')in text for name in required}
r={'passed':positive['passed'] and not negative['passed'] and all(witnesses.values()) and negative['inputs_unchanged'],'witnesses':witnesses,'source_sha256':h.sha(h.SOURCE),'bridge_sha256':h.sha(h.NATIVE_SOURCE),'runner_sha256':h.sha(Path(__file__)),'scope':'Complete production Kotlin compiled twice; replacing private bridge call with original shared native array call must violate isolated-bridge unavailable/invalid protocol witnesses. Host JNI implementations are adapters, not actual native isolation proof.','device_status':'NOT_TESTED'};(o/'report.json').write_text(json.dumps(r,indent=2)+'\n');print(r['passed']);raise SystemExit(0 if r['passed']else 1)
