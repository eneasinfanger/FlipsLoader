package ch.eneas.flips.loader.api;

import org.bukkit.plugin.java.JavaPlugin;

public interface ClassProcessStage<T> {
    String getStageDescriptor();

    boolean matchesClass(Class<?> clazz);

    void processClass(ClassProcessInfo<T> info);

    interface ClassProcessInfo<T> {
        T getProcessInstance();

        Class<T> getProcessClass();

        JavaPlugin getSourcePlugin();

        String getNormalizedPluginName();
    }
}
