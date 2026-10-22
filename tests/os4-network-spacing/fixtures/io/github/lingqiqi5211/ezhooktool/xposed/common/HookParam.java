package io.github.lingqiqi5211.ezhooktool.xposed.common;
public class HookParam {
    private final Object target;
    private final Object[] args;
    public boolean failed;
    public boolean skipped;
    public HookParam(Object target, Object... args) { this.target = target; this.args = args; }
    public Object getThisObject() { return target; }
    public Object[] getArgs() { return args; }
    public boolean getHasThrowable() { return failed; }
    public boolean isSkipped() { return skipped; }
}
