package ch.eneas.flips.loader.internal;

import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

class Instantiator {
    private static final Map<Class<?>, Object> instances = new HashMap<>();

    private Instantiator() {}

    static void addInstance(Object instance) {
        instances.put(instance.getClass(), instance);
    }

    @SuppressWarnings("unchecked")
    static <T>T getInstance(Class<T> clazz, JavaPlugin srcPlugin) throws NewInstanceFailedException {
        Object instance = instances.get(clazz);
        if (instance != null) {
            return (T) instance;
        }
        instance = createInstance(clazz, srcPlugin);
        instances.put(clazz, instance);
        return (T) instance;
    }

    @SuppressWarnings("unchecked")
    static <T>T getCachedInstance(Class<T> clazz) {
        return (T) instances.get(clazz);
    }

    private static Object createInstance(Class<?> clazz, JavaPlugin srcPlugin) throws NewInstanceFailedException {
        try {
            final Bit isNestedClass = Bit.of(clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()));
            final Constructor<?> con = getConstructor(clazz, isNestedClass, srcPlugin.getClass());

            con.setAccessible(true);

            final Object[] initArgs = new Object[con.getParameterCount()];
            if (isNestedClass.asBool) {
                initArgs[0] = getInstance(clazz.getDeclaringClass(), srcPlugin);
            }
            if (con.getParameterCount() > isNestedClass.asInt) {
                initArgs[isNestedClass.asInt] = srcPlugin;
            }
            return con.newInstance(initArgs);
        } catch (InaccessibleObjectException e) {
            throw new NewInstanceFailedException("failed to access constructor of " + clazz, e);
        } catch (InvocationTargetException e) {
            throw new NewInstanceFailedException("constructor of " + clazz + " threw an exception", e.getCause());
        } catch (InstantiationException e) {
            throw new NewInstanceFailedException("failed to instantiate " + clazz, e);
        } catch (IllegalAccessException e) {
            throw new NewInstanceFailedException("constructor of " + clazz + " is inaccessible", e);
        }
    }

    private static Constructor<?> getConstructor(Class<?> clazz, Bit isNested, Class<? extends JavaPlugin> srcPluginClass) throws NewInstanceFailedException {
        final Constructor<?> con = getNoArgConstructor(clazz);
        final int conParams = con.getParameterCount() - isNested.asInt; // first parameter of non-static nested classes is always declaring class

        if (conParams > 1 || (conParams == 1 && !con.getParameterTypes()[isNested.asInt].isAssignableFrom(srcPluginClass))) {
            throw new NewInstanceFailedException(clazz + " must have an accessible no-arg constructor or constructor with your plugin as only argument", null);
        }
        return con;
    }

    private static Constructor<?> getNoArgConstructor(Class<?> clazz) {
        final Constructor<?>[] cons = clazz.getDeclaredConstructors();
        if (cons.length > 1) {
            try {
                return clazz.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new NewInstanceFailedException(clazz + " must have an accessible no-arg constructor or constructor with your plugin as only argument", e);
            }
        }
        return cons[0];
    }

    static class NewInstanceFailedException extends RuntimeException {
        public NewInstanceFailedException(String message, Throwable cause) {
            super(requireNonNull(message, "message"), cause);
        }
    }

    private static class Bit { // i don't want to convert booleans to 1 and 0 all the time
        public static final Bit TRUE = new Bit(true);
        public static final Bit FALSE = new Bit(false);

        public static Bit of(boolean value) {
            return value ? TRUE : FALSE;
        }

        public final boolean asBool;
        public final byte asInt;

        private Bit(boolean value) {
            this.asBool = value;
            this.asInt = (byte) (value ? 1 : 0);
        }
    }
}
