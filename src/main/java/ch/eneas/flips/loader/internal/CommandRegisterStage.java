package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.CommandRegister;

import static ch.eneas.flips.loader.internal.FlipsLoaderDelegate.getLogger;

class CommandRegisterStage implements ClassProcessStage<CommandRegister> {
    @Override
    public String getStageDescriptor() {
        return "command register";
    }

    @Override
    public boolean matchesClass(Class<?> clazz) {
        return CommandRegister.class.isAssignableFrom(clazz);
    }

    @Override
    public void processClass(ClassProcessInfo<CommandRegister> info) {
        var commandsToRegister = info.getProcessInstance().registerAll();
        if (commandsToRegister == null) {
            throw new IllegalArgumentException("expected a non-null iterable from command register " + info.getProcessClass().getName() + "!");
        }
        if (commandsToRegister.isEmpty()) {
            throw new IllegalArgumentException("expected at least a command from command register " + info.getProcessClass().getName());
        }
        for (var command : commandsToRegister) {
            if (command == null) {
                throw new IllegalArgumentException("expected only non-null commands from command register " + info.getProcessClass().getName() + "!");
            }
            command.register(info.getNormalizedPluginName());
        }
    }
}
