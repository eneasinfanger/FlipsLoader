package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.FlipsLoaderConfig;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;

@Getter
@Accessors(fluent = true)
class InternalLoaderConfig implements FlipsLoaderConfig {
    private JavaPlugin plugin;
    private boolean editable = true;
    private boolean initCommandAPI = true;
    @NonNull
    private Consumer<CommandAPIPaperConfig> commandAPICustomizer = config -> {};
    private boolean debugLoader = false;
    private boolean debugInjection = false;
    private List<String> ignorePackages = List.of();

    @Override
    public FlipsLoaderConfig initCommandAPI(boolean init) {
        checkEditable();
        initCommandAPI = init;
        return this;
    }

    @Override
    public FlipsLoaderConfig commandAPICustomizer(@NonNull Consumer<CommandAPIPaperConfig> customizer) {
        checkEditable();
        commandAPICustomizer = customizer;
        return this;
    }

    @Override
    public FlipsLoaderConfig debugLoader(boolean debug) {
        checkEditable();
        debugLoader = debug;
        return this;
    }

    @Override
    public FlipsLoaderConfig debugInjection(boolean debug) {
        checkEditable();
        debugInjection = debug;
        return this;
    }

    @Override
    public FlipsLoaderConfig ignoredPackages(@NonNull List<String> ignorePackages) {
        checkEditable();
        this.ignorePackages = List.copyOf(ignorePackages);
        return this;
    }

    synchronized void seal(@NonNull JavaPlugin plugin) {
        checkEditable();
        this.editable = false;
        this.plugin = plugin;
        this.ignorePackages = Stream.concat(ignorePackages.stream(), Stream.of(CommandAPI.class.getPackageName()))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private void checkEditable() {
        checkState(editable, "config is no longer editable! do not mutate config after FlipsLoader#init.. was called");
    }
}
