package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.Inject;
import ch.eneas.flips.loader.api.Injectable;
import ch.eneas.flips.loader.api.InjectionException;
import ch.eneas.flips.loader.api.Instance;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.eneas.flips.loader.internal.LogUtil.lazyToString;
import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
class Instantiator {
    private final Map<Class<?>, Object> instances = new HashMap<>();
    private final Map<JavaPlugin, Logger> loggers = new HashMap<>();
    private final ClassCache classCache;
    private final InternalLoaderConfig config;

    @SuppressWarnings("unchecked")
    <T> T getInstance(Class<T> clazz, JavaPlugin srcPlugin) throws NewInstanceFailedException {
        return (T) obtainInstance(clazz, srcPlugin, new LinkedHashSet<>());
    }

    private Object obtainInstance(Class<?> clazz, JavaPlugin srcPlugin, SequencedSet<Class<?>> injectionCycle) {
        if (JavaPlugin.class.isAssignableFrom(clazz) && !instances.containsKey(clazz)) {
            throw new NewInstanceFailedException("Cannot create instance of another plugin!", null);
        }
        Object instance = instances.get(clazz);
        if (instance != null) {
            debugLog(srcPlugin, "Returning cached instance for {}", clazz);
            return instance;
        }
        if (injectionCycle.contains(clazz)) {
            throw new InjectionException("cyclic dependency injecting " + clazz + " for " + injectionCycle.getLast());
        }
        injectionCycle.add(clazz);
        instance = createNewInstance(clazz, srcPlugin, injectionCycle);
        injectFields(instance, srcPlugin, injectionCycle);
        instances.put(clazz, instance);
        return instance;
    }

    private Object createNewInstance(Class<?> clazz, JavaPlugin srcPlugin, SequencedSet<Class<?>> injectionCycle) throws NewInstanceFailedException {
        try {
            final Bit isNestedClass = Bit.of(clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()));

            if (isNestedClass.asBool && (clazz.getDeclaringClass().isInstance(srcPlugin) || clazz.getDeclaringClass().isAnnotationPresent(Injectable.class))) {
                throw new InjectionException("Declaring class of nested @Injectable " + clazz + " must also be @Injectable or be the source plugin.");
            }

            final Constructor<?> con = getInjectableConstructor(clazz, isNestedClass);
            debugLog(srcPlugin, "Using constructor {}({}) for new instance of {}",
                clazz.getName(),
                lazyToString(() -> Arrays.stream(con.getParameterTypes()).map(String::valueOf).collect(Collectors.joining(", "))),
                clazz);
            con.setAccessible(true);

            final Object[] initArgs = new Object[con.getParameterCount()];
            Class<?>[] paramTypes = con.getParameterTypes();
            for (int i = 0; i < con.getParameterCount(); i++) {
                if (paramTypes[i] == Instance.class) {
                    Class<?> resolvedType;
                    try {
                        resolvedType = resolveInstanceType(con.getGenericParameterTypes()[i]);
                    } catch (InjectionException e) {
                        throw new InjectionException(e.getMessage() + " => " + formatConParams(con, isNestedClass, paramTypes, i));
                    }
                    initArgs[i] = new DynamicInstance<>(this, resolvedType, srcPlugin);
                } else {
                    Class<?> target;
                    try {
                        target = getInjectableCandidate(paramTypes[i], classCache);
                    } catch (InjectionException e) {
                        throw new InjectionException(e.getMessage() + " => " + formatConParams(con, isNestedClass, paramTypes, i));
                    }
                    initArgs[i] = obtainInstance(target, srcPlugin, injectionCycle);
                }
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

    private Class<?> resolveInstanceType(Type genericInstanceType) {
        if (!(genericInstanceType instanceof ParameterizedType pt)) {
            throw new InjectionException("No type for instance provided! Must be used as Instance<Type>!");
        }
        if (pt.getActualTypeArguments()[0] instanceof Class<?> c) {
            return c;
        } else if (pt.getActualTypeArguments()[0] instanceof ParameterizedType p) {
            return (Class<?>) p.getRawType();
        }
        throw new InjectionException("Unable to use Instance with type variables!");
    }

    public void injectPluginFields(@NonNull JavaPlugin plugin) {
        if (!isInjectable(plugin.getClass())) {
            getLogger(plugin).warn("Plugin {} currently doesn't need to be @Injectable, but will be in the future.", plugin.getClass().getName());
        }
        instances.put(plugin.getClass(), plugin);
        injectFields(plugin, plugin, new LinkedHashSet<>());
    }

    private void injectFields(Object instance, JavaPlugin srcPlugin, SequencedSet<Class<?>> injectionCycle) {
        Arrays.stream(instance.getClass().getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(Inject.class))
            .forEach(f -> {
                if (Modifier.isFinal(f.getModifiers())) {
                    throw new InjectionException("@Inject field " + f.getDeclaringClass() + "#" + f.getName() + " cannot be final! " +
                        "If it is initialized in the constructor, it must not be annotated with @Inject!");
                }
                try {
                    f.setAccessible(true);
                    if (f.get(instance) != null) {
                        throw new InjectionException("@Inject field " + f.getDeclaringClass() + "#" + f.getName() + " must not be previously initialized!");
                    }
                    Object injectInstance;
                    if (f.getType() == Instance.class) {
                        Class<?> resolvedType;
                        try {
                            resolvedType = resolveInstanceType(f.getGenericType());
                        } catch (InjectionException e) {
                            throw new InjectionException(f.getDeclaringClass() + "#" + f.getName() + ": " + e.getMessage());
                        }
                        injectInstance = new DynamicInstance<>(this, resolvedType, srcPlugin);
                    } else {
                        Class<?> injectClass;
                        try {
                            injectClass = getInjectableCandidate(f.getType(), classCache);
                        } catch (InjectionException e) {
                            throw new InjectionException(f.getDeclaringClass() + "#" + f.getName() + ": " + e.getMessage());
                        }
                        debugLog(srcPlugin, "Injecting field {}#{} with type {}", f.getDeclaringClass(), f.getName(), injectClass);
                        injectInstance = obtainInstance(injectClass, srcPlugin, injectionCycle);
                    }
                    f.set(instance, injectInstance);
                } catch (InaccessibleObjectException | IllegalAccessException e) {
                    throw new InjectionException("Couldn't access @Inject field " + f.getDeclaringClass() + "#" + f.getName(), e);
                }
            });
    }

    private String formatConParams(Constructor<?> con, Bit isNested, Class<?>[] paramTypes, int paramIdx) {
        var format = new StringBuilder(con.getDeclaringClass().getName()).append("(\n");
        for (int i = isNested.asInt, last = paramTypes.length - 1; i < paramTypes.length; i++) {
            format.append("\t").append(paramTypes[i].getName());
            if (i != last) {
                format.append(",");
            }
            if (i == paramIdx) {
                format.append(" <-- HERE");
            }
            format.append("\n");
        }
        return format.append(")").toString();
    }

    private void debugLog(JavaPlugin plugin, String msg, Object... params) {
        LogUtil.debugInj(config, getLogger(plugin), msg, params);
    }

    private Logger getLogger(JavaPlugin plugin) {
        return loggers.computeIfAbsent(plugin, p -> LogUtil.getProcessLogger(plugin, getClass()));
    }

    private static Constructor<?> getInjectableConstructor(Class<?> clazz, Bit isNested) {
        final Constructor<?>[] cons = clazz.getDeclaredConstructors();
        if (cons.length == 1) {
            if (cons[0].getParameterCount() != isNested.asInt && !cons[0].isAnnotationPresent(Inject.class)) {
                throw new InjectionException("constructor in @Injectable class " + clazz.getName() + " with parameters must be annotated with @Inject");
            }
            return cons[0];
        }
        final List<Constructor<?>> injectableCons = Arrays.stream(cons)
            .filter(con -> con.isAnnotationPresent(Inject.class))
            .toList();
        if (injectableCons.size() == 1) {
            return injectableCons.getFirst();
        } else if (!injectableCons.isEmpty()) {
            throw new InjectionException("injectable class " + clazz.getName() + " must only define ONE constructor annotated with @Inject");
        }
        try {
            return clazz.getDeclaredConstructor(isNested.asBool ? new Class[]{clazz.getDeclaringClass()} : new Class[0]);
        } catch (NoSuchMethodException e) {
            throw new NewInstanceFailedException("@Injectable " + clazz + " must have an accessible no-arg constructor or constructor annotated with @Inject", null);
        }
    }

    <T> Stream<Class<? extends T>> getInjectableCandidates(Class<T> clazz) {
        if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
            if (isInjectable(clazz) || JavaPlugin.class.isAssignableFrom(clazz)) {
                return Stream.of(clazz);
            }
            return Stream.empty();
        }
        return classCache.getChildren(clazz, ClassCache.isNonAbstract(), Instantiator::isInjectable).stream();
    }

    static <T> Class<? extends T> getInjectableCandidate(Class<T> clazz, ClassCache classCache) {
        if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
            if (!isInjectable(clazz) && !JavaPlugin.class.isAssignableFrom(clazz)) {
                throw new InjectionException("Class " + clazz.getName() + " is not @Injectable!");
            }
            return clazz;
        }
        Set<Class<? extends T>> sub = classCache.getChildren(clazz, ClassCache.isNonAbstract());
        List<Class<? extends T>> injectable = sub.stream().filter(Instantiator::isInjectable).toList();
        if (injectable.isEmpty()) {
            List<Class<? extends T>> plugin = sub.stream().filter(JavaPlugin.class::isAssignableFrom).toList();
            if (plugin.size() == 1) {
                return plugin.getFirst();
            } else if (plugin.size() > 1) {
                throw new InjectionException("Found multiple plugins matching " + clazz + ": " + plugin);
            }
            String extra = sub.isEmpty() ? "." : (", but found " + sub.size() + " non-injectable, non-abstract subclasses: " + sub);
            throw new InjectionException("Found no @Injectable subclass for " + clazz + extra);
        } else if (injectable.size() > 1) {
            throw new InjectionException("Found multiple injectable candidates for " + clazz + ": " + injectable);
        }
        return injectable.getFirst();
    }

    static boolean isInjectable(Class<?> clazz) {
        return clazz.isAnnotationPresent(Injectable.class);
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
