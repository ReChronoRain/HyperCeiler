import com.sevtinge.hyperceiler.common.diagnostics.*;
import java.nio.file.*;
import java.util.*;

public final class PresentationTest {
    private static DiagnosticEvent event(String id,String feature){return new DiagnosticEvent(id,feature,"NATIVE_INLINE","com.miui.home","com.miui.home",123,"boot","500","v1","elf:build","SDK37",1000,"INSTALLED","INSTALL","INSTALL_OK","original.evidence: untouched; /path","logcat",1);}
    public static void main(String[] args)throws Exception{
        List<String> expected=Files.readAllLines(Path.of(args[0]));
        for(int i=0;i<expected.size();i++){
            var e=event("home.layout.slot."+i,"桌面布局");
            String name=DiagnosticFeatureNames.forEvent(e);
            // HCD1's frozen legacy registry predates the appended shortcut/haptic slots.
            // Old records stay unknown there, while current records carry exact native metadata.
            if(i<236)assert name.equals(expected.get(i)):i+" "+name+" != "+expected.get(i);
            else assert name.equals("用途未知的历史原生挂钩"):i+" "+name;
            assert DiagnosticFeatureNames.forEvent(event("home.layout.slot."+i,expected.get(i)))
                .equals(expected.get(i)):i+" current native metadata lost";
        }
        assert DiagnosticFeatureNames.forEvent(event("home.layout.slot.99999","桌面布局")).contains("未知");
        assert DiagnosticFeatureNames.forEvent(event("home.layout.slot.-1","桌面布局")).contains("未知");
        var precise=event("home.layout.slot.200","最近任务卡片圆角大小");
        assert DiagnosticFeatureNames.forEvent(precise).equals("最近任务卡片圆角大小");
        String javaOwner="com.sevtinge.hyperceiler.libhook.rules.home.recent.CardTextSize/Hook(android.Context)";
        assert DiagnosticFeatureNames.forEvent(event(javaOwner,javaOwner)).equals("最近任务卡片文字大小");
        assert LegacyDiagnosticAdapter.label("drop_geometry").equals("图标拖放落点与桌面边距");
        var legacy=event("home.layout.slot.4","桌面布局");String wire=legacy.wire();
        var report=DiagnosticRules.analyze(List.of(legacy),new DiagnosticRules.Environment("boot",Map.of(123,"500"),true,2000,true,Set.of("elf:build")),0,0,"");
        var f=report.findings().get(0);
        assert f.feature().equals("Dock 底部边距")&&f.state()==DiagnosticRules.State.INSTALLED&&DiagnosticRules.stateLabel(f.state()).equals("待执行");
        assert f.events().get(0).wire().equals(wire)&&legacy.feature().equals("桌面布局");
        assert DiagnosticRules.reportText(report).contains("Dock 底部边距");
        String source="安装：已安装；执行/效果。参数（未知）…规则_ID [x] "+'\n'+"下一行，保留,末尾!";
        String human=DiagnosticPresentation.prose(source);
        for(int p:human.codePoints().toArray()){
            int kind=Character.getType(p);
            boolean punctuation=(kind>=20&&kind<=30);
            assert !punctuation||p=='，':human;
        }
        assert human.contains("下一行")&&human.contains("\n")&&!human.endsWith("，");
        assert DiagnosticPresentation.prose(human).equals(human);
        System.out.println("Diagnostic presentation PASS: all legacy native slot identities, exact recent/Java labels, unknown fallback, report names, unchanged state/raw evidence, comma-only prose");
    }
}
