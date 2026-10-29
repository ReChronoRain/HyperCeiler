#!/usr/bin/env python3
"""Verify native slot metadata against the shipped consumer and comma-only presentation."""
from pathlib import Path
import tempfile,subprocess,json
root=Path(__file__).resolve().parents[2];s=(root/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
a=s.index('constexpr size_t kConfigCaptureSlot =');b=s.index('\n',s.index('constexpr size_t kSlotCount ='))
constants=s[a:b]
a=s.index('const char* diagnostic_feature(size_t slot) {');b=s.index('\n}\n',a)+3
body=s[a:b]
prefix='''#include <atomic>
#include <memory>
#include <string>
#include <vector>
#include <iostream>
#include <cassert>
#define HC_LAYOUT_KNOB_COUNT 8
namespace home_layout::features::recent { enum class Action {Scalar,FrameScalar,PoolReturn,ScaleReturn,Style,FrameStyle,Freeform,ScalarReturn,FreeformColor};struct Site {Action action{};int setting=0;};struct Plan {std::vector<Site> sites;};}
struct RecentRouting {home_layout::features::recent::Plan plan;};std::shared_ptr<const RecentRouting> g_recent_routing;
'''
test='''
int main(){
 for(size_t i=0;i<kSlotCount;++i)std::cout<<i<<" "<<diagnostic_feature(i)<<"\\n";
 auto r=std::make_shared<RecentRouting>();
 for(int i=0;i<15;++i)r->plan.sites.push_back({home_layout::features::recent::Action::Scalar,i});
 g_recent_routing=r;
 assert(std::string(diagnostic_feature(kRecentSlotBase))=="最近任务卡片圆角大小");
 assert(std::string(diagnostic_feature(kRecentSlotBase+1))=="最近任务卡片标题高度");
 assert(std::string(diagnostic_feature(kRecentSlotBase+10))=="最近任务小窗按钮背景颜色");
 assert(std::string(diagnostic_feature(kRecentSlotBase+11))=="最近任务隐藏图标");
 assert(std::string(diagnostic_feature(kRecentSlotBase+12))=="最近任务隐藏文本");
 assert(std::string(diagnostic_feature(kRecentSlotBase+13))=="最近任务划卡移除动画");
 assert(std::string(diagnostic_feature(kRecentSlotBase+14))=="最近任务上滑关闭卡片振动");
 r->plan.sites[0].action=home_layout::features::recent::Action::Style;
 assert(std::string(diagnostic_feature(kRecentSlotBase))=="最近任务应用名称显示与文字样式");
 r->plan.sites[0].action=home_layout::features::recent::Action::ScaleReturn;
 assert(std::string(diagnostic_feature(kRecentSlotBase))=="最近任务横向与纵向卡片缩放");
}
'''
with tempfile.TemporaryDirectory()as tmp:
 p=Path(tmp);(p/'names.cpp').write_text(prefix+constants+'\n'+body+test)
 subprocess.run(['c++','-std=c++20','-O2',str(p/'names.cpp'),'-o',str(p/'names')],check=True)
 raw=subprocess.check_output([str(p/'names')],text=True)
 names=[l.split(' ',1)[1]for l in raw.splitlines()]
 assert not any(n in ['桌面布局','最近任务','用途未知的原生挂钩']for n in names)
 (p/'native-names.txt').write_text('\n'.join(names))
 sources=list((root/'library/common/src/main/java/com/sevtinge/hyperceiler/common/diagnostics').glob('*.java'))
 sources.append(Path(__file__).with_name('PresentationTest.java'))
 subprocess.run(['javac','-encoding','UTF-8','-d',tmp,*map(str,sources)],check=True)
 subprocess.run(['java','-ea','-cp',tmp,'PresentationTest',str(p/'native-names.txt')],check=True)
 print('Native functional labels',len(names),'slots; runtime recent settings and shared actions verified')

import xml.etree.ElementTree as ET,unicodedata
ns='{http://schemas.android.com/apk/res/android}'
page=ET.parse(root/'app/src/main/res/xml/prefs_hook_diagnostics.xml').getroot()
first=list(list(page)[0])[0]
assert first.tag.endswith('DiagnosticsOverviewPreference') and first.get(ns+'key')=='diagnostics_summary'
for file in ['home.xml','home_new.xml']:
 entry=[n for n in ET.parse(root/'library/core/src/main/res/xml'/file).iter()if n.get(ns+'key')=='prefs_key_home_diagnostics']
 assert len(entry)==1 and entry[0].get(ns+'persistent')=='false'
for directory in ['values','values-zh-rCN']:
 for n in ET.parse(root/'app/src/main/res'/directory/'diagnostics.xml').getroot():
  text=''.join(n.itertext())
  assert all(ch in ',，' or not unicodedata.category(ch).startswith(('P','S'))for ch in text),(n.get('name'),text)
print('Diagnostics UI contract PASS: overview first, both launcher entries, comma-only resource copy')

fragment=(root/'app/src/main/java/com/sevtinge/hyperceiler/diagnostics/HookDiagnosticsFragment.java').read_text()
assert 'assessment.counts()' in fragment and 'counts.success()' in fragment
assert all(old not in fragment for old in ['最终效果','f.effect()','f.installation()','f.execution()','hook_diagnostics_levels'])
assert not any(n.get('name','').startswith('hook_diagnostics_levels') for n in ET.parse(root/'app/src/main/res/values/diagnostics.xml').getroot())
print('Success presentation PASS: shared counts, execution counts as success, layered prompts removed')

# Each overview metric has a distinct section, current data is grouped, history is optional.
assert 'case 2->"pending";default->"unknown";' in fragment
assert 'DiagnosticSummary.from(report)' in fragment and 'assessment.features()' in fragment
assert 'for(var f:report.findings()' not in fragment
assert 'detail(DiagnosticSummary.Feature' in fragment and 'private void logs(' in fragment
assert 'private void history()' in fragment and 'history.setVisible(!assessment.history().isEmpty())' in fragment
assert '.limit(200)' not in fragment
assert '当前情况' in fragment and '接下来怎么做' in fragment
print('Feature projection UI PASS: distinct metric destinations, one semantic row per feature, separate history and technical logs')
