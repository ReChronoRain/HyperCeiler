"""Verify the production keyboard-only navigation inset filter."""
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
stubs = {
    'android/content/Context.java': 'package android.content; public class Context {public Info info=new Info();public Info getApplicationInfo(){return info;}public static class Info {public int targetSdkVersion=35;}}',
    'android/graphics/Insets.java': 'package android.graphics; public class Insets {public final int left,top,right,bottom;public Insets(int l,int t,int r,int b){left=l;top=t;right=r;bottom=b;}public static Insets of(int l,int t,int r,int b){return new Insets(l,t,r,b);}}',
    'android/view/View.java': 'package android.view;public class View {public static final int VISIBLE=0,GONE=8;public ViewGroup parent;public Object getParent(){return parent;}}',
    'android/view/ViewGroup.java': 'package android.view;public class ViewGroup extends View {public int visibility=0,children=1;public android.content.Context context=new android.content.Context();public android.content.Context getContext(){return context;}public int getVisibility(){return visibility;}public int getChildCount(){return children;}public void requestApplyInsets(){}}',
    'android/view/WindowInsets.java': '''package android.view;import android.graphics.Insets;public class WindowInsets {public Insets nav,stable;public int other=42;public WindowInsets(Insets v,Insets s){nav=v;stable=s;}public Insets getInsets(int type){return nav;}public Insets getInsetsIgnoringVisibility(int type){return stable;}public static class Type {public static int navigationBars(){return 2;}}public static class Builder {WindowInsets out;public Builder(WindowInsets in){out=new WindowInsets(in.nav,in.stable);out.other=in.other;}public Builder setInsets(int t,Insets i){out.nav=i;return this;}public Builder setInsetsIgnoringVisibility(int t,Insets i){out.stable=i;return this;}public WindowInsets build(){return out;}}}''',
    'com/sevtinge/hyperceiler/common/log/ModuleLog.java': 'package com.sevtinge.hyperceiler.common.log;public class ModuleLog {public static int logs;public static void i(String t,String m){logs++;}}',
}
test = '''import android.view.*;import android.graphics.Insets;import com.sevtinge.hyperceiler.libhook.appbase.input.MiuiImeInsets;
public class InsetsTest {
static void check(boolean v){if(!v)throw new AssertionError();}
public static void main(String[] args){
 var frame=new ViewGroup();var area=new ViewGroup();var keyboard=new View();keyboard.parent=frame;
 var original=new WindowInsets(Insets.of(7,1,9,65),Insets.of(7,1,9,80));var filter=new MiuiImeInsets();
 check(filter.forInputView(keyboard,original)==original);filter.bind(frame,area);
 var out=filter.forInputView(keyboard,original);check(out!=original);check(out.nav.bottom==0&&out.stable.bottom==0);
 check(out.nav.left==7&&out.nav.top==1&&out.nav.right==9&&out.other==42);check(original.nav.bottom==65);
 check(filter.forInputView(area,original)==original);var sibling=new View();check(filter.forInputView(sibling,original)==original);
 area.visibility=View.GONE;check(filter.forInputView(keyboard,original)==original);area.visibility=View.VISIBLE;
 area.children=0;check(filter.forInputView(keyboard,original)==original);area.children=1;
 filter.forInputView(keyboard,original);check(com.sevtinge.hyperceiler.common.log.ModuleLog.logs==1);
 var flat=new WindowInsets(Insets.of(7,0,9,0),Insets.of(7,0,9,0));check(filter.forInputView(keyboard,flat)==flat);
 filter.clear();check(filter.forInputView(keyboard,original)==original);
 var legacy=new MiuiImeInsets();frame.context.info.targetSdkVersion=34;legacy.bind(frame,area);check(legacy.forInputView(keyboard,original)==original);
 System.out.println("IME inset filter: PASS (duplicate gap, keyboard subtree, MIUI/hidden/empty areas, safe edges, SDK, repeat, original immutability)");
}}
'''
with tempfile.TemporaryDirectory(prefix='hc-ime-insets-') as temp:
    temp = Path(temp)
    sources = []
    for name, text in stubs.items():
        p = temp / name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(text)
        sources.append(p)
    harness = temp / 'InsetsTest.java'
    harness.write_text(test)
    sources += [harness, root / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/appbase/input/MiuiImeInsets.java']
    subprocess.run(['javac', '-d', str(temp)] + [str(p) for p in sources], check=True)
    subprocess.run(['java', '-ea', '-cp', str(temp), 'InsetsTest'], check=True)
