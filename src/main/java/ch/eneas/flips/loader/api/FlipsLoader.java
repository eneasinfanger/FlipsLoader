package ch.eneas.flips.loader.api;

import ch.eneas.flips.loader.internal.FlipsLoaderImpl;
import org.bukkit.plugin.java.JavaPlugin;

public interface FlipsLoader {
    /**
     * Shortcut for {@link #getLoader()}{@code .}{@link #initPluginClasses(JavaPlugin, ClassProcessStage[])}
     *
     * @see #initPluginClasses(JavaPlugin, ClassProcessStage[])
     */
    static void initPlugin(JavaPlugin plugin, ClassProcessStage<?>... customStages) {
        getLoader().initPluginClasses(plugin, customStages);
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
        return FlipsLoaderImpl.getInstance();
    }

    /**
     * Loads and initializes all classes implementing {@link CommandRegister}, {@link Service},
     * {@link org.bukkit.event.Listener Listener}, classes annotated with {@link dev.jorel.commandapi.annotations.Command @Command}
     * and any classes matched by custom process stages.
     *
     * @param plugin       Plugin instance whose classes shall be searched.
     * @param customStages Custom instances of {@link ClassProcessStage} for discovering other classes than described above
     *                     and evaluating custom initialization logic.
     * @throws RuntimeException if discovering or loading classes goes wrong or any otherwise critical error occurs.
     */
    void initPluginClasses(JavaPlugin plugin, ClassProcessStage<?>... customStages);

    /**
     * <p>
     * Everything described as in {@link #initPluginClasses(JavaPlugin, ClassProcessStage[])} but just loads the classes
     * and creates the instances, but waits with the initialization until {@link DelayedInit#run()} is called.
     * </p><p>
     * Even if the resulting trigger is discarded or the {@code init} method is never called (which will result
     * in commands and listeners not being registered and services not started), this plugin counts as initialized
     * and calling a second initialization on this plugin will throw an exception.
     * </p>
     *
     * @param plugin       Plugin instance whose classes shall be searched.
     * @param customStages Custom instances of {@link ClassProcessStage} for discovering other classes than described above
     *                     and evaluating custom initialization logic.
     * @return A "trigger" for actually initializing the found classes.
     * @throws RuntimeException if discovering or loading classes goes wrong or any otherwise critical error occurs.
     */
    DelayedInit initPluginClassesDelayed(JavaPlugin plugin, ClassProcessStage<?>... customStages);

    /**
     * Shuts down all services. (maybe does more stuff like unregister something in the future)
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
    <P extends JavaPlugin> P getPluginInstance(Class<P> clazz);

    /**
     * Gets the instance of an already initialized class (command register, service, etc.).
     *
     * @param plugin The plugin which this class belongs to.
     * @param clazz  The desired class
     * @param <T>    Type of the desired class
     * @return The instance
     * @throws IllegalStateException if this class wasn't previously initialized by this loader.
     * @see #getInitializedInstance(Class, Class)
     */
    <T> T getInitializedInstance(JavaPlugin plugin, Class<T> clazz);

    /**
     * Shortcut for {@link #getInitializedInstance(JavaPlugin, Class)} by also obtaining the plugin instance by class.
     *
     * @param pluginClazz The plugin class
     * @param clazz       The desired class
     * @param <T>         Type of the desired class
     * @return The instance
     * @throws IllegalStateException if this plugin wasn't registered using this loader or the class wasn't previously initialized by this loader.
     * @see #getInitializedInstance(JavaPlugin, Class)
     * @see #getPluginInstance(Class)
     */
    <T> T getInitializedInstance(Class<? extends JavaPlugin> pluginClazz, Class<T> clazz);

    /**
     * Trigger for delayed initialization of discovered classes.
     *
     * @see #initPluginClassesDelayed(JavaPlugin, ClassProcessStage[])
     */
    @FunctionalInterface
    interface DelayedInit {
        /**
         * Initialize the classes and trigger the startup / command registration / custom logic.
         *
         * @see #initPluginClasses(JavaPlugin, ClassProcessStage[])
         */
        void run();
    }
}
