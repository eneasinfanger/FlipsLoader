package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.FlipsLoader;
import ch.eneas.flips.loader.api.FlipsLoaderConfig;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FlipsLoaderImpl implements FlipsLoader {
    @Getter
    private static final FlipsLoader instance = new FlipsLoaderImpl();

    private final Map<JavaPlugin, PluginInitializer> initializers = new HashMap<>();

    @Override
    public void initPluginClasses(JavaPlugin plugin, Consumer<FlipsLoaderConfig> configurator) {
        initPluginClassesDelayed(plugin, configurator).run();
    }

    @Override
    public DelayedInit initPluginClassesDelayed(JavaPlugin plugin, Consumer<FlipsLoaderConfig> configurator) {
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
    public void deinitPluginClasses(JavaPlugin plugin) {
        var initializer = validate(initializers.remove(plugin), "plugin not initialized / already deinitialized!");
        initializer.deinitClasses();
    }

    @Override
    public <P extends JavaPlugin> P getPluginInstance(Class<P> clazz) {
        var plugin = initializers.keySet().stream()
            .filter(clazz::isInstance)
            .map(clazz::cast)
            .findAny()
            .orElse(null);
        return validate(plugin, "no registered plugin for %s found", clazz);
    }

    @Override
    public <T> T getInitializedInstance(JavaPlugin plugin, Class<T> clazz) {
        var initializer = validate(initializers.get(plugin), "plugin not initialized");
        return validate(initializer.getInstance(clazz), "no registered instance for %s found", clazz);
    }

    @Override
    public <T> T getInitializedInstance(Class<? extends JavaPlugin> pluginClazz, Class<T> clazz) {
        return getInitializedInstance(getPluginInstance(pluginClazz), clazz);
    }

    private <T> T validate(T obj, String msg, Object... args) {
        Preconditions.checkState(obj != null, msg, args);
        return obj;
    }
}
