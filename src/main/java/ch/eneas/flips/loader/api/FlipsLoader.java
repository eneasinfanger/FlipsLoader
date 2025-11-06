package ch.eneas.flips.loader.api;

import ch.eneas.flips.loader.internal.FlipsLoaderDelegate;
import org.bukkit.plugin.java.JavaPlugin;

public interface FlipsLoader {
    static void registerPlugin(JavaPlugin plugin) {
        FlipsLoaderDelegate.getInstance().registerPluginClasses(plugin);
    }

    void registerPluginClasses(JavaPlugin plugin);
}
