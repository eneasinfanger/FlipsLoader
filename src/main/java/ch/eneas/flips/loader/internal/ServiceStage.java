package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.Service;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

import static ch.eneas.flips.loader.internal.FlipsLoaderDelegate.getLogger;

@RequiredArgsConstructor
public class ServiceStage implements ClassProcessStage<Service> {
    private final Map<Service, JavaPlugin> services;

    @Override
    public String getStageDescriptor() {
        return "service";
    }

    @Override
    public boolean matchesClass(Class<?> clazz) {
        return Service.class.isAssignableFrom(clazz);
    }

    @Override
    public void processClass(ClassProcessInfo<Service> info) {
        Service service = info.getProcessInstance();
        JavaPlugin plugin = info.getSourcePlugin();
        try {
            service.startup(plugin);
            services.put(service, plugin);
        } catch (Exception startupException) {
            getLogger().error("Failed to start service '{}' of plugin '{}'", service, plugin, startupException);
        }
    }
}
