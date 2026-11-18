"""Exercise split width/current-config call linkage without symbols or fixed addresses."""
from pathlib import Path
import json, os, shlex, subprocess, tempfile
here=Path(__file__).resolve().parent
root=here.parents[1]
data=json.loads((here/'small-9239.json').read_text())
with tempfile.TemporaryDirectory(prefix='hc-small-folder-') as directory:
    out=Path(directory)
    header='static Functions fixture(){return {\n'
    for f in data['functions']:
        header+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
    header+='};}\n'
    for key in ('config','small'):header+=f'static constexpr uint32_t {key}={data[key]};\n'
    (out/'fixture.h').write_text(header)
    binary=out/'test.exe'
    subprocess.run([*shlex.split(os.environ.get('CXX','c++')),'-std=c++20','-O2','-UNDEBUG',
        '-Wall','-Wextra','-Werror','-I'+str(root/'app/src/main/cpp'),'-I'+str(out),
        str(here/'small_runtime_test.cpp'),'-o',str(binary)],check=True)
    subprocess.run([str(binary)],check=True)
