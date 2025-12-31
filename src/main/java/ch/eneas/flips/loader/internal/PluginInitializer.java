package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.ClassProcessStage;
import ch.eneas.flips.loader.api.Service;
import com.google.common.collect.ImmutableList;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.eneas.flips.loader.api.ClassProcessStage.ClassProcessInfo;
import static ch.eneas.flips.loader.api.FlipsLoader.DelayedInit;
import static ch.eneas.flips.loader.internal.DefaultClassProcessStages.*;
import static ch.eneas.flips.loader.internal.LoaderLoggerFactory.debug;

class PluginInitializer {
    private final Logger log;
    private final InternalLoaderConfig config;
    private final Instantiator instantiator = new Instantiator();
    private final List<Service> services = new ArrayList<>();

    PluginInitializer(InternalLoaderConfig config) {
        this.config = config;
        this.log = LoaderLoggerFactory.getProcessLogger(config.plugin(), getClass());
    }

    public DelayedInit initClasses() {
        initCommandAPI();

        Set<Class<?>> classes = getClasses(config.plugin())
            .stream()
            .filter(cls -> !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers()))
            .collect(Collectors.toSet());
        debug(config, log, "Found {} (non-abstract) classes in plugin {}", classes.size(), config.plugin());

        final List<ClassProcessStage<?>> processStages = arrangeProcessStages();
        debug(config, log, "Using stages: {}", processStages);

        Map<ClassProcessStage<?>, List<Class<?>>> matchingClasses = processStages.parallelStream()
            .collect(Collectors.toMap(stage -> stage, stage -> new ArrayList<>()));

        classes.parallelStream().forEach(clazz -> {
            if (config.searchPackages().stream().noneMatch(pkg -> clazz.getPackageName().startsWith(pkg))) {
                return;
            }
            processStages.forEach(stage -> {
                if (stage.matchesClass(clazz)) {
                    debug(config, log, "Found {} matching {}.", clazz.getName(), stage.getStageDescriptor());
                    matchingClasses.get(stage).add(clazz);
                }
            });
        });

        instantiator.addInstance(config.plugin());

        String normalizedPluginName = config.plugin().getName().strip().toLowerCase().replace(' ', '_');
        var processInfos = matchingClasses.values().parallelStream()
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toMap(clazz -> clazz, clazz -> createClassProcessInfo(clazz, normalizedPluginName)));

        return () -> processStages.forEach(stage -> matchingClasses.get(stage).forEach(clazz -> {
            try {
                debug(config, log, "Processing {} {}", stage.getStageDescriptor(), clazz.getName());
                //noinspection unchecked,rawtypes
                stage.processClass((ClassProcessInfo) processInfos.get(clazz));
            } catch (Exception e) {
                log.error("Error in {} stage while processing {} of plugin {}", stage.getStageDescriptor(), clazz, config.plugin(), e);
            }
        }));
    }

    private void initCommandAPI() {
        if (!config.initCommandAPI()) {
            return;
        }
        var cmdConfig = new CommandAPIPaperConfig(config.plugin())
            .silentLogs(true);
        config.commandAPICustomizer().accept(cmdConfig);

        CommandAPI.onLoad(cmdConfig);
        CommandAPI.onEnable();
    }

    private List<ClassProcessStage<?>> arrangeProcessStages() {
        return ImmutableList.<ClassProcessStage<?>>builder()
            .add(commandRegisterStage())
            .add(annotatedCommandStage())
            .add(serviceStage(services::add))
            .add(listenerStage())
            .addAll(config.customProcessStages())
            .build();
    }

    private static Set<Class<?>> getClasses(JavaPlugin plugin) {
        try {
            return ClassCollector.collectPluginClasses(plugin);
        } catch (IOException e) {
            throw new UncheckedIOException("IO-Operation failed while loading plugin '" + plugin + "' classes!", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load class file of plugin '" + plugin + "'!", e);
        }
    }

    public void deinitClasses() {
        for (Service service : services) {
            try {
                service.shutdown(config.plugin());
            } catch (Exception e) {
                log.error("Failed to shutdown service '{}' from plugin '{}'", service, config.plugin(), e);
            }
        }
        if (config.initCommandAPI()) {
            CommandAPI.onDisable();
        }
    }

    private <T> ClassProcessStage.ClassProcessInfo<T> createClassProcessInfo(Class<T> clazz, String normalizedPluginName) {
        return new ClassProcessStage.ClassProcessInfo<>() {
            final T instance = instantiator.getInstance(clazz, config.plugin());

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
                return config.plugin();
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
