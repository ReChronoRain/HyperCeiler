"""Validate production 3x1 folder recognition using executable instructions only."""
from pathlib import Path
import json, os, shlex, subprocess, tempfile
ROOT=Path(__file__).resolve().parents[2]
HERE=Path(__file__).resolve().parent
data=json.loads((HERE/'9239.json').read_text(encoding='utf-8'))
with tempfile.TemporaryDirectory(prefix='hc-medium-folder-') as directory:
    out=Path(directory)
    header='static Functions fixture(){return {\n'
    for row in data['functions']:
        header+='{'+str(row['va'])+',{'+','.join(map(str,row['words']))+'}},\n'
    header+='};}\n'
    for key in ('config','producer','preview','origin','landing'):header+=f'static constexpr uint32_t {key}={data[key]};\n'
    (out/'fixture.h').write_text(header,encoding='utf-8')
    compiler=shlex.split(os.environ.get('CXX','c++'))
    executable=out/('test.exe' if os.name=='nt' else 'test')
    subprocess.run(compiler+['-std=c++20','-O2','-UNDEBUG','-Wall','-Wextra','-Werror',
        '-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(out),str(HERE/'test.cpp'),'-o',str(executable)],check=True)
    subprocess.run([str(executable)],check=True)
