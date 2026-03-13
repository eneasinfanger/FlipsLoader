package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.Instance;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

@RequiredArgsConstructor
class DynamicInstance<T> implements Instance<T> {
    private final Instantiator instantiator;
    private final Class<T> clazz;
    private final JavaPlugin srcPlugin;

    @Override
    public T get() {
        return instantiator.getInstance(clazz, srcPlugin);
    }

    @Override
    public Stream<T> stream() {
        return instantiator.getInjectableCandidates(clazz)
            .map(c -> instantiator.getInstance(c, srcPlugin));
    }
}
