package io.github.lingqiqi5211.ezhooktool.core.java;

import java.lang.reflect.Method;

public final class Methods {
    private Methods() {
    }

    public static Object callMethod(Object target, String methodName, Object... args) {
        try {
            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                types[i] = args[i] == null ? Object.class : args[i].getClass();
            }
            Method method = target.getClass().getMethod(methodName, types);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
