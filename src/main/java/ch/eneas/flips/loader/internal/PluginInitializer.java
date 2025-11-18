package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.ClassProcessStage;
import ch.eneas.flips.loader.api.Service;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.eneas.flips.loader.api.FlipsLoader.DelayedInit;

@Slf4j
class PluginInitializer {
    private final JavaPlugin plugin;
    private final List<ClassProcessStage<?>> processStages = new ArrayList<>();
    private final Instantiator instantiator = new Instantiator();
    private final List<Service> services = new ArrayList<>();

    PluginInitializer(JavaPlugin plugin, List<ClassProcessStage<?>> customStages) {
        this.plugin = plugin;
        processStages.add(new CommandRegisterStage());
        processStages.add(new CommandAnnotatedStage<>());
        processStages.add(new ServiceStage(services::add));
        processStages.add(new ListenerStage());
        processStages.addAll(customStages);
    }

    public DelayedInit initClasses() {
        Set<Class<?>> classes = getClasses(plugin)
            .stream()
            .filter(cls -> !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers()))
            .collect(Collectors.toSet());

        Map<ClassProcessStage<?>, List<Class<?>>> matchingClasses = processStages.parallelStream()
            .collect(Collectors.toMap(stage -> stage, stage -> new ArrayList<>()));

        classes.parallelStream().forEach(clazz -> {
            if (!clazz.getPackageName().startsWith("ch.eneas")) { // TODO replace with actual config
                return;
            }
            processStages.forEach(stage -> {
                if (stage.matchesClass(clazz)) {
                    matchingClasses.get(stage).add(clazz);
                }
            });
        });

        instantiator.addInstance(plugin);

        String normalizedPluginName = plugin.getName().strip().toLowerCase().replace(' ', '_');
        var processInfos = matchingClasses.values().parallelStream()
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toMap(clazz -> clazz, clazz -> createClassProcessInfo(plugin, clazz, normalizedPluginName)));

        return () -> processStages.forEach(stage -> matchingClasses.get(stage).forEach(clazz -> {
            try {
                //noinspection unchecked,rawtypes
                stage.processClass((ClassProcessStage.ClassProcessInfo) processInfos.get(clazz));
            } catch (Exception e) {
                log.error("Error in {} stage while processing {} of plugin {}", stage.getStageDescriptor(), clazz, plugin, e);
            }
        }));
    }

    private static Set<Class<?>> getClasses(JavaPlugin plugin) {
        try {
            return ClassCollector.collectPluginClasses(plugin);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to load plugin classes!", e);
        }
    }

    public void deinitClasses() {
        for (Service service : services) {
            try {
                service.shutdown(plugin);
            } catch (Exception e) {
                log.error("Failed to shutdown service '{}' from plugin '{}'", service, plugin, e);
            }
        }
    }

    private <T> ClassProcessStage.ClassProcessInfo<T> createClassProcessInfo(JavaPlugin plugin, Class<T> clazz, String normalizedPluginName) {
        return new ClassProcessStage.ClassProcessInfo<>() {
            final T instance = instantiator.getInstance(clazz, plugin);

            @Override
            public T getProcessInstance() {
                return instance;
            }

            @Override
            public Class<T> getProcessClass() {
                return clazz;
            }

            @Override
            public JavaPlugin getSourcePlugin() {
                return plugin;
            }

            @Override
            public String getNormalizedPluginName() {
                return normalizedPluginName;
            }
        };
    }

    public <T> T getInstance(Class<T> clazz) {
        return instantiator.getCachedInstance(clazz);
    }
}
