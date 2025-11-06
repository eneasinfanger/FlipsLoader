package ch.eneas.flips.loader.internal;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class ListenerStage implements ClassProcessStage<Listener> {
    @Override
    public String getStageDescriptor() {
        return "listener";
    }

    @Override
    public boolean matchesClass(Class<?> clazz) {
        return Listener.class.isAssignableFrom(clazz);
    }

    @Override
    public void processClass(ClassProcessInfo<Listener> info) {
        Bukkit.getServer().getPluginManager().registerEvents(info.getProcessInstance(), info.getSourcePlugin());
    }
}
