package com.sevtinge.hyperceiler.libhook.base;

public abstract class BaseHook {
    public abstract void init();
    protected Class<?> findClassIfExists(String name) {
        try { return Class.forName(name); }
        catch (ClassNotFoundException e) { throw new AssertionError(e); }
    }
    public static Object getStaticObjectField(Class<?> owner, String field) {
        try { return owner.getField(field).get(null); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
}
