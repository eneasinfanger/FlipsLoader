package ch.eneas.flips.loader.api;

import ch.eneas.flips.loader.internal.FlipsLoaderDelegate;
import org.bukkit.plugin.java.JavaPlugin;

public interface FlipsLoader {
    static void registerPlugin(JavaPlugin plugin, ClassProcessStage<?>... customStages) {
        FlipsLoaderDelegate.getInstance().registerPluginClasses(plugin, customStages);
    }

    void registerPluginClasses(JavaPlugin plugin, ClassProcessStage<?>... customStages);
}
