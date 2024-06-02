package com.sevtinge.hyperceiler.module.base;

import com.hchen.hooktool.HCHook;
import com.hchen.hooktool.tool.ClassTool;
import com.hchen.hooktool.tool.DexkitTool;
import com.hchen.hooktool.tool.ExpandTool;
import com.hchen.hooktool.tool.FieldTool;
import com.hchen.hooktool.tool.MethodTool;

public abstract class BaseTool extends BaseHook {
    public static HCHook hcHook;
    public static ClassTool classTool;
    public static MethodTool methodTool;
    public static FieldTool fieldTool;
    public static DexkitTool dexkitTool;
    public static ExpandTool expandTool;

    public abstract void doHook();

    @Override
    public void init() {
        BaseTool.hcHook = new HCHook();
        BaseTool.classTool = hcHook.classTool();
        BaseTool.methodTool = hcHook.methodTool();
        BaseTool.fieldTool = hcHook.fieldTool();
        BaseTool.dexkitTool = hcHook.dexkitTool();
        BaseTool.expandTool = hcHook.expandTool();
        BaseTool.hcHook.setThisTag(TAG);
        doHook();
    }
}
