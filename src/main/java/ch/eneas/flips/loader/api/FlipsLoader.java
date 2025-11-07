package ch.eneas.flips.loader.api;

import ch.eneas.flips.loader.internal.FlipsLoaderDelegate;
import org.bukkit.plugin.java.JavaPlugin;

public interface FlipsLoader {
    static void registerPlugin(JavaPlugin plugin, ClassProcessStage<?>... customStages) {
        FlipsLoaderDelegate.getInstance().registerPluginClasses(plugin, customStages);
    }

    static DelayedInit registerPluginDelayed(JavaPlugin plugin, ClassProcessStage<?>... customStages) {
        return FlipsLoaderDelegate.getInstance().registerPluginClassesDelayed(plugin, customStages);
    }

    /**
     * Returns an instance of the given class <b>if it has been registered previously</b>.
     *
     * @throws IllegalStateException if no registered instance of this class was found
     */
    static <T>T get(Class<T> clazz) {
        return FlipsLoaderDelegate.getInstance().getRegisteredInstance(clazz);
    }

    void registerPluginClasses(JavaPlugin plugin, ClassProcessStage<?>... customStages);

    DelayedInit registerPluginClassesDelayed(JavaPlugin plugin, ClassProcessStage<?>... customStages);

    interface DelayedInit {
        void init();
    }
}
