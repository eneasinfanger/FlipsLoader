package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.FlipsLoaderConfig;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;

@Getter(AccessLevel.PACKAGE)
@Accessors(fluent = true)
class InternalLoaderConfig implements FlipsLoaderConfig {
    private JavaPlugin plugin;
    private boolean initCommandAPI = true;
    @NonNull
    private Consumer<CommandAPIPaperConfig> commandAPICustomizer = config -> {};
    @Getter(AccessLevel.NONE)
    private Set<DebugCategory> debugCategories = Set.of();
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
    public FlipsLoaderConfig debugLoader(DebugCategory[] categories) {
        checkEditable();
        debugCategories = EnumSet.copyOf(Arrays.asList(categories));
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
        this.plugin = plugin;
        this.ignorePackages = Stream.concat(ignorePackages.stream(), Stream.of(CommandAPI.class.getPackageName()))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    boolean debugInitialization() {
        return debugCategories.contains(DebugCategory.INITIALIZATION);
    }

    boolean debugInjection() {
        return debugCategories.contains(DebugCategory.INJECTION);
    }

    boolean debugClassLoading() {
        return debugCategories.contains(DebugCategory.CLASS_LOADING);
    }

    private void checkEditable() {
        checkState(plugin == null, "Do not mutate config after FlipsLoader#init.. was called");
    }
}
