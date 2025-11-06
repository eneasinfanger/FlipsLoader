package ch.eneas.flips.loader.api;

import org.bukkit.plugin.java.JavaPlugin;

public interface Service {
    void startup(JavaPlugin myPlugin);
    void shutdown(JavaPlugin myPlugin);
}
