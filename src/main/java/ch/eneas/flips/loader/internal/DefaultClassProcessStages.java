package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.*;
import ch.eneas.flips.loader.api.ClassProcessStage.ClassProcessInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

import static ch.eneas.flips.loader.internal.RefUtil.unwrapCause;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class DefaultClassProcessStages {
    private static final String COMMAND_CLASS_SUFFIX = "$Command";

    static ClassProcessStage<CommandRegister> commandRegisterStage() {
        return new SimpleClassProcessStage<>(
            "command register",
            CommandRegister.class::isAssignableFrom,
            DefaultClassProcessStages::registerCommands
        );
    }

    static ClassProcessStage<Object> annotatedCommandStage() {
        return new SimpleClassProcessStage<>(
            "@RegisterCommand",
            c -> c.getName().endsWith(COMMAND_CLASS_SUFFIX),
            DefaultClassProcessStages::registerAnnotatedCommands
        );
    }

    static ClassProcessStage<Service> serviceStage(Consumer<Service> adder) {
        return new SimpleClassProcessStage<>(
            "service",
            Service.class::isAssignableFrom,
            newRegisterServiceProcessor(adder)
        );
    }

    static ClassProcessStage<Listener> listenerStage() {
        return new SimpleClassProcessStage<>(
            "listener",
            Listener.class::isAssignableFrom,
            DefaultClassProcessStages::registerListener
        );
    }

    private static void registerCommands(ClassProcessInfo<CommandRegister> info) {
        final String className = info.getProcessClass().getName();
        var commandsToRegister = info.getProcessInstance().registerAll();
        checkState(commandsToRegister != null, "expected a non-null iterable from command register %s!", className);
        checkArgument(!commandsToRegister.isEmpty(), "expected at least a command from command register %s", className);
        for (var command : commandsToRegister) {
            checkArgument(command != null, "expected only non-null commands from command register %s!", className);
            command.register(info.getNormalizedPluginName());
        }
    }

    /**
     * Implementation like {@link dev.jorel.commandapi.CommandAPI#registerCommand(Class)}
     */
    private static void registerAnnotatedCommands(ClassProcessInfo<?> info) {
        try {
            info.getProcessClass()
                .getDeclaredMethod("register")
                .invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to register supposedly @Command annotated class." +
                "If this is not a generated class; please do not use the " + COMMAND_CLASS_SUFFIX + " for regular classes.",
                unwrapCause(e));
        }
    }

    private static Consumer<ClassProcessInfo<Service>> newRegisterServiceProcessor(Consumer<Service> adder) {
        return info -> {
            Service service = info.getProcessInstance();
            JavaPlugin plugin = info.getSourcePlugin();
            try {
                service.startup(plugin);
                adder.accept(service);
            } catch (Exception startupException) {
                log.error("Failed to start service '{}' of plugin '{}'", service, plugin, startupException);
            }
        };

    }

    private static void registerListener(ClassProcessInfo<Listener> info) {
        Bukkit.getServer()
            .getPluginManager()
            .registerEvents(info.getProcessInstance(), info.getSourcePlugin());
    }
}
