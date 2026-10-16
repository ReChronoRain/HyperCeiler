package io.github.lingqiqi5211.ezhooktool.xposed.common;

public class HookParam {
    private final Object thisObject;
    private final Object[] args;

    public HookParam(Object thisObject, Object[] args) {
        this.thisObject = thisObject;
        this.args = args;
    }

    public Object getThisObject() {
        return thisObject;
    }

    public Object[] getArgs() {
        return args;
    }
}
