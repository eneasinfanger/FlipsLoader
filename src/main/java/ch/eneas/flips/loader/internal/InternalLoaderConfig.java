package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.ClassProcessStage;
import ch.eneas.flips.loader.api.FlipsLoaderConfig;
import com.google.common.collect.ImmutableList;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
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
    private List<String> searchPackages = List.of();
    private List<ClassProcessStage<?>> customProcessStages = List.of();

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
    public FlipsLoaderConfig searchPackages(@NonNull List<String> searchPackages) {
        checkEditable();
        this.searchPackages = List.copyOf(searchPackages);
        return this;
    }

    @Override
    public FlipsLoaderConfig customProcessStages(@NonNull List<ClassProcessStage<?>> customProcessStages) {
        checkEditable();
        this.customProcessStages = List.copyOf(customProcessStages);
        return this;
    }

    @Override
    public FlipsLoaderConfig customProcessStages(@NonNull ClassProcessStage<?> first, @NonNull ClassProcessStage<?>... more) {
        checkEditable();
        this.customProcessStages = ImmutableList.<ClassProcessStage<?>>builderWithExpectedSize(more.length + 1)
            .add(first).add(more)
            .build();
        return this;
    }

    synchronized void seal(@NonNull JavaPlugin plugin) {
        checkEditable();
        this.editable = false;
        this.plugin = plugin;
        this.searchPackages = Stream.concat(searchPackages.stream(), Stream.of(plugin.getClass().getPackageName()))
            .distinct()
            .toList();
    }

    private void checkEditable() {
        checkState(editable, "config is no longer editable! do not mutate config after FlipsLoader#init.. was called");
    }
}
