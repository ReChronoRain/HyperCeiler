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
        p=temp/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);sources.append(p)
    sources += [root/'app/src/main/java/com/sevtinge/hyperceiler/log/LogSyncDiagnostics.java',Path(__file__).with_name('LogSyncDiagnosticsTest.java')]
    subprocess.run(['javac','-encoding','UTF-8','-d',str(temp/'classes')]+[str(p) for p in sources],check=True)
    subprocess.run(['java','-ea','-cp',str(temp/'classes'),'com.sevtinge.hyperceiler.log.LogSyncDiagnosticsTest'],check=True)
    hooks=(root/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
    begin=hooks.index('std::array<std::atomic_uint32_t,6> g_scale_bind_failure')
    end=hooks.index('bool bind_icon_render()',begin)
    (temp/'binding-diagnostic.h').write_text(hooks[begin:end])
    for name in ['bind_folder_animation_scale','bind_widget_scale','bind_folder_icon_scale']:
        begin=hooks.index('bool '+name+'()');end=hooks.index('\nbool ',begin+6)
        body=hooks[begin:end]
        assert 'return false;' not in body,name+' has an unreported failure path'
        assert 'scale_bind_failure' in body,name
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-I'+str(root/'app/src/main/cpp'),'-I'+str(temp),str(Path(__file__).with_name('native.cpp')),'-o',str(temp/'native')],check=True)
    subprocess.run([str(temp/'native')],check=True)
    java=(root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java').read_text()
    header=(root/'app/src/main/cpp/targets/home/home_endpoint_diagnostics.h').read_text()
    for j,c in [('ENDPOINT_DIAGNOSTICS_MAGIC','kEndpointDiagnosticsMagic'),('ENDPOINT_DIAGNOSTICS_SCHEMA','kEndpointDiagnosticsSchema'),('ENDPOINT_REVISION','kEndpointRevision'),('ENDPOINT_CAPABILITIES','kIndependentScalesCapability')]:
        a=int(re.search(j+r'\s*=\s*(0x[0-9a-fA-F]+|\d+)',java)[1],0)
        b=int(re.search(c+r'\s*=\s*(0x[0-9a-fA-F]+|\d+)',header)[1],0)
        assert a==b,(j,c)
print('endpoint writer/reader revision and capability contract: PASS')

# Execute the exact production shell-copy expression against disposable directories.
# Both cp and chmod failures must produce an explicit diagnostic verdict; empty is valid.
with tempfile.TemporaryDirectory(prefix='hc-lspd-copy-') as temp:
    temp=Path(temp)
    loader=(root/'app/src/main/java/com/sevtinge/hyperceiler/log/XposedLogLoader.java').read_text()
    expression=re.search(r'final String captured = ShellUtils.rootExecCmd\((.*?)\)\.trim\(\);',loader,re.S)[1]
    source=temp/'CopyCommand.java'
    source.write_text('import java.io.File; public class CopyCommand {public static void main(String[] args){String sourceDir=args[0];File targetDir=new File(args[1]);System.out.print('+expression+');}}')
    subprocess.run(['javac','-d',str(temp),str(source)],check=True)
    before=temp/'source logs';after=temp/'destination logs';before.mkdir();after.mkdir()
    command=subprocess.check_output(['java','-cp',str(temp),'CopyCommand',str(before),str(after)],text=True)
    def shell(prefix=''):
        return subprocess.check_output(['/bin/sh','-c',prefix+command],text=True).strip()
    assert shell()=='HC_LSPD_EMPTY'
    (before/'modules fixture.log').write_text('fixture\n')
    assert shell()=='HC_LSPD_COPY_OK'
    assert (after/'modules fixture.log').read_text()=='fixture\n'
    assert shell('cp() { echo copy-denied >&2; return 13; }; ').endswith('HC_LSPD_COPY_FAILED')
    assert shell('chmod() { echo chmod-denied >&2; return 13; }; ').endswith('HC_LSPD_COPY_FAILED')
    assert 'if (copyLspdLogs()) persistFilteredLogsToDatabase();' in loader
print('LSPosed capture verdicts: PASS (empty/success/copy denied/chmod denied; failed captures retain previous DB logs)')
