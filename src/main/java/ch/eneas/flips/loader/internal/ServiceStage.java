package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.ClassProcessStage;
import ch.eneas.flips.loader.api.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class ServiceStage implements ClassProcessStage<Service> {
    private final Consumer<Service> services;

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
            services.accept(service);
        } catch (Exception startupException) {
            log.error("Failed to start service '{}' of plugin '{}'", service, plugin, startupException);
        }
    }
}
