package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.ClassProcessStage;
import ch.eneas.flips.loader.api.RegisterCommand;

import static ch.eneas.flips.loader.internal.RefUtil.unwrapCause;

public class CommandAnnotatedStage<T> implements ClassProcessStage<T> {
    @Override
    public String getStageDescriptor() {
        return "@RegisterCommand";
    }

    @Override
    public boolean matchesClass(Class<?> clazz) {
        // TODO: vielleicht mit "clazz.name.endsWith('$Command')" ersetzen? (und check auf register methode)
        return clazz.isAnnotationPresent(RegisterCommand.class);
    }

    @Override
    public void processClass(ClassProcessInfo<T> info) {
        // Implementation of dev.jorel.commandapi.CommandAPI#registerCommand(Class) with better error handling.
        try {
            Class.forName(info.getProcessClass().getName() + "$Command")
                .getDeclaredMethod("register")
                .invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to register supposedly @RegisterCommand annotated class. Is the @dev.jorel.commandapi.annotations.Command annotation present on this class? (Both annotations must be present!)", unwrapCause(e));
        }
    }
}
