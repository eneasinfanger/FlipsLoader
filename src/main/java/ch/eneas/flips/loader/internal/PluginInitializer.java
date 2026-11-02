package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.CommandRegister;
import ch.eneas.flips.loader.api.Service;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

import static ch.eneas.flips.loader.api.FlipsLoader.DelayedInit;
import static ch.eneas.flips.loader.internal.Instantiator.isInjectable;
import static ch.eneas.flips.loader.internal.LogUtil.debugInit;
import static ch.eneas.flips.loader.internal.RefUtil.unwrapCause;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

class PluginInitializer {
    private static final String COMMAND_CLASS_SUFFIX = "$Command";
    private final Logger log;
    private final InternalLoaderConfig config;
    @Getter(AccessLevel.PACKAGE)
    private final ClassCache classCache;
    @Getter(AccessLevel.PACKAGE)
    private final Instantiator instantiator;
    private final List<Service> services = new ArrayList<>();
    private final Set<AutoCloseable> closeables = new HashSet<>();

    PluginInitializer(InternalLoaderConfig config) {
        this.config = config;
        this.log = LogUtil.getProcessLogger(config.plugin(), getClass());
        this.classCache = new ClassCache(getClasses(config.plugin()));
        this.instantiator = new Instantiator(classCache, config);
    }

    private Set<Class<?>> getClasses(JavaPlugin plugin) {
        try {
            return ClassCollector.collectPluginClasses(plugin, config);
        } catch (IOException e) {
            throw new UncheckedIOException("IO-Operation failed while loading plugin '" + plugin + "' classes!", e);
        }
    }

    public DelayedInit initClasses() {
        initCommandAPI();
        instantiator.injectPluginFields(config.plugin());

        if (config.debugInitialization()) {
            debugInit(config, log, "Found {} classes (with {} being non-abstract) in plugin {}",
                classCache.getClassCount(false),
                classCache.getClassCount(true),
                config.plugin());
        }

        return new PluginInitProcessor();
    }

    private void initCommandAPI() {
        if (!config.initCommandAPI()) {
            return;
        }
        var cmdConfig = new CommandAPIPaperConfig(config.plugin())
            .silentLogs(true);
        config.commandAPICustomizer().accept(cmdConfig);

        debugInit(config, log, "Enabling CommandAPI...");
        CommandAPI.onLoad(cmdConfig);
        CommandAPI.onEnable();
    }

    public void deinitClasses() {
        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.error("Failed to close closeable '{}' from plugin '{}'", closeable, config.plugin(), e);
            }
        }
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

    private class PluginInitProcessor implements DelayedInit {
        @Override
        public void run() {
            initCommandRegisters();
            initAnnotatedCommands();
            initServices();
            initEventListeners();
        }

        private void initCommandRegisters() {
            String pluginNamespace = config.plugin().getName().strip().toLowerCase().replace(' ', '_');
            var commandRegisters = classCache.getChildren(CommandRegister.class, ClassCache.isNonAbstract(), this::includePackage);
            debugInit(config, log, "Found {} command registers: {}", commandRegisters.size(), commandRegisters);
            warnIfNotInjectable(commandRegisters, "Command register");
            commandRegisters.forEach(regCls -> {
                var reg = instantiator.getInstance(regCls, config.plugin());
                try {
                    debugInit(config, log, "Processing command register {}", regCls.getName());
                    registerCommands(reg, pluginNamespace);
                    registerCloseable(reg);
                } catch (Exception e) {
                    log.error("Error while processing command register {} of plugin {}", regCls, config.plugin(), e);
                }
            });
        }

        private void registerCommands(CommandRegister reg, String pluginNamespace) {
            final String className = reg.getClass().getName();
            var commandsToRegister = reg.registerAll();
            checkState(commandsToRegister != null, "expected a non-null iterable from command register %s!", className);
            checkArgument(!commandsToRegister.isEmpty(), "expected at least a command from command register %s", className);
            for (var command : commandsToRegister) {
                checkArgument(command != null, "expected only non-null commands from command register %s!", className);
                command.register(pluginNamespace);
            }
        }

        private void initAnnotatedCommands() {
            Class<?>[] generatedCommands = classCache.getClassesByName(n -> n.endsWith(COMMAND_CLASS_SUFFIX), ClassCache.isNonAbstract(), this::includePackage)
                .toArray(Class[]::new);
            Class<?>[] originalCommands = Arrays.stream(generatedCommands)
                .map(this::findAnnotatedClass)
                .toArray(Class[]::new);

            debugInit(config, log, "Found {} @Command annotated classes: {}", generatedCommands.length, generatedCommands);
            warnIfNotInjectable(Arrays.asList(originalCommands), "@Command");
            for (int i = 0; i < generatedCommands.length; i++) {
                if (originalCommands[i] != null) {
                    try {
                        debugInit(config, log, "Processing @Command {}", originalCommands[i].getName());
                        registerAnnotatedCommands(generatedCommands[i]);
                    } catch (Exception e) {
                        log.error("Error while processing @Command {}/{} of plugin {}", originalCommands[i].getName(), generatedCommands[i].getName(), config.plugin(), e);
                    }
                }
            }
        }

        private Class<?> findAnnotatedClass(Class<?> generatedClass) {
            try {
                return Class.forName(generatedClass.getName().substring(0, generatedClass.getName().length() - COMMAND_CLASS_SUFFIX.length()), true, generatedClass.getClassLoader());
            } catch (ClassNotFoundException e) {
                log.error("Original @Command annotated class for {} not found, if class is not generated: please do not use '{}' suffix for regular classes!", generatedClass, COMMAND_CLASS_SUFFIX, e);
                return null;
            }
        }

        /**
         * Implementation like {@link dev.jorel.commandapi.CommandAPI#registerCommand(Class)}
         */
        private void registerAnnotatedCommands(Class<?> cmdClass) {
            try {
                cmdClass.getDeclaredMethod("register").invoke(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to register supposedly @Command annotated class." +
                    "If this is not a generated class; please do not use the " + COMMAND_CLASS_SUFFIX + " for regular classes.",
                    unwrapCause(e));
            }
        }

        private void initServices() {
            var serviceClasses = classCache.getChildren(Service.class, ClassCache.isNonAbstract(), this::includePackage);
            debugInit(config, log, "Found {} services: {}", serviceClasses.size(), serviceClasses);
            warnIfNotInjectable(serviceClasses, "Service");
            serviceClasses.forEach(serviceCls -> {
                var service = instantiator.getInstance(serviceCls, config.plugin());
                try {
                    debugInit(config, log, "Starting service {}", serviceCls.getName());
                    service.startup(config.plugin());
                    services.add(service);
                    registerCloseable(service);
                } catch (Exception startupException) {
                    log.error("Failed to start service '{}' of plugin '{}'", service, config.plugin(), startupException);
                }
            });
        }

        private void initEventListeners() {
            var listeners = classCache.getChildren(Listener.class, ClassCache.isNonAbstract(), this::includePackage);
            debugInit(config, log, "Found {} event listeners: {}", listeners.size(), listeners);
            warnIfNotInjectable(listeners, "Event listener");
            listeners.forEach(listenerCls -> {
                var listener = instantiator.getInstance(listenerCls, config.plugin());
                try {
                    debugInit(config, log, "Registering event listener {}", listenerCls.getName());
                    Bukkit.getServer()
                        .getPluginManager()
                        .registerEvents(listener, config.plugin());
                    registerCloseable(listener);
                } catch (Exception e) {
                    log.error("Error while processing event listener {} of plugin {}", listenerCls, config.plugin(), e);
                }
            });
        }

        private void registerCloseable(Object bean) {
            if (bean instanceof AutoCloseable closeable) {
                closeables.add(closeable);
            }
        }

        private <C extends Class<?>> void warnIfNotInjectable(Iterable<C> classes, String identifier) {
            classes.forEach(c -> {
                if (c != null && !isInjectable(c)) {
                    log.warn("{} {} currently doesn't need to be @Injectable, but will be in the future.", identifier, c.getName());
                }
            });
        }

        private <T> boolean includePackage(Class<? extends T> c) {
            return config.packageFilter().test(c.getPackageName());
        }
    }
}
