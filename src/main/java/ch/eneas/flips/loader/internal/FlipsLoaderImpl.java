package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.FlipsLoader;
import ch.eneas.flips.loader.api.Service;
import ch.eneas.flips.loader.internal.ClassProcessStage.ClassProcessInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@ApiStatus.Internal
public final class FlipsLoaderImpl extends JavaPlugin implements FlipsLoader {
    private final Map<Service, JavaPlugin> services = new HashMap<>();
    private final Logger logger = getSLF4JLogger();
    private final List<ClassProcessStage<?>> stages = Arrays.asList(
        new CommandRegisterStage(),
        new CommandAnnotatedStage<>(),
        new ServiceStage(services),
        new ListenerStage()
    );

    @Override
    public void registerPluginClasses(JavaPlugin plugin) {
        Set<Class<?>> classes = getClasses(plugin)
            .stream()
            .filter(cls -> !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers()))
            .collect(Collectors.toSet());

        Map<ClassProcessStage<?>, List<Class<?>>> matchingClasses = stages.parallelStream()
            .collect(Collectors.toMap(stage -> stage, stage -> new ArrayList<>()));

        classes.parallelStream().forEach(clazz -> stages.forEach(stage -> {
            if (stage.matchesClass(clazz)) {
                matchingClasses.get(stage).add(clazz);
            }
        }));

        String normalizedPluginName = plugin.getName().strip().toLowerCase().replace(' ', '_');
        var processInfos = matchingClasses.values().parallelStream()
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toMap(clazz -> clazz, clazz -> createClassProcessInfo(plugin, clazz, normalizedPluginName)));

        stages.forEach(stage -> matchingClasses.get(stage).forEach(clazz -> {
            try {
                //noinspection unchecked,rawtypes
                stage.processClass((ClassProcessInfo) processInfos.get(clazz));
            } catch (Exception e) {
                getSLF4JLogger().error("Error in {} stage while processing {} of plugin {}", stage.getStageDescriptor(), clazz, plugin, e);
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

    private static <T> ClassProcessInfo<T> createClassProcessInfo(JavaPlugin plugin, Class<T> clazz, String normalizedPluginName) {
        return new ClassProcessInfo<>() {
            final T instance = Instantiator.getInstance(clazz, plugin);

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

    @Override
    public void onDisable() {
        for (Map.Entry<Service, JavaPlugin> entry : services.entrySet()) {
            Service service = entry.getKey();
            JavaPlugin plugin = entry.getValue();
            try {
                service.shutdown(plugin);
            } catch (Exception e) {
                logger.error("Failed to shutdown service '{}' from plugin '{}'", service, plugin, e);
            }
        }
    }
}
