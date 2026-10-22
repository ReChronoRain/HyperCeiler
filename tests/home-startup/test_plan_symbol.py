#!/usr/bin/env python3
"""Negative control for the precise missing FolderIconGetxController lookup."""
import argparse, hashlib, json, pathlib, resource, subprocess
ROOT=pathlib.Path(__file__).resolve().parents[2];CPP=ROOT/'app/src/main/cpp';BASE=ROOT.parent

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--generated',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,required=True)
    a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False);build=a.generated.resolve()/'build'
    resource.setrlimit(resource.RLIMIT_CORE,(0,0));tweaks=CPP/'targets/home/tweaks';sym=tweaks/'symtab.cpp'
    text=sym.read_text();old='        {"FolderIconGetxController.folderIconSize",\n         "FolderIconGetxController.folderIconSize", "文件夹图标宽高基准"},\n'
    assert text.count(old)==1;changed=out/'symtab-missing-symbol.cpp';changed.write_text(text.replace(old,''))
    assert json.loads((a.generated/'report.json').read_text())['source_inputs'][str(sym.relative_to(ROOT))]==hashlib.sha256(sym.read_bytes()).hexdigest()
    binary=out/'plan-with-missing-symbol'
    cmd=['c++','-std=c++20','-O2','-I'+str(CPP),'-I'+str(build),'-I'+str(tweaks),'-I'+str(ROOT/'tests/home-layout-native/stubs'),
        str(ROOT/'tests/home-startup/plan_fixture.cpp'),str(changed)]
    cmd += [str(tweaks/(n+'.cpp')) for n in ('scanner','image','ht_plan','blob')]
    cmd += [str(build/(n+'.o')) for n in ('xz_crc32','xz_crc64','xz_dec_lzma2','xz_dec_stream')]+['-o',str(binary)]
    compiled=subprocess.run(cmd,capture_output=True,text=True)
    (out/'compile.stdout').write_text(compiled.stdout);(out/'compile.stderr').write_text(compiled.stderr);assert compiled.returncode==0
    run=subprocess.run([str(binary),str(BASE/'firmware/launcher-native/lib/arm64-v8a/libapp.so')],capture_output=True,text=True)
    (out/'run.stdout').write_text(run.stdout);(out/'run.stderr').write_text(run.stderr)
    okay=run.returncode!=0 and 'sq=0' in run.stderr
    report={'passed':okay,'negative_control':'remove only the exact production symbol lookup entry',
        'returncode':run.returncode,'missing_square_guard_detected':'sq=0' in run.stderr,
        'source_inputs':{str(x.relative_to(ROOT)):hashlib.sha256(x.read_bytes()).hexdigest() for x in
            (pathlib.Path(__file__),sym,ROOT/'tests/home-startup/plan_fixture.cpp')},'command':cmd}
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report));assert okay
if __name__=='__main__':main()
