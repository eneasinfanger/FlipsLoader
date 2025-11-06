package ch.eneas.flips.loader.internal;

import java.util.*;
import java.util.function.BiPredicate;

class ClassComparer {

    static boolean structurallyEquals(Class<?> a, Class<?> b) {
        return structurallyEquals(a, b, new HashSet<>());
    }

    private static boolean structurallyEquals(Class<?> a, Class<?> b, Set<String> visited) {
        if (a == b) {
            return true;
        }
        if (!a.getName().equals(b.getName())) {
            return false;
        }
        if (a.getModifiers() != b.getModifiers()) {
            return false;
        }
        if (a.getClassLoader() == b.getClassLoader()) {
            return false;
        }

        String key = a.getName() + "|" + System.identityHashCode(a.getClassLoader()) +
            " vs " + b.getName() + "|" + System.identityHashCode(b.getClassLoader());
        if (!visited.add(key)) {
            return true;
        }

        if (!compareArray(a.getInterfaces(), b.getInterfaces(), (x, y) -> structurallyEquals(x, y, visited))) {
            return false;
        }
        if (!compareArray(a.getDeclaredConstructors(), b.getDeclaredConstructors(), (c1, c2) ->
            c1.getModifiers() == c2.getModifiers() &&
                areTypesEqual(c1.getParameterTypes(), c2.getParameterTypes(), visited) &&
                areTypesEqual(c1.getExceptionTypes(), c2.getExceptionTypes(), visited)
        )) {
            return false;
        }
        if (!compareArray(a.getDeclaredMethods(), b.getDeclaredMethods(), (m1, m2) ->
            m1.getName().equals(m2.getName()) &&
                m1.getModifiers() == m2.getModifiers() &&
                structurallyEquals(m1.getReturnType(), m2.getReturnType(), visited) &&
                areTypesEqual(m1.getParameterTypes(), m2.getParameterTypes(), visited) &&
                areTypesEqual(m1.getExceptionTypes(), m2.getExceptionTypes(), visited)
        )) {
            return false;
        }
        if (!compareArray(a.getDeclaredFields(), b.getDeclaredFields(), (f1, f2) ->
            f1.getName().equals(f2.getName()) &&
                f1.getModifiers() == f2.getModifiers() &&
                structurallyEquals(f1.getType(), f2.getType(), visited)
        )) {
            return false;
        }
        if (!compareArray(a.getDeclaredClasses(), b.getDeclaredClasses(), (x, y) -> structurallyEquals(x, y, visited))) {
            return false;
        }

        Class<?> superA = a.getSuperclass();
        Class<?> superB = b.getSuperclass();
        if (superA == null && superB == null) {
            return true;
        }
        if (superA == null || superB == null) {
            return false;
        }

        return structurallyEquals(superA, superB, visited);
    }

    private static boolean areTypesEqual(Class<?>[] a, Class<?>[] b, Set<String> visited) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (!structurallyEquals(a[i], b[i], visited)) return false;
        }
        return true;
    }

    private static <T> boolean compareArray(T[] a, T[] b, BiPredicate<T, T> comparator) {
        if (a.length != b.length) return false;
        List<T> unmatched = new ArrayList<>(List.of(b));
        outer:
        for (T itemA : a) {
            Iterator<T> iter = unmatched.iterator();
            while (iter.hasNext()) {
                T itemB = iter.next();
                if (comparator.test(itemA, itemB)) {
                    iter.remove();
                    continue outer;
                }
            }
            return false;
        }
        return unmatched.isEmpty();
    }
}
