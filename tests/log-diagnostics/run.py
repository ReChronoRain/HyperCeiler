#!/usr/bin/env python3
"""Production failure reports stay visible with ordinary logging disabled; old endpoints remain usable."""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory(prefix='hc-log-diagnostics-') as temp:
    temp=Path(temp)
    stubs={
        'android/os/Build.java':'''package android.os; public class Build {public static final String MANUFACTURER="fixture",MODEL="device",DISPLAY="build"; public static class VERSION {public static final int SDK_INT=37;}}''',
        'android/content/Context.java':'''package android.content; public class Context {public String getPackageName(){return "com.sevtinge.hyperceiler";} public android.content.pm.PackageManager getPackageManager(){return new android.content.pm.PackageManager();}}''',
        'android/content/pm/PackageManager.java':'''package android.content.pm; public class PackageManager {public PackageInfo getPackageInfo(String name,int flags){return new PackageInfo();}}''',
        'android/content/pm/PackageInfo.java':'''package android.content.pm; public class PackageInfo {public String versionName="fixture";public long lastUpdateTime=123;public long getLongVersionCode(){return 4779;}}''',
        'com/sevtinge/hyperceiler/common/utils/ShellUtils.java':'''package com.sevtinge.hyperceiler.common.utils;public class ShellUtils {public static int result=1;public static boolean fail; public static int checkRootPermission(){if(fail)throw new IllegalStateException("su unavailable");return result;}}''',
        'com/sevtinge/hyperceiler/common/log/ModuleLog.java':'''package com.sevtinge.hyperceiler.common.log; public class ModuleLog {public static int failures;public static void e(String tag,String message,Throwable error){failures++;}}''',
        'com/sevtinge/hyperceiler/log/db/LogDao.java':'''package com.sevtinge.hyperceiler.log.db;public interface LogDao {LogEntry latestSyncDiagnostic(String tag);void insert(LogEntry entry);}''',
        'com/sevtinge/hyperceiler/log/db/LogEntry.java':'''package com.sevtinge.hyperceiler.log.db;public class LogEntry {public final String module,level,tag,message,source;public LogEntry(String module,String level,String tag,String message,long time,String source,String pid){this.module=module;this.level=level;this.tag=tag;this.message=message;this.source=source;}public String getLevel(){return level;}public String getMessage(){return message;}}'''
    }
    sources=[]
    for name,text in stubs.items():
        p=temp/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8');sources.append(p)
    sources += [root/'app/src/main/java/com/sevtinge/hyperceiler/log/LogSyncDiagnostics.java',Path(__file__).with_name('LogSyncDiagnosticsTest.java')]
    subprocess.run(['javac','-encoding','UTF-8','-d',str(temp/'classes')]+[str(p) for p in sources],check=True)
    subprocess.run(['java','-ea','-cp',str(temp/'classes'),'com.sevtinge.hyperceiler.log.LogSyncDiagnosticsTest'],check=True)
    hooks=(root/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text(encoding='utf-8')
    begin=re.search(r'std::array<std::atomic_uint32_t,\d+> g_scale_bind_failure',hooks).start()
    end=hooks.index('bool bind_icon_render()',begin)
    (temp/'binding-diagnostic.h').write_text(hooks[begin:end],encoding='utf-8')
    for name in ['bind_folder_animation_scale','bind_widget_scale','bind_folder_icon_scale']:
        begin=hooks.index('bool '+name+'()');end=hooks.index('\nbool ',begin+6)
        body=hooks[begin:end]
        assert 'return false;' not in body,name+' has an unreported failure path'
        assert 'scale_bind_failure' in body,name
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-I'+str(root/'app/src/main/cpp'),'-I'+str(temp),str(Path(__file__).with_name('native.cpp')),'-o',str(temp/'native')],check=True)
    subprocess.run([str(temp/'native')],check=True)
    java=(root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java').read_text(encoding='utf-8')
    header=(root/'app/src/main/cpp/targets/home/home_endpoint_diagnostics.h').read_text(encoding='utf-8')
    for j,c in [('ENDPOINT_DIAGNOSTICS_MAGIC','kEndpointDiagnosticsMagic'),('ENDPOINT_DIAGNOSTICS_SCHEMA','kEndpointDiagnosticsSchema'),('ENDPOINT_REVISION','kEndpointRevision')]:
        a=int(re.search(j+r'\s*=\s*(0x[0-9a-fA-F]+|\d+)',java)[1],0)
        b=int(re.search(c+r'\s*=\s*(0x[0-9a-fA-F]+|\d+)',header)[1],0)
        assert a==b,(j,c)
    capabilities=int(re.search(r'ENDPOINT_CAPABILITIES\s*=\s*(\d+)',java)[1])
    expected=0
    for key in ('kIndependentScalesCapability','kDockTitleCapability','kRecentLayoutCapability','kDrawerSearchCapability','kLayoutPoliciesCapability','kDockHorizontalCapability','kInfiniteScrollCapability'):
        expected |= int(re.search(key+r'\s*=\s*(\d+)',header)[1])
    assert capabilities==expected
print('endpoint writer/reader revision and capability contract: PASS')

# Execute the production LSPosed copy script against disposable directories.
# Copy/chmod failures must be explicit and must retain the previous target contents.
with tempfile.TemporaryDirectory(prefix='hc-lspd-copy-') as temp:
    temp=Path(temp)
    loader=(root/'app/src/main/java/com/sevtinge/hyperceiler/log/XposedLogLoader.java').read_text(encoding='utf-8')
    constants='\n'.join(re.findall(r'    private static final int LSPD_[A-Z_]+ = \d+;',loader))
    script_method=re.search(r'    static String buildLspdCopyScript\(.*?\n    \}',loader,re.S)[0]
    quote_method=re.search(r'    private static String shellQuote\(.*?\n    \}',loader,re.S)[0]
    source=temp/'CopyCommand.java'
    source.write_text('public class CopyCommand {\n'+constants+'\n'+script_method+'\n'+quote_method+
        '\npublic static void main(String[] args){System.out.print(buildLspdCopyScript(args[0],args[1]));}}',encoding='utf-8')
    subprocess.run(['javac','-d',str(temp),str(source)],check=True)
    before=temp/"source ' logs";after=temp/'destination logs';before.mkdir();after.mkdir()
    def run_copy(prefix=''):
        command=subprocess.check_output(['java','-cp',str(temp),'CopyCommand',str(before),str(after)],text=True)
        result=subprocess.run(['/bin/sh','-c',prefix+command],text=True,capture_output=True)
        lines=result.stdout.strip().splitlines()
        return result.returncode,lines[-1] if lines else ''
    def reset_target():
        for child in after.iterdir():
            child.unlink()
        (after/'previous.log').write_text('previous\n',encoding='utf-8')
    reset_target()
    code,verdict=run_copy()
    assert (code,verdict)==(21,'HC_LSPD_EMPTY')
    assert (after/'previous.log').read_text(encoding='utf-8')=='previous\n'
    assert 'clearDirectory(targetDir);' in loader
    (before/'modules_fixture name.log').write_text('modules\n',encoding='utf-8')
    (before/'verbose_fixture name.log').write_text('verbose\n',encoding='utf-8')
    (before/'modules.log').write_text('legacy modules\n',encoding='utf-8')
    (before/'verbose.log').write_text('legacy verbose\n',encoding='utf-8')
    (before/'unrelated.txt').write_text('ignored\n',encoding='utf-8')
    (before/'nested').mkdir()
    (before/'nested'/'modules_nested.log').write_text('ignored\n',encoding='utf-8')
    code,verdict=run_copy()
    assert (code,verdict)==(0,'HC_LSPD_COPY_OK')
    assert sorted(p.name for p in after.iterdir())==[
        'modules.log','modules_fixture name.log','verbose.log','verbose_fixture name.log'
    ]
    reset_target()
    code,verdict=run_copy('cp() { echo copy-denied >&2; return 13; }; ')
    assert (code,verdict)==(24,'HC_LSPD_COPY_FAILED')
    assert (after/'previous.log').read_text(encoding='utf-8')=='previous\n'
    assert not list(temp.glob('destination logs.hc-*'))
    code,verdict=run_copy('chmod() { echo chmod-denied >&2; return 13; }; ')
    assert (code,verdict)==(25,'HC_LSPD_CHMOD_FAILED')
    assert (after/'previous.log').read_text(encoding='utf-8')=='previous\n'
    assert not list(temp.glob('destination logs.hc-*'))
    before=temp/'missing source'
    code,verdict=run_copy()
    assert (code,verdict)==(20,'HC_LSPD_SOURCE_UNAVAILABLE')
    assert (after/'previous.log').read_text(encoding='utf-8')=='previous\n'
    assert 'ShellUtils.execRootCommand(command)' in loader
    assert 'if (copyLspdLogs()) persistFilteredLogsToDatabase();' in loader
    for access in ('DIRECT','PROC_ROOT','NSENTER_MOUNT','NSENTER_TARGET'):
        assert access in loader,access
    health=(root/'library/common/src/main/java/com/sevtinge/hyperceiler/common/log/LoggerHealthChecker.kt').read_text(encoding='utf-8')
    assert 'PAGE_MARKER_REGEX.matches(firstNonBlankLine)' in health
    assert 'LSPD_ENTRY_REGEX.containsMatchIn(firstNonBlankLine)' in health
print('LSPosed capture compatibility: PASS (direct/proc-root/nsenter strategies; empty/success/copy denied/chmod denied/source unavailable; failed staging retains target)')
