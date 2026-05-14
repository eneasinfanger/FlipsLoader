package ch.eneas.flips.loader.internal;

import lombok.*;
import lombok.experimental.Delegate;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class LogUtil {
    private static final String FL = "FlipsLoader/";

    public static Logger getLoaderLogger(Class<?> you) {
        return new LazyLogger(() -> LoggerFactory.getLogger(FL + you.getSimpleName()));
    }

    public static Logger getProcessLogger(JavaPlugin processingPlugin, Class<?> clazz) {
        return getProcessLogger(processingPlugin, clazz.getSimpleName());
    }

    public static Logger getProcessLogger(JavaPlugin processingPlugin, String name) {
        return new LazyLogger(() -> LoggerFactory.getLogger(processingPlugin.getSLF4JLogger().getName() + "/" + name));
    }

    public static void debugInit(InternalLoaderConfig config, Logger logger, String message, Object... params) {
        if (config.debugInitialization()) {
            logger.info(message, params);
        }
    }

    public static void debugInj(InternalLoaderConfig config, Logger logger, String message, Object... params) {
        if (config.debugInjection()) {
            logger.info(message, params);
        }
    }

    public static void debugCL(InternalLoaderConfig config, Logger logger, String message, Object... params) {
        if (config.debugClassLoading()) {
            logger.info(message, params);
        }
    }

    public static Object lazyToString(Supplier<String> producer) {
        return new LazyToString(producer);
    }

    // ch.eneas.flips.libs.logging.LazyLogger
    @RequiredArgsConstructor
    private static class LazyLogger implements Logger {
        @NonNull
        private final Supplier<Logger> factory;
        @Delegate
        @Getter(lazy = true)
        private final Logger logger = factory.get();
    }

    // ch.eneas.flips.libs.logging.LogUtils.LazyToString
    @RequiredArgsConstructor
    private static class LazyToString {
        private final Supplier<String> toStringFunc;
        private String toStringCache;

        @Override
        public String toString() {
            return toStringCache != null ? toStringCache : (toStringCache = toStringFunc.get());
        }
    }
}
