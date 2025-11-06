package ch.eneas.flips.loader.api;

import dev.jorel.commandapi.ExecutableCommand;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public interface CommandRegister {
    /**
     * Creates and returns the command which should be registered under this plugin.
     * <dl style="color:orange">
     *     <dt><strong>Warning:</strong></dt>
     *     <dd>Only {@link #registerAll()} is called, so if both methods are overridden, this command is not going to get registered.</dd>
     * </dl>
     * @return A {@link dev.jorel.commandapi.CommandTree} or {@link dev.jorel.commandapi.CommandAPICommand} to register. Is also allowed to return {@code null} if {@linkplain #registerAll()} returns something. (One of the methods just has to return something.)
     * @apiNote Example:
     * <pre>
     * {@code
     * import dev.jorel.commandapi.CommandAPICommand;
     * import dev.jorel.commandapi.arguments.*;
     * import org.bukkit.entity.Player;
     *
     * @Override
     * public CommandAPICommand register() {
     *     return new CommandAPICommand("mycommand")
     *     .withArguments(new StringArgument("name"))
     *     .withArguments(new IntegerArgument("amount"))
     *     .withOptionalArguments(new PlayerArgument("player"))
     *     .withOptionalArguments(new PlayerArgument("target"))
     *     .withOptionalArguments(new GreedyStringArgument("message"))
     *     .executesPlayer((player, args) -> {
     *         String name = (String) args.get(0);
     *         int amount = (int) args.get("amount");
     *         Player p = (Player) args.getOrDefault("player", player);
     *         Player target = (Player) args.getOrDefault("target", () -> player);
     *         String message = (String) args.getOptional("message").orElse("Hello!");
     *
     *         // ...
     *     });
     * }
     * }
     * </pre>
     * @implNote The default implementation returns {@code null}.
     */
    default ExecutableCommand<?, CommandSender> register() {
        throw new IllegalStateException("this register() method or the registerAll() method MUST be implemented");
    }

    /**
     * Creates and returns an iterable of commands which should be registered under this plugin.
     * @return An iterable (Any subclass of {@linkplain java.util.Collection} or something else with an iterator) of commands to register.
     * @implNote <p style="color:orange">The default method the result of {@link #register()} in a collection.</p>
     */
    default Collection<? extends ExecutableCommand<?, CommandSender>> registerAll() {
        return Collections.singleton(register());
    }
}
