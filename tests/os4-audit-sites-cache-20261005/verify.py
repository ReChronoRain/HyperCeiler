from pathlib import Path
import sys, re, subprocess, os, json, shutil

D = Path(__file__).resolve().parent
W = D.parents[1]
R = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else W
run = D / ('run-' + R.name)
run.mkdir(exist_ok=True)

def source(name):
    return (R / 'app/src/main/cpp/targets/home/tweaks' / name).read_text(encoding='utf8')

def strip(s):
    return '\n'.join(l for l in s.splitlines() if not l.startswith(('#include', '#pragma')))

def function(s, key):
    a = s.index(key); b = s.index('{', a); n = 1; j = b + 1
    while n:
        n += (s[j] == '{') - (s[j] == '}'); j += 1
    return s[a:j]

# Execute production cache, wire parser/serializer, executable-range and image-range logic.
# Only Linux syscalls/stat and live-memory reads are host boundary substitutes.
pre = (D / 'boundary.cpp').read_text(encoding='utf8')
header = source('ht_plan.h')
types = header[header.index('constexpr uint32_t kMaxNoClearSites'):header.index('struct PlanPatch')]
plan = source('ht_plan.cpp')
decl = strip(source('image.h')) + '\nnamespace hometweaks { struct FunctionSignature {}; }\n' + strip(source('scanner.h'))
decl += '\nnamespace hometweaks {\n' + types
decl += '\nbool SerializeSites(const LocatedSites&, std::vector<uint8_t>*);\nbool ParseSites(const uint8_t*,size_t,LocatedSites*);\n'
decl += function(source('image.cpp'), 'bool RangeInImage(') + '\n'
decl += function(source('scanner.cpp'), 'bool CodeView::ExecutableRangesOk()') + '\n'
decl += function(plan, 'struct ByteWriter') + ';\n' + function(plan, 'struct ByteReader') + ';\n'
decl += 'constexpr uint32_t kSitesPayloadVersion = 9;\n'
decl += function(plan, 'bool SerializeSites(') + '\n' + function(plan, 'bool ParseSites(') + '\n}\n'
production = strip(source('htcache.cpp'))
production = re.sub(r'\bstruct stat\b', 'struct fixture_stat', production)
production = re.sub(r'\boff_t\b', 'fixture_off_t', production)
calls = ['open', 'openat', 'mkdir', 'mkdirat', 'fstat', 'read', 'pread', 'write', 'fchmod', 'fsync', 'close', 'rename', 'renameat', 'unlink', 'unlinkat', 'geteuid', 'getegid', 'getpid']
for call in calls:
    production = re.sub(r'(?<![\w:])' + call + r'\s*\(', 'mock_' + call + '(', production)
cpp = pre + '\n' + decl + '\n' + production + '\n' + (D / 'cases.cpp').read_text(encoding='utf8')
(run / 'production.cpp').write_text(cpp, encoding='utf8')
os.environ['ZIG_GLOBAL_CACHE_DIR'] = str(W / 'tests/dynamic-address-audit/runtime/cache')
os.environ['ZIG_LOCAL_CACHE_DIR'] = str(run / 'cache')
exe = run / 'test.exe'
cmd = [str(W / 'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'), 'c++', '-std=c++20', '-Wall', '-Wextra', '-Werror', '-Wno-unused-function', '-Wno-unused-const-variable', '-Wno-unused-parameter', str(run / 'production.cpp'), '-o', str(exe)]
records = []
p = subprocess.run(cmd, capture_output=True, text=True, encoding='utf8', errors='replace')
records.append(dict(command=cmd,input=str(R),output=p.stdout+p.stderr,exit=p.returncode))
print(p.stdout+p.stderr,end='')
if p.returncode:
    (run/'commands.json').write_text(json.dumps(records,indent=2),encoding='utf8');sys.exit(1)
cases = '''symbol-valid symbol-limit symbol-crc symbol-magic symbol-version symbol-length symbol-truncated symbol-owner symbol-mode symbol-link symbol-id identity-valid identity-unsampled identity-rodata identity-live-patch identity-empty identity-noexec identity-wx identity-livefault identity-partialfault identity-inode identity-readfault identity-statfault identity-mutation identity-closefault identity-eintr identity-extent identity-budget identity-machine identity-phoff identity-load-extent identity-inventory identity-apk identity-real cache-valid cache-zero cache-link cache-owner cache-mode cache-fifo cache-directory-owner cache-directory-link cache-public cache-eintr cache-mutation cache-reentrant cache-close cache-tail cache-boolean cache-crc cache-id cache-user save-valid save-temp save-eintr save-chmod save-filesync save-dirsync save-close save-rename save-write save-zero save-count save-icons save-empty drop-private'''.split()
failed = 0
for case in cases:
    fixture = run / ('fixture-' + case); fixture.mkdir(exist_ok=True)
    # Only direct files under this resolved fixture are cleared; no recursive delete.
    for f in fixture.iterdir():
        assert f.resolve().parent == fixture.resolve()
        if f.is_file(): f.unlink()
    if case == 'identity-real':
        shutil.copyfile('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f/launcher-7722-libapp.so', fixture/'real')
    cmd = [str(exe), case, str(fixture)]
    p = subprocess.run(cmd,capture_output=True,text=True,encoding='utf8',errors='replace')
    records.append(dict(command=cmd,input=str(R)+'; actual fixture bytes and injected boundary faults',output=p.stdout+p.stderr,exit=p.returncode))
    print(p.stdout+p.stderr,end='')
    if p.returncode: failed += 1
entry = source('entry.cpp')
probe = (D/'entry-probe.cpp').read_text(encoding='utf8')
probe = probe.replace('// INSERT_PRODUCTION_STATE', function(entry,'struct State')+';')
methods = function(entry,'bool NeedAcquire(')+'\n'+function(entry,'void AcquireSites(')+'\n'+function(entry,'void HomeTweaksPrepareForLauncherChild(')
probe = probe.replace('// INSERT_PRODUCTION_METHODS',methods)
(run/'entry.cpp').write_text(probe,encoding='utf8')
entrycmd = [cmd for cmd in records[0]['command']]
entrycmd = [str(run/'entry.cpp') if v == str(run/'production.cpp') else str(run/'entry.exe') if v == str(exe) else v for v in entrycmd]
entrycmd += ['-Wno-unused-variable']
for cmd in [entrycmd,[str(run/'entry.exe')]]:
    p=subprocess.run(cmd,capture_output=True,text=True,encoding='utf8',errors='replace')
    records.append(dict(command=cmd,input=str(R)+'; production AcquireSites/State/child reset, boundary loaders',output=p.stdout+p.stderr,exit=p.returncode))
    print(p.stdout+p.stderr,end='')
    if p.returncode:failed+=1;break
print(f'SITES_CACHE_AUDIT={len(cases)} cases + entry lifecycle; failed={failed}')
(run/'commands.json').write_text(json.dumps(records,indent=2,ensure_ascii=False),encoding='utf8')
sys.exit(bool(failed))
