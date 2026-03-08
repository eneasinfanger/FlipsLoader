package ch.eneas.flips.loader.internal;

import lombok.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ClassCache {
    private final Set<Class<?>> classes;
    private final Map<Class<?>, Set<Class<?>>> directChildren = new HashMap<>();
    private final Map<Class<?>, List<Class<?>>> directInterfaces = new HashMap<>();
    private final Map<Class<? extends Annotation>, Set<Class<?>>> annotated = new HashMap<>();

    ClassCache(Set<Class<?>> classes) {
        this.classes = classes;
    }

    public int getClassCount(boolean onlyNonAbstract) {
        return !onlyNonAbstract
            ? classes.size()
            : (int) classes.stream().filter(isNonAbstract()).count();
    }

    @SafeVarargs
    public final <T> Set<Class<? extends T>> getChildren(@NonNull Class<T> superType, Predicate<Class<? extends T>>... filters) {
        var children = new HashSet<Class<? extends T>>();
        collectAllChildren(superType, children);
        return collect(children.stream(), filters);
    }

    private <T> void collectAllChildren(Class<? extends T> superType, Set<Class<? extends T>> children) {
        for (var child : getDirectChildren(superType)) {
            if (children.add(child)) {
                collectAllChildren(child, children);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> Set<Class<? extends T>> getDirectChildren(Class<T> superType) {
        return (Set) directChildren.computeIfAbsent(superType, s -> classes.stream()
            .filter(c -> c.getSuperclass() == s || getDirectInterfaces(c).contains(s))
            .collect(Collectors.toSet()));
    }

    private List<Class<?>> getDirectInterfaces(Class<?> cls) {
        return directInterfaces.computeIfAbsent(cls, c -> Arrays.asList(c.getInterfaces()));
    }

    @SafeVarargs
    public final Set<Class<?>> getAnnotatedClasses(@NonNull Class<? extends Annotation> annotation, Predicate<Class<?>>... filters) {
        return annotated.computeIfAbsent(annotation, a -> collect(classes.stream()
            .filter(c -> c.isAnnotationPresent(annotation)), filters));
    }

    @SafeVarargs
    public final Set<Class<?>> getClassesByName(Predicate<String> nameFilter, Predicate<Class<?>>... filters) {
        return collect(classes.parallelStream().filter(c -> nameFilter.test(c.getName())), filters);
    }

    @SafeVarargs
    private <C extends Class<?>> Set<C> collect(Stream<C> classes, Predicate<C>... filters) {
        if (filters != null) {
            for (var filter : filters) {
                classes = classes.filter(filter);
            }
        }
        return classes.collect(toSetView());
    }

    private static <T> Collector<T, ?, Set<T>> toSetView() {
        return Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet);
    }

    public static <C extends Class<?>> Predicate<C> isNonAbstract() {
        return cls -> !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers());
    }
}
