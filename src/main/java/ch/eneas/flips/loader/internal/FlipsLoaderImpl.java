package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.FlipsLoader;
import ch.eneas.flips.loader.api.FlipsLoaderConfig;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static ch.eneas.flips.loader.internal.Instantiator.getInjectableCandidate;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
class FlipsLoaderImpl implements FlipsLoader {
    @Getter
    private static final FlipsLoader instance = new FlipsLoaderImpl();

    private final Map<JavaPlugin, PluginInitializer> initializers = new HashMap<>();

    @Override
    public void initPluginClasses(JavaPlugin plugin, Consumer<FlipsLoaderConfig> configurator) {
        initPluginClassesDelayed(plugin, configurator).run();
    }

    @Override
    public DelayedInit initPluginClassesDelayed(@NonNull JavaPlugin plugin, Consumer<FlipsLoaderConfig> configurator) {
        if (initializers.containsKey(plugin)) {
            throw new IllegalStateException("plugin already initialized!");
        }

        var config = new InternalLoaderConfig();
        if (configurator != null) {
            configurator.accept(config);
        }
        config.seal(plugin);

        var initializer = new PluginInitializer(config);
        initializers.put(plugin, initializer);
        return initializer.initClasses();
    }

    @Override
    public void deinitPluginClasses(@NonNull JavaPlugin plugin) {
        var initializer = validateNNull(initializers.remove(plugin), "plugin not initialized / already deinitialized!");
        initializer.deinitClasses();
    }

    @Override
    public <P extends JavaPlugin> P getPlugin(@NonNull Class<P> clazz) {
        var plugin = initializers.keySet().stream()
            .filter(clazz::isInstance)
            .map(clazz::cast)
            .findAny()
            .orElse(null);
        return validateNNull(plugin, "no registered plugin for %s found", clazz);
    }

    @Override
    public <T> T getInstance(@NonNull JavaPlugin plugin, @NonNull Class<T> clazz) {
        PluginInitializer initializer = validateNNull(initializers.get(plugin), "plugin not initialized");
        if (clazz.isInstance(plugin)) {
            return clazz.cast(plugin);
        }
        Class<? extends T> injectable = getInjectableCandidate(clazz, initializer.getClassCache());
        return initializer.getInstantiator().getInstance(injectable, plugin);
    }

    @Override
    public <T> T getInstance(Class<? extends JavaPlugin> pluginClazz, Class<T> clazz) {
        return getInstance(getPlugin(pluginClazz), clazz);
    }

    @Override
    public <T> Stream<? extends T> getInstances(JavaPlugin plugin, Class<T> clazz) {
        checkArgument(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()), "Class must be an interface or abstract");
        var initializer = validateNNull(initializers.get(plugin), "plugin not initialized");
        Set<Class<? extends T>> children = initializer.getClassCache().getChildren(clazz, ClassCache.isNonAbstract(), Instantiator::isInjectable);
        return children.stream().map(c -> initializer.getInstantiator().getInstance(c, plugin));
    }

    @Override
    public <T> Stream<? extends T> getInstances(Class<? extends JavaPlugin> pluginClazz, Class<T> clazz) {
        return getInstances(getPlugin(pluginClazz), clazz);
    }

    private <T> T validateNNull(T obj, String msg, Object... args) {
        checkState(obj != null, msg, args);
        return obj;
    }
}
