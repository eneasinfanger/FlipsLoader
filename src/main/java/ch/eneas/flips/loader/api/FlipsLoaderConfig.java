package ch.eneas.flips.loader.api;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIConfig;
import dev.jorel.commandapi.CommandAPIPaperConfig;

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
    FlipsLoaderConfig commandAPICustomizer(Consumer<CommandAPIPaperConfig> customizer);

    /**
     * If enabled then the loader logs some values about which classes are discovered and processed.
     *
     * @return this config
     * @apiNote Defaults to {@code false}.
     */
    FlipsLoaderConfig debugLoader(boolean debug);

    /**
     * If enabled then the loader logs additional values about which classes are created, cached, etc. for injection.
     *
     * @return this config
     * @apiNote Defaults to {@code false}.
     */
    FlipsLoaderConfig debugInjection(boolean debug);

    /**
     * Which packages should not be used for discovering classes.
     * This exists since a lot of plugins use shading and without limiting discovery to certain packages it can cause performance issues or runtime errors.
     * <p>
     * Package names should be dot-separated and all packages starting with an item of this list are considered.
     * </p>
     * <p>
     * For example if {@code ch.eneas} is provided, then packages like {@code ch.eneas.flips} and {@code ch.eneas.hello.world} <b>will</b> be ignored.
     * </p>
     *
     * @return this config
     * @apiNote the {@code dev.jorel.commandapi} package is excluded by default.
     */
    FlipsLoaderConfig ignoredPackages(List<String> ignorePackages);
}
