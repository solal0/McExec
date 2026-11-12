package dev.mcexec.client;

import java.lang.foreign.Arena;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/** Explicit advanced JVM tools. Does not sandbox Groovy or make operations reversible. */
public final class AdvancedAccess {
    public Field findField(Class<?> type, String name) throws ReflectiveOperationException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }
    public Object get(Object target, String name) throws ReflectiveOperationException {
        return findField(target instanceof Class<?> c ? c : target.getClass(), name)
                .get(target instanceof Class<?> ? null : target);
    }
    public void set(Object target, String name, Object value) throws ReflectiveOperationException {
        findField(target instanceof Class<?> c ? c : target.getClass(), name)
                .set(target instanceof Class<?> ? null : target, value);
    }
    /** Like set(), but record the previous field value for this script's best-effort undo. */
    public void setTracked(Object target, String name, Object value) throws ReflectiveOperationException {
        ScriptSession session = MCExecClient.runtime().requireSession();
        Field field = findField(target instanceof Class<?> c ? c : target.getClass(), name);
        Object instance = target instanceof Class<?> ? null : target;
        Object previous = field.get(instance);
        field.set(instance, value);
        session.onStop(() -> {
            try { field.set(instance, previous); }
            catch (ReflectiveOperationException failure) {
                MCExecClient.runtime().error("Could not restore reflected field: " + field, failure);
            }
        });
    }
    public MethodHandles.Lookup privateLookup(Class<?> type) throws IllegalAccessException {
        return MethodHandles.privateLookupIn(type, MethodHandles.lookup());
    }
    public List<String> fields(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).map(f -> Modifier.toString(f.getModifiers())
                + " " + f.getType().getTypeName() + " " + f.getName()).toList();
    }
    public List<String> methods(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods()).map(Method::toGenericString).toList();
    }
    /** Caller owns the arena and must close it. Native code may crash the process. */
    public Arena arena() { return Arena.ofConfined(); }
}
