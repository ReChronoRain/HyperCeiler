package com.sevtinge.hyperceiler.libhook.base;

public abstract class BaseHook {
    public abstract void init();
    protected Class<?> findClassIfExists(String name) {
        if ("com.android.systemui.statusbar.phone.MiuiIconManagerUtils".equals(name))
            return com.android.systemui.statusbar.phone.MiuiIconManagerUtils.class;
        throw new AssertionError("Unknown fixture class: " + name);
    }
    public static Object getStaticObjectField(Class<?> owner, String field) {
        try { return owner.getField(field).get(null); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
}
