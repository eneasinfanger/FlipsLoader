package ch.eneas.flips.loader.internal;

import ch.eneas.flips.loader.api.FlipsLoader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ImplAccess {
    public static FlipsLoader getLoader() {
        return FlipsLoaderImpl.getInstance();
    }
}
