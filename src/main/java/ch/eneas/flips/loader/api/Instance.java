package ch.eneas.flips.loader.api;

import java.util.stream.Stream;

public interface Instance<T> {
    /**
     * Gets the single valid instance of this type.
     *
     * @return singleton of given type
     * @throws InjectionException if none or multiple valid classes of this type were found
     */
    T get();

    /**
     * Stream all valid instances.
     *
     * @return a stream of all singletons of valid types.
     */
    Stream<T> stream();
}
