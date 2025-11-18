package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.ClassProcessStage;
import ch.eneas.flips.loader.api.FlipsLoader;
import com.google.common.base.Preconditions;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FlipsLoaderImpl implements FlipsLoader {
    @Getter
    private static final FlipsLoader instance = new FlipsLoaderImpl();

    private final Map<JavaPlugin, PluginInitializer> initializers = new HashMap<>();

    @Override
    public void initPluginClasses(JavaPlugin plugin, ClassProcessStage<?>... customStages) {
        initPluginClassesDelayed(plugin, customStages).run();
    }

    @Override
    public DelayedInit initPluginClassesDelayed(JavaPlugin plugin, ClassProcessStage<?>... customStages) {
        if (initializers.containsKey(plugin)) {
            throw new IllegalStateException("plugin already initialized!");
        }

        // TODO only do if enabled in config
        CommandAPI.onLoad(new CommandAPIBukkitConfig(plugin)
            .usePluginNamespace()
            .silentLogs(true));
        CommandAPI.onEnable();

        var initializer = new PluginInitializer(plugin, List.of(customStages));
        initializers.put(plugin, initializer);
        return initializer.initClasses();
    }

    @Override
    public void deinitPluginClasses(JavaPlugin plugin) {
        var initializer = validate(initializers.remove(plugin), "plugin not initialized / already deinitialized!");
        initializer.deinitClasses();

        // TODO only do if enabled in config
        CommandAPI.onDisable();
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
