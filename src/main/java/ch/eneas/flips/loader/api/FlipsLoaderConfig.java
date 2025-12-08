package ch.eneas.flips.loader.api;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPIConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.Consumer;

public interface FlipsLoaderConfig {
    /**
     * Call {@link CommandAPI#onLoad(CommandAPIConfig)} / {@link CommandAPI#onEnable()} / {@link CommandAPI#onDisable()} when initializing a plugin.
     *
     * @param init if the CommandAPI should be initialized.
     * @return this config
     * @apiNote Defaults to {@code true} since the FlipsLoader is usually used together with the CommandAPI and that can save manually calling the init method.
     * You should disable this if you are not shading the CommandAPI.
     */
    FlipsLoaderConfig initCommandAPI(boolean init);

    /**
     * Extra configuration for the {@link CommandAPIConfig}.
     * Only needed if {@link #initCommandAPI(boolean)}  is enabled.
     *
     * @param customizer a customizer for the CommandAPI configuration that is passed when initialising it.
     * @return this config
     * @apiNote Default FlipsLoader already sets the config's plugin to the initialising plugin, sets the namespace to use the plugins name and enables silent logs.
     */
    FlipsLoaderConfig commandAPICustomizer(Consumer<CommandAPIBukkitConfig> customizer);

    /**
     * If enabled then the loader logs some values about which classes are discovered and processed.
     *
     * @return this config
     * @apiNote Defaults to {@code false}.
     */
    FlipsLoaderConfig debugLoader(boolean debug);

    /**
     * Which packages should be used for discovering classes.
     * This exists since a lot of plugins use shading and without limiting discovery to certain packages it can cause performance issues or runtime errors.
     * <p>
     * Package names should be dot-separated and all packages starting with an item of this list are considered.
     * </p>
     * <p>
     * For example if {@code ch.eneas} is provided, then packages like {@code ch.eneas.flips} and {@code ch.eneas.hello.world} <b>will</b> be searched.
     * </p>
     *
     * @return this config
     * @apiNote the package of the initialised plugin is automatically added to the packages in this list
     */
    FlipsLoaderConfig searchPackages(List<String> searchPackages);

    /**
     * Custom instances of {@link ClassProcessStage} for discovering other classes than described in {@link FlipsLoader#initPluginClasses(JavaPlugin, Consumer)}
     * and evaluating custom initialization logic.
     *
     * @return this config
     */
    FlipsLoaderConfig customProcessStages(List<ClassProcessStage<?>> customProcessStages);

    /**
     * Custom instances of {@link ClassProcessStage} for discovering other classes than described in {@link FlipsLoader#initPluginClasses(JavaPlugin, Consumer)}
     * and evaluating custom initialization logic.
     *
     * @return this config
     */
    // use single argument before varargs to avoid identical signatures with internal config getter which has the same name
    FlipsLoaderConfig customProcessStages(ClassProcessStage<?> customStage, ClassProcessStage<?>... customStages);
}
