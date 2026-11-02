package ch.eneas.flips.loader.api;

import ch.eneas.flips.loader.internal.ImplAccess;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;
import java.util.stream.Stream;

public interface FlipsLoader {
    /**
     * Shortcut for {@link #getLoader()}{@code .}{@link #initPluginClasses(JavaPlugin, Consumer)}
     *
     * @see #initPluginClasses(JavaPlugin, Consumer)
     */
    static void initPlugin(JavaPlugin plugin) {
        getLoader().initPluginClasses(plugin, null);
    }

    /**
     * Shortcut for {@link #getLoader()}{@code .}{@link #deinitPluginClasses(JavaPlugin)}
     *
     * @see #deinitPluginClasses(JavaPlugin)
     */
    static void deinitPlugin(JavaPlugin plugin) {
        getLoader().deinitPluginClasses(plugin);
    }

    /**
     * Gets the universal {@code FlipsLoader} implementation.
     *
     * @return a {@code FlipsLoader} instance.
     */
    static FlipsLoader getLoader() {
        return ImplAccess.getLoader();
    }

    /**
     * Loads and initializes all classes implementing {@link CommandRegister}, {@link Service},
     * {@link Listener Listener}, classes annotated with {@link dev.jorel.commandapi.annotations.Command @Command}
     * and any classes matched by custom process stages.
     *
     * @param plugin       Plugin instance whose classes shall be searched.
     * @param configurator Custom config configurator for this plugin initialisation or {@code null} for the default config.
     * @throws RuntimeException if discovering or loading classes goes wrong or any otherwise critical error occurs.
     */
    void initPluginClasses(JavaPlugin plugin, Consumer<FlipsLoaderConfig> configurator);

    /**
     * <p>
     * Everything described as in {@link #initPluginClasses(JavaPlugin, Consumer)} but just loads the classes
     * and creates the instances, but waits with the initialization until {@link DelayedInit#run()} is called.
     * </p><p>
     * Even if the resulting trigger is discarded or the {@code init} method is never called (which will result
     * in commands and listeners not being registered and services not started), this plugin counts as initialized
     * and calling a second initialization on this plugin will throw an exception.
     * </p>
     *
     * @param plugin       Plugin instance whose classes shall be searched.
     * @param configurator Custom config configurator for this plugin initialisation or {@code null} for the default config.
     * @return A "trigger" for actually initializing the found classes.
     * @throws RuntimeException if discovering or loading classes goes wrong or any otherwise critical error occurs.
     */
    DelayedInit initPluginClassesDelayed(JavaPlugin plugin, Consumer<FlipsLoaderConfig> configurator);

    /**
     * Shuts down all services and calls {@link AutoCloseable#close() close()} on all {@link CommandRegister
     * CommandRegisters}, {@link Service Services} and {@link Listener Listeners} that implement
     * the {@link AutoCloseable} interface.
     *
     * @param plugin Plugins whose services should be shutdown.
     * @throws RuntimeException if the plugin was not initialized or already deinitialized.
     */
    void deinitPluginClasses(JavaPlugin plugin);

    /**
     * Gets the instance of a registered plugin class.
     *
     * @param clazz The plugin class
     * @param <P>   Type of the plugin
     * @return The previously registered instance
     * @throws IllegalStateException if this plugin wasn't registered using this FlipsLoader.
     */
    <P extends JavaPlugin> P getPlugin(Class<P> clazz);

    /**
     * Gets the instance of an any class that resides in the plugins class path. If the class is an interface
     * or abstract class, finds injectable subclasses of the class. The class must either be abstract or be
     * annotated with {@code @Injectable}.
     *
     * @param plugin The plugin which this class belongs to
     * @param clazz  The desired class
     * @param <T>    Type of the desired class
     * @return The instance
     * @throws InjectionException if the class is non-abstract and not injectable, or if no or multiple injectable
     *                            subclasses have been found.
     * @see #getInstance(Class, Class)
     * @see #getInstances(JavaPlugin, Class)
     */
    <T> T getInstance(JavaPlugin plugin, Class<T> clazz);

    /**
     * Shortcut for {@link #getInstance(JavaPlugin, Class)} by also obtaining the plugin instance by class.
     *
     * @param pluginClazz The plugin class
     * @param clazz       The desired class, must be an interface class or abstract
     * @param <T>         Type of the desired class
     * @return The instance
     * @throws InjectionException if the class is non-abstract and not injectable, or if no or multiple injectable
     *                            subclasses have been found.
     * @see #getInstance(JavaPlugin, Class)
     * @see #getPlugin(Class)
     * @see #getInstances(JavaPlugin, Class)
     */
    <T> T getInstance(Class<? extends JavaPlugin> pluginClazz, Class<T> clazz);

    /**
     * Gets all {@code @Injectable} instances of children of the interface / abstract class that reside in the plugins class path.
     *
     * @param plugin The plugin which this class belongs to
     * @param clazz  The super class
     * @param <T>    Type of the super class
     * @return Stream of children
     */
    <T> Stream<? extends T> getInstances(JavaPlugin plugin, Class<T> clazz);

    /**
     * Shortcut for {@link #getInstances(JavaPlugin, Class)} by also obtaining the plugin instance by class.
     *
     * @param pluginClazz The plugin class
     * @param clazz       The super class
     * @param <T>         Type of the super class
     * @return Stream of children
     */
    <T> Stream<? extends T> getInstances(Class<? extends JavaPlugin> pluginClazz, Class<T> clazz);

    /**
     * Trigger for delayed initialization of discovered classes.
     *
     * @see #initPluginClassesDelayed(JavaPlugin, Consumer)
     */
    @FunctionalInterface
    interface DelayedInit {
        /**
         * Initialize the classes and trigger the startup / command registration / custom logic.
         *
         * @see #initPluginClasses(JavaPlugin, Consumer)
         */
        void run();
    }
}
