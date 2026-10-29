/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.recent;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Runtime AIDL/group/TaskInfo contracts, no vendor implementation class or task mutation. */
public final class HomeRecentCardsFilter {
    private HomeRecentCardsFilter() {}
    public static Set<String> packages(Set<String> source) {
        if (source.size() > 1024) throw new IllegalArgumentException("too many packages");
        for (String name : source) if (name == null || name.length() > 255
            || !name.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*"))
            throw new IllegalArgumentException("invalid package");
        return Set.copyOf(source);
    }
    public static Method query(Class<?> contract) throws ReflectiveOperationException {
        Method found = null;
        for (Method m : contract.getMethods()) if (m.getName().equals("getRecentTasks")
            && java.util.Arrays.equals(m.getParameterTypes(), new Class<?>[]{int.class,int.class,int.class})
            && m.getReturnType().isArray() && !m.getReturnType().getComponentType().isPrimitive()) {
            if (found != null && !found.equals(m)) throw new NoSuchMethodException("ambiguous recent-task AIDL");
            found = m;
        }
        if (found == null) throw new NoSuchMethodException("recent-task AIDL");
        return found;
    }
    public static final class Plan {
        private final Class<?> array, task;
        private final Method list, first, second, componentPackage, intentComponent, intentPackage;
        private final Field realActivity, baseIntent;
        public Plan(Class<?> array, Class<?> task) throws ReflectiveOperationException {
            if (!array.isArray() || array.getComponentType().isPrimitive()) throw new NoSuchMethodException("task group array");
            this.array = array; this.task = task;
            Class<?> group = array.getComponentType();
            Method all = optional(group, "getTaskInfoList");
            if (all != null && !List.class.isAssignableFrom(all.getReturnType())) throw new NoSuchMethodException("task group list type");
            list = all;
            first = all == null ? group.getMethod("getTaskInfo1") : null;
            second = all == null ? optional(group, "getTaskInfo2") : null;
            for (Method m : new Method[]{first,second}) if (m != null && !task.isAssignableFrom(m.getReturnType()))
                throw new NoSuchMethodException("task getter type");
            for (Method m : new Method[]{list,first,second}) if (m != null && Modifier.isStatic(m.getModifiers()))
                throw new NoSuchMethodException("static task getter");
            realActivity = task.getField("realActivity"); baseIntent = task.getField("baseIntent");
            componentPackage = realActivity.getType().getMethod("getPackageName");
            intentComponent = baseIntent.getType().getMethod("getComponent");
            intentPackage = baseIntent.getType().getMethod("getPackage");
            if (componentPackage.getReturnType() != String.class || intentPackage.getReturnType() != String.class
                || intentComponent.getReturnType() != realActivity.getType()) throw new NoSuchMethodException("task component contract");
        }
        private static Method optional(Class<?> c, String name) {
            try { return c.getMethod(name); } catch (NoSuchMethodException absent) { return null; }
        }
        private boolean hiddenTask(Object value, Set<String> packages) throws ReflectiveOperationException {
            if (value == null) return false;
            if (!task.isInstance(value)) throw new IllegalArgumentException("foreign task type");
            Object component = realActivity.get(value);
            Object intent = null;
            if (component == null) {
                intent = baseIntent.get(value);
                if (intent != null) component = intentComponent.invoke(intent);
            }
            Object name = component != null ? componentPackage.invoke(component)
                : intent == null ? null : intentPackage.invoke(intent);
            return name instanceof String && packages.contains(name);
        }
        private boolean hiddenGroup(Object group, Set<String> packages) throws ReflectiveOperationException {
            if (group == null) return false;
            if (list != null) {
                Object value = list.invoke(group);
                if (!(value instanceof List<?> members) || members.size() > 128)
                    throw new IllegalArgumentException("invalid task members");
                boolean hidden = false;
                for (Object member : members) hidden |= hiddenTask(member, packages);
                return hidden;
            }
            boolean hidden = hiddenTask(first.invoke(group), packages);
            if (second != null) hidden |= hiddenTask(second.invoke(group), packages);
            return hidden;
        }
        /** Split/freeform groups are indivisible cards, preserve every retained object and order. */
        public Object filter(Object original, Set<String> packages) throws ReflectiveOperationException {
            if (original == null || packages.isEmpty()) return original;
            if (!array.isInstance(original)) throw new IllegalArgumentException("foreign result array");
            int size = Array.getLength(original);
            if (size > 4096) throw new IllegalArgumentException("task list budget");
            List<Object> keep = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                Object group = Array.get(original, i);
                if (!hiddenGroup(group, packages)) keep.add(group);
            }
            if (keep.size() == size) return original;
            Object result = Array.newInstance(array.getComponentType(), keep.size());
            for (int i = 0; i < keep.size(); ++i) Array.set(result, i, keep.get(i));
            return result;
        }
    }
}
