from pathlib import Path
import subprocess, time, json, sys
O=Path(__file__).resolve().parent
A=['adb','-s','192.168.31.52:33509']
URI='content://com.sevtinge.hyperceiler.provider.sharedprefs'
def c(*args):
    p=subprocess.run(A+list(args),capture_output=True,timeout=30)
    with (O/'device-commands.jsonl').open('a',encoding='utf-8') as f:
        f.write(json.dumps(dict(command=A+list(args),exit=p.returncode,output=p.stdout.decode('utf-8','replace'),error=p.stderr.decode('utf-8','replace')),ensure_ascii=False)+'\n')
    assert p.returncode==0,p.stderr.decode(errors='replace')
    return p.stdout
def put(spec):
    assert b'ok=true' in c('shell','content','call','--uri',URI,'--method','hc_debug_put','--arg',spec)
def margin(top,bottom):
    for k,v in [('top',top),('bottom',bottom)]:
        put(f'integer:home_layout_workspace_padding_{k}:{v}')
def state():
    s=c('shell','dumpsys','window','policy').decode()
    assert 'showing=false' in s
    assert 'com.miui.home/com.miui.home.launcher.Launcher' in c('shell','dumpsys','window').decode()
def restart():
    c('shell','am','force-stop','com.miui.home'); c('shell','input','keyevent','3');time.sleep(5);state()
def record(name,outside=False):
    state();pid=c('shell','pidof','com.miui.home')
    remote='/sdcard/Download/hc-folder-landing-'+name+'.mp4'
    p=subprocess.Popen(A+['shell','screenrecord','--time-limit','6',remote],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    time.sleep(1);c('shell','input','tap','735','535');time.sleep(1.1)
    if outside:c('shell','input','tap','1120','2300')
    else:c('shell','input','keyevent','4')
    out,err=p.communicate(timeout=15);assert p.returncode==0,(out,err)
    c('pull',remote,str(O/(name+'.mp4')));c('shell','rm','-f',remote)
    assert c('shell','pidof','com.miui.home')==pid
    (O/(name+'-native.log')).write_bytes(c('logcat','-d','-s','HyperCeiler.HomeLayout'))
    (O/(name+'-final.png')).write_bytes(c('exec-out','screencap','-p'))
    print('RECORDED',name,'PID_STABLE='+pid.decode().strip(),'EXIT=0',flush=True)
phase=sys.argv[1]
c('shell','setprop','debug.hyperceiler.prefs_write','1')
if phase=='modified':
    for k in ['top','bottom']:put(f'boolean:home_layout_workspace_padding_{k}_enable:true')
    margin(0,240);time.sleep(3);restart()
    (O/'modified-before-live.png').write_bytes(c('exec-out','screencap','-p'))
    margin(110,164);time.sleep(4)
    (O/'modified-after-live.png').write_bytes(c('exec-out','screencap','-p'))
    record('modified-live')
    record('modified-outside',True)
elif phase=='restore':
    for k,e in json.loads((O/'test-settings-original.json').read_text()).items():
        t='integer' if e['type']=='int' else e['type']
        put(f"{t}:{k.removeprefix('prefs_key_')}:{e['value']}")
    c('shell','setprop','debug.hyperceiler.prefs_write','0');restart()
    print('RESTORED original four top/bottom settings; EXIT=0')
else:raise SystemExit('phase')
