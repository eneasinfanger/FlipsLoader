package ch.eneas.flips.loader.internal;

import ch.eneas.flips.libs.logging.LazyLogger;
import ch.eneas.flips.libs.logging.LogUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class LoaderLoggerFactory {
    private static final String FL = "FlipsLoader/";

    public static Logger getLoaderLogger(Class<?> you) {
        return new LazyLogger(() -> LoggerFactory.getLogger(FL + you.getSimpleName()));
    }

    public static Logger getProcessLogger(JavaPlugin processingPlugin, Class<?> clazz) {
        return getProcessLogger(processingPlugin, clazz.getSimpleName());
    }

    public static Logger getProcessLogger(JavaPlugin processingPlugin, String name) {
        return LogUtils.subLogger(processingPlugin, FL + name);
    }

    public static void debug(InternalLoaderConfig config, Logger logger, String message, Object... params) {
        if (config.debugLoader()) {
            // using INFO-Level and not DEBUG, because paper never shows DEBUG logs
            logger.info(message, params);
        }
    }
}
