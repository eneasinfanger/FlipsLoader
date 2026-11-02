package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.FlipsLoaderConfig;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
    private Set<DebugCategory> debugCategories = EnumSet.noneOf(DebugCategory.class);
    @Getter(AccessLevel.NONE)
    private List<String> ignorePackages = List.of();
    @Getter(AccessLevel.NONE)
    private List<String> searchPackages = List.of();
    private Predicate<String> packageFilter;

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
        Collections.addAll(debugCategories, categories);
        return this;
    }

    @Override
    public FlipsLoaderConfig ignoredPackages(@NonNull List<String> packages) {
        checkEditable();
        this.ignorePackages = List.copyOf(packages);
        return this;
    }

    @Override
    public FlipsLoaderConfig searchPackages(List<String> packages) {
        checkEditable();
        this.searchPackages = List.copyOf(packages);
        return this;
    }

    synchronized void seal(@NonNull JavaPlugin plugin) {
        checkEditable();
        this.plugin = plugin;
        this.ignorePackages = Stream.concat(ignorePackages.stream(), Stream.of(CommandAPI.class.getPackageName()))
            .distinct()
            .toList();
        Predicate<String> searchFilter = pkg -> searchPackages.stream().anyMatch(pkg::startsWith);
        Predicate<String> ignoreFilter = pkg -> ignorePackages.stream().noneMatch(pkg::startsWith);
        this.packageFilter = searchPackages.isEmpty() ? ignoreFilter : searchFilter.and(ignoreFilter);
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
